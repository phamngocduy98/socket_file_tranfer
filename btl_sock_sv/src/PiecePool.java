import java.io.*;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class PiecePool {
    private Map<Integer, String> ownedPiece = null;
    private FileInputStream fis = null;
    String fileName;
    long fileSize;
    int maxPieceId;
    AtomicInteger readCall = new AtomicInteger(0), writeCall = new AtomicInteger(0);

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
        if (fis != null) {
            fis.close();
        }
        fis = new FileInputStream(Utils.getFolderPath() + fileName);
    }

//    @Deprecated
//    public void sendOwnedPiece(SockClient sockClient, int pieceId) throws IOException {
//        if (isOwnedPiece(pieceId)) {
//            this.sendPiece(sockClient, pieceId);
//        }
//    }

    public synchronized boolean isOwnedPiece(int pieceId) {
        return !ownedPiece.getOrDefault(pieceId, "").equals("");
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
            writeCall.incrementAndGet();
            bytesRead += result;
        }
        sockClient.write(readBuffer, 0, bytesToRead).send();
        readBuffer = null;
        return true;
    }

//    @Deprecated
//    public boolean sendPiece(SockClient sockClient, int pieceId) throws IOException {
//        if (pieceId > maxPieceId) return false;
//
//        byte[] readBuffer = new byte[Utils.PIECE_SIZE];
//        int fileOffset = pieceId * Utils.PIECE_SIZE, bytesRead = 0, bytesToRead;
//        if (pieceId == maxPieceId) {
//            bytesToRead = (int) (fileSize - maxPieceId * Utils.PIECE_SIZE);
//        } else {
//            bytesToRead = Utils.PIECE_SIZE;
//        }
//        while (bytesRead < bytesToRead) {
//            synchronized (this) {
//                file.seek(fileOffset + bytesRead);
//                bytesRead += file.read(readBuffer, bytesRead, bytesToRead - bytesRead);
//            }
//        }
//        long checksum = Utils.checksum(readBuffer);
////        Utils.showProgress(Utils.Actions.UPLOAD, maxPieceId * Utils.PIECE_SIZE, (maxPieceId - pieceId) * Utils.PIECE_SIZE, Utils.startTime);
////        System.out.println("send piece checksum="+checksum);
//        sockClient.write(readBuffer, 0, bytesToRead).send();
//        readBuffer = null;
//        return true;
//    }

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
            readCall.incrementAndGet();
            bytesRead += sockClient.read(writeBuffer, bytesRead, bytesToRead - bytesRead);
        }
        String pieceFileName = Utils.getFolderPath() + fileName + "." + pieceId;
        BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(pieceFileName));
        fout.write(writeBuffer, 0, bytesRead);
        fout.close();
        ownedPiece.put(pieceId, pieceFileName);
//        long checksum = Utils.checksum(writeBuffer);
//        System.out.println("receive piece checksum="+checksum);
        Utils.showPieceProgress(Utils.Actions.DOWNLOAD, maxPieceId, ownedPiece.size(), Utils.startTime);
//        System.out.println("[PIECE] "+pieceId+"/"+maxPieceId+" (size="+bytesToRead+")");
        if (ownedPiece.size() > maxPieceId) {
            double duration = (double) (System.currentTimeMillis() - Utils.startTime) / 1000d;
//            System.out.println("read call = " + readCall.getAndSet(0) + "/" + maxPieceId + " writecall = " + writeCall.getAndSet(0) + "|||||");
            System.out.println("[BROADCAST P2P] Download complete in" + duration + "s . Speed = " + String.format("%.1f", fileSize / 1024d / duration) + " kB/s");
            mergePieces();
        }
//        return checksum;
    }

    public void mergePieces() throws IOException {
        byte[] bufferBytes = new byte[Utils.PIECE_SIZE];
        FileOutputStream fout = new FileOutputStream(Utils.getFolderPath()+fileName);
        for (int i = 0; i <= maxPieceId; i++) {
            FileInputStream fin = new FileInputStream(ownedPiece.get(i));
            while (true) {
                int bytesRead = fin.read(bufferBytes);
                if (bytesRead == -1) break;
                else {
                    fout.write(bufferBytes, 0, bytesRead);
                }
            }
            fin.close();
            fin = null;
        }
        fout.close();
    }

    public long getFileSize() {
        return fileSize;
    }

    public int getMaxPieceId() {
        return maxPieceId;
    }

}
