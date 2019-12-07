import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class SockClientTask {
    public static class SendFileThread extends Thread {
        SockClient sockClient;
        String path, fileName;
        boolean showProgress;

        public SendFileThread(SockClient sockClient, String fileName, String path, boolean showProgress) {
            this.sockClient = sockClient;
            this.fileName = fileName;
            this.path = path;
            this.showProgress = showProgress;
        }

        @Override
        public void run() {
            try {
                sendFile(sockClient, fileName, path, showProgress);
            } catch (IOException e) {
                System.out.println("[SEND ERROR] IO Exception!");
            }
        }
    }

    public static void sendLs(SockClient sockClient, String path) throws IOException {
        sockClient.write("list");
        String[] fileNames = Utils.listFiles(path);
        for (String fileName : fileNames) {
            sockClient.write(fileName);
        }
        sockClient.write("[LIST] END");
        sockClient.send();
    }
    public static void receiveLS(SockClient sockClient) throws IOException {
        String data;
        do {
            data = sockClient.readString();
            if (!data.contains("END")) {
                System.out.println("```" + data);
            }
        }
        while (!data.contains("END"));
    }

    public static void sendFile(SockClient sockClient, String fileName, String path, boolean showProgress) throws IOException {
        File file = new File(path + fileName);
        if (file.exists()) {
            sockClient.write("post "+fileName);
            long startTime = System.currentTimeMillis();
            long fileLen = file.length();
            sockClient.write(fileLen).send();
            System.out.println("[SEND " + ((Utils.SOURCE== Utils.Sources.SERVER)?sockClient.getClientAddress():"") + "] Sending File '" + fileName + "'. size = " + fileLen);
            FileInputStream fis = new FileInputStream(path + fileName);

            long bytesRemainToRead = fileLen;
            long bytesRead = 0;
            int packetNum = 0;
            byte[] readBuffer = new byte[Utils.BUFFER_SIZE];
            while (bytesRemainToRead > 0) {
                packetNum++;
                if (bytesRemainToRead < Utils.BUFFER_SIZE) {
                    bytesRead = bytesRemainToRead;
                } else {
                    bytesRead = Utils.BUFFER_SIZE;
                }
                int result = fis.read(readBuffer, 0, (int) bytesRead);
                if (result <= 0) continue;
                bytesRemainToRead -= result;
                sockClient.write(readBuffer, 0, result);
                if (showProgress && packetNum % 100 == 0) {
                    Utils.showProgress(Utils.Actions.UPLOAD, fileLen, bytesRemainToRead, startTime);
                }
            }
            sockClient.send();
            fis.close();
            System.out.println("[SEND " + ((Utils.SOURCE==Utils.Sources.SERVER)?sockClient.getClientAddress():"") + "] File '" + fileName + "' tranfer completed in " + (System.currentTimeMillis() - startTime) + " ms");
        } else {
            System.out.println("[SEND ERROR] File not found!");
        }
    }


    public static void receiveFile(SockClient sockClient, String fileName, String path, boolean showProgress) throws IOException {
        File file = new File(path+ fileName);
        long startTime = System.currentTimeMillis();
        file.createNewFile();
        long fileLen = sockClient.readLong();
        System.out.println("[RECEIVE " + ((Utils.SOURCE== Utils.Sources.SERVER)?sockClient.getClientAddress():"") + "] Downloading file " + fileName + ": size = " + fileLen);

        FileOutputStream fos = new FileOutputStream(path + fileName);

        long bytesRemainToRead = fileLen;
        byte[] readBuffer = new byte[Utils.BUFFER_SIZE];
        int packetNum = 0;
        while (bytesRemainToRead > 0) {
            packetNum++;
            int result = sockClient.read(readBuffer);
            bytesRemainToRead -= result;
            fos.write(readBuffer, 0, result);
            if (showProgress){
                Utils.showProgress(Utils.Actions.DOWNLOAD, fileLen, bytesRemainToRead, startTime);
            }
        }
        fos.close();
        double duration = (double) (System.currentTimeMillis() - startTime) / 1000.0d;
        double speed = ((double) fileLen / duration / 1024.0d);
        System.out.println("[RECEIVE " + ((Utils.SOURCE==Utils.Sources.SERVER)?sockClient.getClientAddress():"") + "] File downloaded in " + duration + " s, speed = " + String.format("%.2f", speed) + "kB/s");
    }
}
