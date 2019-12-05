import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SockServerTaskMulti extends SockServerTask {
    public static class AcceptClientThreadMulti extends AcceptClientThread {

        public AcceptClientThreadMulti(SockServer sockServer) {
            super(sockServer);
        }

        @Override
        public void newClientThread(SockClient sockClient) {
            Thread clientThread = new ClientThreadMulti(sockServer, sockClient);
            clientThread.start();
        }
    }
    public static class ClientThreadMulti extends ClientThread {

        public ClientThreadMulti(SockServer sockServer, SockClient sockClient) {
            super(sockServer, sockClient);
        }
    }

    public static boolean broastcastFile(SockServer sockServer, String fileName) throws IOException, InterruptedException {
        File file = new File(fileName);
        if (!file.exists()) return false;

        ArrayList<SockClient> clients = sockServer.getClients();
        for (SockClient sockClient : clients) {
            sockClient.write("broadcast "+fileName);
            sockClient.write(file.length());
            for (SockClient sockCl : clients) {
                if (!sockCl.equals(sockClient)){
                    sockClient.write(sockCl.getIP());
                }
            }
        }
        System.out.println("[BROADCAST POST] File '" + fileName + "' tranfer started");
        return true;
    }
}
