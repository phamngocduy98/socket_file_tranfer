import java.io.*;
import java.util.Scanner;

public class Main {
    public static String FOLDER_PATH = "H:\\dl\\";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
//        System.out.print("Enter FOLDER_PATH (enter 1 for default: H:\\dl\\): ");
//        String folderPath = scanner.nextLine();
//        if (!folderPath.equals("1")) {
//            FOLDER_PATH = folderPath;
//        }
        System.out.print("Enter CLIENT_ID (1,2,3,...): ");
        String clientId = scanner.nextLine();
        FOLDER_PATH += clientId + "\\";
        System.out.println("Client path = " + FOLDER_PATH);
//        System.out.print("Enter server IP: ");
        String serverIP = "192.168.98.2"; //scanner.nextLine();
//        String serverIP = "127.0.0.1";
//        System.out.print("Enter server port: ");
        int serverPort = 1259; //scanner.nextInt();
//        scanner.nextLine();
        try {
            SockClient sockClient = new SockClient(serverIP, serverPort);
            Thread socketHandleThread = new Thread(new SocketReceiveHandlerRunnable(sockClient));
            socketHandleThread.start();
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

    public static class SocketReceiveHandlerRunnable implements Runnable {
        SockClient clientSocket;

        public SocketReceiveHandlerRunnable(SockClient clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            String receivedMessage = "";
            do {
                try {
                    receivedMessage = clientSocket.readString();
                    System.out.println("[SERVER RESPONSE] " + receivedMessage);

                    if (receivedMessage.contains("ls")) {
                        SockClientTask.receiveLS(clientSocket);
                    } else if (receivedMessage.contains("get")) {
                        SockClientTask.receiveFile(clientSocket, Utils.getDataFromCommand(Utils.Actions.GET, receivedMessage), Main.FOLDER_PATH, true);
                    }
                } catch (IOException e) {
                    System.out.println("[ERROR " + clientSocket.getClientAddress() + "] Socket IO Exception. Reason: client connection may suddenly be closed!");
                    break;
                }
            } while (!receivedMessage.equals("@logout"));
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("[ERROR " + clientSocket.getClientAddress() + "] Cannot close connection. Reason: connection already closed!");
            }
            System.out.println("[CONNECTION " + clientSocket.getClientAddress() + "] Connection closed!");
        }
    }






}
