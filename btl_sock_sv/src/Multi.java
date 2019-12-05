import java.io.*;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Multi {
    private static PiecePool piecePool;
    public static void main(String[] args) throws FileNotFoundException {
        Scanner scanner = new Scanner(System.in);
        SockServer sockServer = null;
        piecePool = new PiecePool();

//            System.out.print("Enter server ip: ");
        String ip = "192.168.98.2"; //scanner.nextLine();
//            String ip = "127.0.0.1";
        try {
            sockServer = new SockServer(ip, 1259);
            System.out.println("[SERVER] Listening " + sockServer.getServerIP() + ":" + sockServer.getServerPort());
        } catch (UnknownHostException e) {
            System.out.println("[ERROR] Server bind error, check server ip");
        } catch (IOException e) {
            System.out.println("[ERROR] Server bind error, check server ip and port!");
            return;
        }
        new Thread(new SockServerTask.AcceptClientThread(sockServer){
            @Override
            public void newClientThread(SockClient sockClient) {
                Thread clientThread = new SockServerTaskMulti.ClientThreadMulti(sockServer, sockClient, piecePool);
                clientThread.start();
            }
        }).start();
        while (true) {
            System.out.print(">");
            String command = scanner.nextLine();
            if (command.contains("ls")) {
                Utils.selfLs();
            } else if (command.contains("post")) {
                try {
                    SockServerTask.broastcastFile(sockServer, Utils.getDataFromCommand(Utils.Actions.POST, command));
                } catch (IOException e) {
                    System.out.println("[BROADCAST ERROR] File broadcast IO Error!");
                    continue;
                } catch (InterruptedException e) {
                    System.out.println("[BROADCAST ERROR] File broadcast interupted!");
                    continue;
                }
            }else if (command.contains("broadcast")) {
                try {
                    String fileName = Utils.getDataFromCommand(Utils.Actions.BROADCAST, command);
                    File file = new File(Utils.getFolderPath()+fileName);
                    if (!file.exists()) {
                        System.out.println("[ERROR] File not exist to broadcast!");
                        continue;
                    }
                    SockServerTaskMulti.broastcastFile(sockServer, file);
                    piecePool.setFileName(file.getName(), file.length());
                    Utils.setStartTime();
                } catch (IOException e) {
                    System.out.println("[BROADCAST ERROR] File broadcast IO Error!");
                    continue;
                }
            } else {
                System.out.println("Invalid command!");
            }
        }
    }
}
