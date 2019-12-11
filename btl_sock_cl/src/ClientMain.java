import java.io.*;
import java.util.Scanner;

public class ClientMain {
    public static PiecePool piecePool = new PiecePool();
    private static SockServer friendSockServer;
    private static String serverIP = "192.168.98.2";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        File folder = new File(Utils.getFolderPath());

        while(!folder.isDirectory() || !folder.exists()){
            System.out.print("Enter CLIENT_FOLDER_PATH (H:/dl/): ");
            Utils.FOLDER_PATH = scanner.nextLine()+"/";
            folder = new File(Utils.getFolderPath());
        }
//        System.out.print("Enter CLIENT_ID for subfolder name (1,2,3,...): ");
//        String clientId = scanner.nextLine();
//        Utils.FOLDER_PATH = Utils.FOLDER_PATH + clientId + "/";
        System.out.println("Client path = " + Utils.getFolderPath());
        System.out.print("Enter server IP: ");
        serverIP = scanner.nextLine();
//        System.out.print("Enter server port: ");
        int serverPort = 1259; //scanner.nextInt();
//        scanner.nextLine();
        try {
            SockClient sockClient = new SockClient(serverIP, serverPort);
            new SocketReceiveHandlerThread(sockClient).start();

            try {
                friendSockServer = new SockServer(1210);
            } catch (IOException e) {
                System.out.println("[ERROR] Client bind error, check server ip and port!");
                return;
            }
            new Thread(new SockServerTask.AcceptClientThread(friendSockServer) {
                @Override
                public void newClientThread(SockClient sockClient) {
                    new FriendReceivePieceThread(sockServer, sockClient, piecePool).start();
                }
            }).start();
            System.out.print(">");
            while (true) {
                String command = scanner.nextLine();
                if (command.equals("ls")) {
                    Utils.selfLs();
                } else if (command.equals("list")) {
                    sockClient.write("list").send();
                } else if (command.contains("get")) {
                    sockClient.write(command).send();
                } else if (command.contains("post")) {
                    SockClientTask.sendFile(sockClient, Utils.getDataFromCommand(Utils.Actions.POST, command), Utils.FOLDER_PATH, true);
                } else if (command.equals("@logout")) {
                    sockClient.write(command).send();
                    sockClient.close();
                    break;
                } else {
                    System.out.println("Invalid command!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class SocketReceiveHandlerThread extends Thread {
        SockClient sockClient;
        FileTranferP2P fileTranferP2P = null;

        public SocketReceiveHandlerThread(SockClient clientSocket) {
            this.sockClient = clientSocket;
        }

        @Override
        public void run() {
            String receivedMessage = "";
            do {
                try {
                    receivedMessage = sockClient.readString();
//                    System.out.println("[SERVER RESPONSE] " + receivedMessage);

                    if (receivedMessage.contains("ERROR")) {
                        // server response a error message
                        System.out.println(receivedMessage);
                    } else if (receivedMessage.contains("list")) {
                        // server response list of file on server
                        SockClientTask.receiveLS(sockClient);
                    } else if (receivedMessage.contains("post")) {
                        // server response requested file (client download file that he requested)
                        SockClientTask.receiveFile(sockClient, Utils.getDataFromCommand(Utils.Actions.POST, receivedMessage), Utils.getFolderPath(), true);
                    } else if (receivedMessage.contains("broadcast")) {
                        // server want to broadcast a file, tell client to download this file.
                        System.out.println("[BROADCAST P2P] Server is now ready to send pieces");
                        FileTranferP2P.Data data = FileTranferP2P.receiveBroadcastData(sockClient, piecePool, Utils.getDataFromCommand(Utils.Actions.BROADCAST, receivedMessage));
                        System.out.println(data.toString());
                        piecePool.setFileName(data.fileName, data.fileSize);
                        fileTranferP2P = new FileTranferP2P(data);
                        fileTranferP2P.nextPiece(sockClient);
                        Utils.setStartTime();
                    } else if (receivedMessage.contains("piece")) {
                        // server will send a piece
//                        String pieceId = Utils.getDataFromCommand(Utils.Actions.PIECE, receivedMessage);
//                        System.out.println("Server piece "+pieceId);
                        fileTranferP2P.receivePiece(sockClient, piecePool, Utils.getDataFromCommand(Utils.Actions.PIECE, receivedMessage));
                        fileTranferP2P.nextPiece(sockClient);
                        continue;
                    }
                    System.out.print(">");
                } catch (IOException e) {
                    System.out.println("[ERROR " + sockClient.getClientAddress() + "] Socket IO Exception"+ e.getMessage());
                    e.printStackTrace();
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


    public static class FriendReceivePieceThread extends Thread {
        PiecePool piecePool;
        SockServer sockServer;
        SockClient sockClient;

        public FriendReceivePieceThread(SockServer sockServer, SockClient sockClient, PiecePool piecePool) {
            this.piecePool = piecePool;
            this.sockServer = sockServer;
            this.sockClient = sockClient;
        }

        @Override
        public void run() {
            String receivedMessage = "";
            do {
                try {
                    receivedMessage = sockClient.readString();
//                    System.out.println("[RECEIVE FRIEND PIECE " + sockClient.getClientAddress() + "] " + receivedMessage);
                    if (receivedMessage.contains("piece")) {
                        int pieceId = Integer.parseInt(Utils.getDataFromCommand(Utils.Actions.PIECE, receivedMessage));
                        piecePool.receivePiece(sockClient, pieceId);
                    }
                } catch (IOException e) {
                    System.out.println("[CONNECTION " + sockClient.getClientAddress() + "] RequestFriend IO Exception!");
                    break;
                }
            } while (!receivedMessage.equals("@logout"));
            try {
                sockClient.close();
            } catch (IOException e) {
                System.out.println("[ERROR " + sockClient.getClientAddress() + "] FriendRequest connection already closed!");
            }
            System.out.println("[CONNECTION " + sockClient.getClientAddress() + "] FriendRequest connection closed!");
        }
    }
}
