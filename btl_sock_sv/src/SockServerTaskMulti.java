import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SockServerTaskMulti extends SockServerTask {
    public static boolean broastcastFile(SockServer sockServer, File file) throws IOException {
        ArrayList<SockClient> clients = sockServer.getClients();
        String fileName = file.getName();
        long fileSize = file.length(), clientSize = (long) clients.size() - 1;
        for (SockClient sockClient : clients) {
            sockClient.write("broadcast " + fileName);
            sockClient.write(fileSize);
            sockClient.write(clientSize);
            for (SockClient sockCl : clients) {
                if (!sockCl.getIP().equals(sockClient.getIP())) {
                    sockClient.write(sockCl.getIP());
                }
            }
            sockClient.send();
        }
        System.out.println("[BROADCAST FILE (P2P)] Broadcast file '" + fileName + "' started!");
        return true;
    }
}
