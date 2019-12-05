import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SockServerTask {

    public static class AcceptClientThread extends Thread {
        SockServer sockServer;

        public AcceptClientThread(SockServer sockServer) {
            this.sockServer = sockServer;
        }

        public void newClientThread(SockClient sockClient){
            Thread clientThread = new ClientThread(sockServer, sockClient);
            clientThread.start();
        }

        @Override
        public void run() {
            while (true) {
                SockClient sockClient = null;
                try {
                    sockClient = sockServer.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("[CONNECTION " + sockServer.getClients().size() + "] Accept new client: " + sockClient.getClientAddress());
                System.out.print(">");
                newClientThread(sockClient);
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
                        SockClientTask.sendLs(sockClient,SockServer.FOLDER_PATH);
                    } else if (receivedMessage.contains("get")) {
                        new SockClientTask.SendFileThread(sockClient, Utils.getDataFromCommand(Utils.Actions.GET, receivedMessage), SockServer.FOLDER_PATH, false).start();
                    } else if (receivedMessage.contains("post")) {
                        SockClientTask.receiveFile(sockClient, Utils.getDataFromCommand(Utils.Actions.POST, receivedMessage), SockServer.FOLDER_PATH, false);
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

    public static boolean broastcastFile(SockServer sockServer, String fileName) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        ArrayList<SockClient> clients = sockServer.getClients();
        ExecutorService es = Executors.newCachedThreadPool();
        for (SockClient sockClient : clients) {
            es.execute(new SockClientTask.SendFileThread(sockClient, fileName, SockServer.FOLDER_PATH, false));
        }
        es.shutdown();
        boolean finished = es.awaitTermination(30, TimeUnit.MINUTES);
        System.out.println("[BROADCAST POST] File '" + fileName + "' tranfer completed in " + (System.currentTimeMillis() - startTime) + " ms");
        return finished;
    }
}
