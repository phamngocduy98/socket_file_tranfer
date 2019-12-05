import com.sun.media.sound.UlawCodec;

import java.io.*;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class Multi {
    private static PiecePool piecePool;
    private static AtomicInteger currentPieceNum = new AtomicInteger(-1);

    public static void main(String[] args) throws FileNotFoundException {
        Scanner scanner = new Scanner(System.in);
        SockServer sockServer = null;
        piecePool = new PiecePool();

//            System.out.print("Enter server ip: ");
//        String ip = "192.168.98.2"; //scanner.nextLine();
        String ip = "127.0.0.1";
        try {
            sockServer = new SockServer(ip, 1259);
            System.out.println("[SERVER] Listening " + sockServer.getServerIP() + ":" + sockServer.getServerPort());
        } catch (UnknownHostException e) {
            System.out.println("[ERROR] Server bind error, check server ip");
        } catch (IOException e) {
            System.out.println("[ERROR] Server bind error, check server ip and port!");
            return;
        }
        new Thread(new SockServerTask.AcceptClientThread(sockServer) {
            @Override
            public void newClientThread(SockClient sockClient) {
                Thread clientThread = new ClientThreadMulti(sockServer, sockClient, piecePool);
                clientThread.start();
            }
        }).start();
        while (true) {
            String command = scanner.nextLine();
            if (command.contains("ls")) {
                Utils.selfLs();
            } else if (command.contains("post")) {
                try {
                    String fileName = Utils.getDataFromCommand(Utils.Actions.POST, command);
                    File file = new File(Utils.getFolderPath() + fileName);
                    if (!file.exists()) {
                        System.out.println("[BROADCAST (Sv-Cl) ERROR] File not exist to broadcast!");
                        continue;
                    }
                    SockServerTask.broastcastFile(sockServer, fileName);
                } catch (InterruptedException e) {
                    System.out.println("[BROADCAST (Sv-Cl) ERROR] File broadcast Interrupted!");
                    continue;
                } catch (IOException e) {
                    System.out.println("[BROADCAST (Sv-Cl) ERROR] File broadcast IOException!");
                    continue;
                }
            } else if (command.contains("broadcast")) {
                try {
                    currentPieceNum.set(-1);
                    String fileName = Utils.getDataFromCommand(Utils.Actions.BROADCAST, command);
                    File file = new File(Utils.getFolderPath() + fileName);
                    if (!file.exists()) {
                        System.out.println("[BROADCAST P2P ERROR] File not exist to broadcast!");
                        continue;
                    }
                    piecePool.setFileName(file.getName(), file.length());
                    SockServerTaskMulti.broastcastFile(sockServer, file);
                    Utils.setStartTime();
                } catch (IOException e) {
                    System.out.println("[BROADCAST P2P ERROR] File broadcast IO Error!");
                    continue;
                }
            } else {
                System.out.println("Invalid command!");
            }
        }
    }

    public static class ClientThreadMulti extends Main.ClientThread {
        PiecePool piecePool;

        public ClientThreadMulti(SockServer sockServer, SockClient sockClient, PiecePool piecePool) {
            super(sockServer, sockClient);
            this.piecePool = piecePool;
        }

        @Override
        public void run() {
            String receivedMessage = "";
            do {
                try {
                    receivedMessage = sockClient.readString();
//                    System.out.println("[COMMAND " + sockClient.getClientAddress() + "] " + receivedMessage);
                    if (receivedMessage.contains("list")) {
                        SockClientTask.sendLs(sockClient, Utils.getFolderPath());
                    } else if (receivedMessage.contains("get")) {
                        // client want to download a file
                        String fileName = Utils.getDataFromCommand(Utils.Actions.GET, receivedMessage);
                        File file = new File(Utils.getFolderPath()+fileName);
                        if (file.exists()){
                            SockClientTask.sendFile(sockClient, fileName, Utils.getFolderPath(), false);
                        } else {
                            sockClient.write("[GET ERROR] File not found!").send();
                        }
                    } else if (receivedMessage.contains("post")) {
                        // client want to upload file to server
                        SockClientTask.receiveFile(sockClient, Utils.getDataFromCommand(Utils.Actions.POST, receivedMessage), Utils.getFolderPath(), false);
                    } else if (receivedMessage.contains("piece")) {
                        // client want to download a piece
                        int currentPieceNumInt = currentPieceNum.incrementAndGet();
                        if (currentPieceNumInt <= piecePool.getMaxPieceId()){
                            sockClient.write("piece " + currentPieceNumInt).send();
                            piecePool.sendPiece(sockClient, currentPieceNumInt);
                            sockClient.send();
                        }
                    }
                } catch (IOException e) {
                    // close the connection after receive error
                    System.out.println("[CONNECTION " + sockClient.getClientAddress() + "] Client IO Exception!");
                    break;
                }
            } while (!receivedMessage.equals("@logout"));
            try {
                sockClient.close();
            } catch (IOException e) {
                System.out.println("[ERROR " + sockClient.getClientAddress() + "] Cannot close connection. Reason: connection already closed!");
            }
            System.out.println("[CONNECTION " + sockClient.getClientAddress() + "] Connection closed!");
        }
    }
}
