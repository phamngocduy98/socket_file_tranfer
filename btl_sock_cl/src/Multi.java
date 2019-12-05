import java.io.*;
import java.util.Scanner;

public class Multi {
    public static String FOLDER_PATH = "H:\\dl\\";
    public static PiecePool piecePool = new PiecePool();
    private static SockServer friendSockServer;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
//        System.out.print("Enter FOLDER_PATH (enter 1 for default: H:\\dl\\): ");
//        String folderPath = scanner.nextLine();
//        if (!folderPath.equals("1")) {
//            FOLDER_PATH = folderPath;
//        }
        System.out.print("Enter CLIENT_ID (1,2,3,...): ");
        String clientId = scanner.nextLine();
        FOLDER_PATH = FOLDER_PATH + clientId + "\\";
        System.out.println("Client path = " + Utils.getFolderPath());
//        System.out.print("Enter server IP: ");
        String serverIP = "192.168.98.2"; //scanner.nextLine();
//        String serverIP = "127.0.0.1";
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
            new Thread(new SockServerTask.AcceptClientThread(friendSockServer){
                @Override
                public void newClientThread(SockClient sockClient) {
                    new FriendReceivePieceThread(sockServer, sockClient, piecePool).start();
                }
            }).start();

            while (true) {
                System.out.print(">");
                String command = scanner.nextLine();
                if (command.equals("ls")) {
                    Utils.selfLs();
                } else if (command.equals("list")) {
                    sockClient.write("ls").send();
                } else if (command.contains("get")) {
                    sockClient.write(command).send();
                } else if (command.contains("post")) {
                    SockClientTask.sendFile(sockClient, Utils.getDataFromCommand(Utils.Actions.POST, command), FOLDER_PATH, true);
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
        FileTranferMultiClient fileTranferMulti = null;

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

                    if (receivedMessage.contains("ls")) {
                        // server response list of file on server
                        SockClientTask.receiveLS(sockClient);
                    } else if (receivedMessage.contains("get")) {
                        // server response requested file (client download file that he requested)
                        SockClientTask.receiveFile(sockClient, Utils.getDataFromCommand(Utils.Actions.GET, receivedMessage), Main.FOLDER_PATH, true);
                    } else if (receivedMessage.contains("broadcast")){
                        // server want to broadcast a file, tell client to download this file.
                        FileTranferMultiClient.Data data = FileTranferMultiClient.receiveBroadcastData(sockClient, piecePool, Utils.getDataFromCommand(Utils.Actions.BROADCAST, receivedMessage));
                        System.out.println(data.toString());
                        piecePool.setFileName(data.fileName, data.fileSize);
                        fileTranferMulti = new FileTranferMultiClient(data);
                        fileTranferMulti.nextPiece(sockClient, piecePool, "-1");
                        Utils.setStartTime();
                    } else if (receivedMessage.contains("piece")){
                        // server will send a piece
                        fileTranferMulti.receivePiece(sockClient, piecePool, Utils.getDataFromCommand(Utils.Actions.PIECE,receivedMessage));
                        fileTranferMulti.nextPiece(sockClient, piecePool, Utils.getDataFromCommand(Utils.Actions.PIECE,receivedMessage));
                    } else if (receivedMessage.contains("friend")){
                        // a friend already own this piece, he will send you soon
                        fileTranferMulti.nextPiece(sockClient, piecePool, Utils.getDataFromCommand(Utils.Actions.FRIEND,receivedMessage));
                    }
                } catch (IOException e) {
                    System.out.println("[ERROR " + sockClient.getClientAddress() + "] Socket IO Exception");
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


    public static class FriendReceivePieceThread extends SockServerTask.ClientThread {
        PiecePool piecePool;
        public FriendReceivePieceThread(SockServer sockServer, SockClient sockClient, PiecePool piecePool) {
            super(sockServer, sockClient);
            this.piecePool = piecePool;
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
