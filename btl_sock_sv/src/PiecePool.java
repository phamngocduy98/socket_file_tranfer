import java.io.*;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PiecePool {
    private RandomAccessFile file = null;
    private Map<Integer, Boolean> ownedPiece = null;
    private FileInputStream fis = null;
    String fileName;
    long fileSize;
    int maxPieceId;

    public PiecePool(String fileName, long fileSize) throws IOException {
        this.setFileName(fileName, fileSize);
    }

    public PiecePool() {
    }

    public void setFileName(String fileName, long fileSize) throws IOException {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.maxPieceId = (int) ((fileSize - 1) / Utils.PIECE_SIZE);
        this.ownedPiece = Collections.synchronizedMap(new HashMap<>());
        if (file != null) {
            file.close();
        }
        file = new RandomAccessFile(Utils.getFolderPath() + fileName, "rw");
        if (fis != null) {
            fis.close();
        }
        fis = new FileInputStream(Utils.getFolderPath() + fileName);
    }

    public void sendOwnedPiece(SockClient sockClient, int pieceId) throws IOException {
        if (isOwnedPiece(pieceId)) {
            this.sendPiece(sockClient, pieceId);
        }
    }

    public synchronized boolean isOwnedPiece(int pieceId) {
        return ownedPiece.getOrDefault(pieceId, false);
    }

    public boolean sendNextPiece(SockClient sockClient, int nextPieceId) throws IOException {
        if (nextPieceId > maxPieceId) return false;

        byte[] readBuffer = new byte[Utils.PIECE_SIZE];
        int bytesRead = 0, bytesToRead;
        if (nextPieceId == maxPieceId) {
            bytesToRead = (int) (fileSize - maxPieceId * Utils.PIECE_SIZE);
        } else {
            bytesToRead = Utils.PIECE_SIZE;
        }
        while (bytesRead < bytesToRead) {
            int result = fis.read(readBuffer, bytesRead, bytesToRead - bytesRead);
            bytesRead += result;
        }
        sockClient.write(readBuffer, 0, bytesToRead).send();
        readBuffer = null;
        return true;
    }

    public boolean sendPiece(SockClient sockClient, int pieceId) throws IOException {
        if (pieceId > maxPieceId) return false;

        byte[] readBuffer = new byte[Utils.PIECE_SIZE];
        int fileOffset = pieceId * Utils.PIECE_SIZE, bytesRead = 0, bytesToRead;
        if (pieceId == maxPieceId) {
            bytesToRead = (int) (fileSize - maxPieceId * Utils.PIECE_SIZE);
        } else {
            bytesToRead = Utils.PIECE_SIZE;
        }
        while (bytesRead < bytesToRead) {
            synchronized (file) {
                file.seek(fileOffset + bytesRead);
                bytesRead += file.read(readBuffer, bytesRead, bytesToRead - bytesRead);
            }
        }
        long checksum = Utils.checksum(readBuffer);
//        Utils.showProgress(Utils.Actions.UPLOAD, maxPieceId * Utils.PIECE_SIZE, (maxPieceId - pieceId) * Utils.PIECE_SIZE, Utils.startTime);
//        System.out.println("send piece checksum="+checksum);
        sockClient.write(readBuffer, 0, bytesToRead).send();
        readBuffer = null;
        return true;
    }

    public void receivePiece(SockClient sockClient, int pieceId) throws IOException {
        byte[] receiveBytes = new byte[Utils.PIECE_SIZE];
        receivePiece(sockClient, pieceId, receiveBytes);
        receiveBytes = null;
    }

    public void receivePiece(SockClient sockClient, int pieceId, byte[] writeBuffer) throws IOException {
        int fileOffset = pieceId * Utils.PIECE_SIZE, bytesRead = 0, bytesToRead;
        if (pieceId == maxPieceId) {
            bytesToRead = (int) (fileSize - maxPieceId * Utils.PIECE_SIZE);
        } else {
            bytesToRead = Utils.PIECE_SIZE;
        }
        while (bytesRead < bytesToRead) {
            bytesRead += sockClient.read(writeBuffer, bytesRead, bytesToRead - bytesRead);
        }
        synchronized (file) {
            file.seek(fileOffset);
            file.write(writeBuffer);
        }
        synchronized (ownedPiece) {
            ownedPiece.put(pieceId, true);
        }
//        long checksum = Utils.checksum(writeBuffer);
//        System.out.println("receive piece checksum="+checksum);
        Utils.showPieceProgress(Utils.Actions.DOWNLOAD, maxPieceId , ownedPiece.size(), Utils.startTime);
//        System.out.println("[PIECE] "+pieceId+"/"+maxPieceId+" (size="+bytesToRead+")");
        if (ownedPiece.size() > maxPieceId) {
            double duration = (double) (System.currentTimeMillis() - Utils.startTime) / 1000d;
            System.out.println("[BROADCAST P2P] Download complete in" + duration + "s . Speed = " + String.format("%.1f",fileSize / 1024d / duration) + " kB/s");
            file.close();
            file = null;
        }
//        return checksum;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getMaxPieceId() {
        return maxPieceId;
    }

}
