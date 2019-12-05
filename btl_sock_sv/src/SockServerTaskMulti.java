import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SockServerTaskMulti extends SockServerTask {
    private static AtomicInteger currentPieceNum = new AtomicInteger(0);

    public static class ClientThreadMulti extends ClientThread {
        PiecePool piecePool;

        public ClientThreadMulti(SockServer sockServer, SockClient sockClient) {
            super(sockServer, sockClient);
        }

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
                    if (receivedMessage.contains("ls")) {
                        SockClientTask.sendLs(sockClient, SockServer.FOLDER_PATH);
                    } else if (receivedMessage.contains("get")) {
                        new SockClientTask.SendFileThread(sockClient, Utils.getDataFromCommand(Utils.Actions.GET, receivedMessage), SockServer.FOLDER_PATH, false).start();
                    } else if (receivedMessage.contains("post")) {
                        SockClientTask.receiveFile(sockClient, Utils.getDataFromCommand(Utils.Actions.POST, receivedMessage), SockServer.FOLDER_PATH, false);
                    } else if (receivedMessage.contains("piece")) {
                        int pieceId = Integer.parseInt(Utils.getDataFromCommand(Utils.Actions.PIECE, receivedMessage));
                        if (pieceId > piecePool.getMaxPieceId()) {
                            System.out.println("[ERROR] CLient request invalid piece id " + pieceId);
//                            continue;
                        }
                        int currentPieceNumInt = currentPieceNum.incrementAndGet();
                        sockClient.write("piece " + currentPieceNumInt).send();
                        piecePool.sendPiece(sockClient, currentPieceNumInt);
                        sockClient.send();
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

    public static boolean broastcastFile(SockServer sockServer, File file) throws IOException {
        ArrayList<SockClient> clients = sockServer.getClients();
        for (SockClient sockClient : clients) {
            sockClient.write("broadcast " + file.getName());
            sockClient.write(file.length());
            sockClient.write((long) clients.size() - 1);
            for (SockClient sockCl : clients) {
                if (!sockCl.getIP().equals(sockClient.getIP())) {
                    sockClient.write(sockCl.getIP());
                }
            }
            sockClient.send();
        }
        System.out.println("[BROADCAST POST] File '" + file.getName() + "' tranfer started");
        return true;
    }
}
