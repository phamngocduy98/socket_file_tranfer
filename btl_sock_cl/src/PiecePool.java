import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PiecePool {
    RandomAccessFile file;
    private Map<Integer, Boolean> ownedPiece;
    long fileSize;
    int maxPieceId;

    public PiecePool(String fileName, long fileSize) throws FileNotFoundException {
        this.setFileName(fileName, fileSize);
    }

    public PiecePool() {}

    public void setFileName(String fileName, long fileSize) throws FileNotFoundException {
        this.fileSize = fileSize;
        maxPieceId = (int) ((fileSize-1)/Utils.BUFFER_SIZE);
        file = new RandomAccessFile(Utils.getFolderPath()+fileName, "rw");
        ownedPiece = Collections.synchronizedMap(new HashMap<>());
    }

    public void sendOwnedPiece(SockClient sockClient, int pieceId) throws IOException {
        if (isOwnedPiece(pieceId)) {
            this.sendPiece(sockClient, pieceId);
        }
    }

    public synchronized boolean isOwnedPiece(int pieceId) {
        return ownedPiece.getOrDefault(pieceId, false);
    }

    public boolean sendPiece(SockClient sockClient, int pieceId) throws IOException {
        if (pieceId > maxPieceId) return false;

        byte[] readBuffer = new byte[Utils.BUFFER_SIZE];
        int fileOffset = pieceId * Utils.BUFFER_SIZE, bytesRead = 0, bytesToRead;
        if (pieceId == maxPieceId){
            bytesToRead = (int)(fileSize - maxPieceId*Utils.BUFFER_SIZE);
        } else {
            bytesToRead = Utils.BUFFER_SIZE;
        }
        while (bytesRead < bytesToRead) {
            synchronized (file){
                file.seek(fileOffset + bytesRead);
                int result = file.read(readBuffer, bytesRead, bytesToRead - bytesRead);
                bytesRead += result;
            }
        }
        long checksum = Utils.checksum(readBuffer);
        if (pieceId % 2 == 0){
            Utils.showProgress(Utils.Actions.UPLOAD, maxPieceId*Utils.BUFFER_SIZE,(maxPieceId-pieceId)*Utils.BUFFER_SIZE, Utils.startTime);
        }
//        System.out.println("send piece checksum="+checksum);
        sockClient.write(readBuffer, 0, bytesToRead).send();
        return true;
    }

    public long receivePiece(SockClient sockClient, int pieceId) throws IOException {
        byte[] receiveBytes = new byte[Utils.BUFFER_SIZE];
        return receivePiece(sockClient, pieceId, receiveBytes);
    }

    public long receivePiece(SockClient sockClient, int pieceId, byte[] writeBuffer) throws IOException {
        int fileOffset = pieceId * Utils.BUFFER_SIZE, bytesRead = 0, bytesToRead;
        if (pieceId == maxPieceId){
            bytesToRead = (int)(fileSize - maxPieceId*Utils.BUFFER_SIZE);
        } else {
            bytesToRead = Utils.BUFFER_SIZE;
        }
        while (bytesRead < bytesToRead) {
            int result;
            result = sockClient.read(writeBuffer, bytesRead, bytesToRead - bytesRead);
            bytesRead += result;
        }
        synchronized (file){
            file.seek(fileOffset);
            file.write(writeBuffer);
        }
        synchronized (ownedPiece){
            ownedPiece.put(pieceId, true);
        }
        long checksum = Utils.checksum(writeBuffer);
//        System.out.println("receive piece checksum="+checksum);
        if (pieceId % 2 == 0){
            Utils.showProgress(Utils.Actions.DOWNLOAD, maxPieceId*Utils.BUFFER_SIZE,(maxPieceId-pieceId)*Utils.BUFFER_SIZE, Utils.startTime);
        }
        if (ownedPiece.size() > maxPieceId){
            System.out.println("COMPLETE!");
//            close();
        }
        return checksum;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getMaxPieceId() {
        return maxPieceId;
    }

    public void close() throws IOException {
        file.close();
    }
}
