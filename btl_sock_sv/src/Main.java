import java.io.*;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        SockServer sockServer = null;

//            System.out.print("Enter server ip: ");
        String ip = "192.168.98.2"; //scanner.nextLine();
//            String ip = "127.0.0.1";'
        try {
            sockServer = new SockServer(ip, 1259);
            System.out.println("[SERVER] Listening " + sockServer.getServerIP() + ":" + sockServer.getServerPort());
        } catch (UnknownHostException e) {
            System.out.println("[ERROR] Server bind error, check server host ip");
        } catch (IOException e) {
            System.out.println("[ERROR] Server bind error, IO Exception, check server ip and port!");
            return;
        }
        new Thread(new SockServerTask.AcceptClientThread(sockServer){
            @Override
            public void newClientThread(SockClient sockClient) {
                new ClientThread(sockServer, sockClient).start();
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
            } else {
                System.out.println("Invalid command!");
            }
        }
    }

    public static class ClientThread extends Thread {
        SockServer sockServer;
        SockClient sockClient;

        public ClientThread(SockServer sockServer, SockClient sockClient) {
            this.sockServer = sockServer;
            this.sockClient = sockClient;
        }

        @Override
        public void run() {
            String receivedMessage = "";
            do {
                try {
                    receivedMessage = sockClient.readString();
                    System.out.println("[COMMAND " + sockClient.getClientAddress() + "] " + receivedMessage);
                    if (receivedMessage.contains("ls")) {
                        SockClientTask.sendLs(sockClient,Utils.getFolderPath());
                    } else if (receivedMessage.contains("get")) {
                        new SockClientTask.SendFileThread(sockClient, Utils.getDataFromCommand(Utils.Actions.GET, receivedMessage), Utils.getFolderPath(), false).start();
                    } else if (receivedMessage.contains("post")) {
                        SockClientTask.receiveFile(sockClient, Utils.getDataFromCommand(Utils.Actions.POST, receivedMessage), Utils.getFolderPath(), false);
                    }
                } catch (IOException e) {
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
