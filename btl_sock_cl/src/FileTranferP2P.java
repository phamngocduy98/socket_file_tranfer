import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.util.ArrayList;

public class FileTranferP2P {
    private Data data;

    public FileTranferP2P(Data data) throws FileNotFoundException {
        this.data = data;
    }

    public void receivePiece(SockClient sockClient, PiecePool piecePool, String pieceIdStr) throws IOException {
//        System.out.println("receive from server piece "+pieceIdStr);
        int pieceId = Integer.parseInt(pieceIdStr);
        byte[] receiveBytes = new byte[Utils.PIECE_SIZE];
        piecePool.receivePiece(sockClient, pieceId, receiveBytes);
        for (SockClient client : this.data.friendsSock) {
//            System.out.println("Share piece"+pieceIdStr+" = "+Utils.checksum(receiveBytes));
            client.writeQueue("piece "+pieceIdStr);
            client.writeQueue(receiveBytes, 0, receiveBytes.length).send();
//            piecePool.sendOwnedPiece(client, pieceId);
        }
        receiveBytes = null;
    }

    public void nextPiece(SockClient sockClient) throws IOException {
        sockClient.write("piece").send();
    }

    public static class Data implements SockClient.OnCloseListener{
        String fileName;
        long fileSize;
        ArrayList<String> friendIPs;
        ArrayList<SockClient> friendsSock;
        PiecePool piecePool;

        public Data(String fileName, long fileSize, PiecePool piecePool) {
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.friendIPs = new ArrayList<>();
            this.friendsSock = new ArrayList<>();
            this.piecePool = piecePool;
        }

        public void addFriend(String ip) throws IOException {
            this.friendIPs.add(ip);
            SockClient friendClSock = new SockClient(ip, 1210, (SockClient.OnCloseListener) this);
            this.friendsSock.add(friendClSock);
        }

        public void removeFriend(SockClient sockClient){
            int index = this.friendsSock.indexOf(sockClient);
            if (index < 0) index = this.friendsSock.indexOf(sockClient);
            if (index < 0) return;
            this.friendIPs.remove(index);
            this.friendsSock.remove(index);
        }

        @Override
        public String toString() {
            return "Data{" +
                    "fileName='" + fileName + '\'' +
                    ", fileSize=" + fileSize + '\'' +
                    ", clientSize=" + friendIPs.size() +
                    '}';
        }

        @Override
        public void onClientClose(SockClient sockClient) {
            this.removeFriend(sockClient);
        }
    }
    public static Data receiveBroadcastData(SockClient sockClient, PiecePool piecePool, String fileName) throws IOException {
        long fileSize = sockClient.readLong();
        long clientsCount = sockClient.readLong();
        Data data = new Data(fileName, fileSize, piecePool);
        for (int i=0;i<clientsCount;i++){
            String friendIP = sockClient.readString();
            data.addFriend(friendIP);
        }
        return data;
    }
}
