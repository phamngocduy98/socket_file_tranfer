import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SockServerTask {

    public static abstract class AcceptClientThread extends Thread {
        SockServer sockServer;
        public AcceptClientThread(SockServer sockServer) {
            this.sockServer = sockServer;
        }
        public abstract void newClientThread(SockClient sockClient);

        @Override
        public void run() {
            while (true) {
                try {
                    SockClient sockClient = sockServer.accept();
                    System.out.println("[CONNECTION " + sockServer.getClients().size() + "] Accept new client: " + sockClient.getClientAddress());
                    newClientThread(sockClient);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean broastcastFile(SockServer sockServer, String fileName) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        ArrayList<SockClient> clients = sockServer.getClients();
        ExecutorService es = Executors.newCachedThreadPool();
        System.out.println("[BROADCAST FILE] (Sv-Cl)] Broadcast file '" + fileName + "' started");
        for (SockClient sockClient : clients) {
            es.execute(new SockClientTask.SendFileThread(sockClient, fileName, Utils.getFolderPath(), false));
        }
        es.shutdown();
        boolean finished = es.awaitTermination(30, TimeUnit.MINUTES);
        System.out.println("[BROADCAST FILE] (Sv-Cl)] Broadcast file '" + fileName + "' completed in " + ((double)System.currentTimeMillis() - startTime)/1000d + " s");
        return finished;
    }
}
