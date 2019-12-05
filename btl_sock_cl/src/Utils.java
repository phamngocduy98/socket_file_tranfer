import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class Utils {
    public static final int BUFFER_SIZE = 256*1024;
    public static final Pattern getPattern = Pattern.compile("(?<=(get|GET|geT|gEt|gET|Get|GeT|GEt).).*");
    public static final Pattern postPattern = Pattern.compile("(?<=(post|POST).).*");
    public static final Pattern piecePattern = Pattern.compile("(?<=(piece|PIECE).).*");
    public static final Pattern friendPattern = Pattern.compile("(?<=(friend|FRIEND).).*");
    public static final Pattern broadcastPattern = Pattern.compile("(?<=(broadcast|BROADCAST).).*");
    public static long startTime = System.currentTimeMillis();
    enum Actions {
        GET, POST, PIECE, DOWNLOAD, UPLOAD, FRIEND, BROADCAST
    }
    enum Sources {
        SERVER, CLIENT
    }

    public static void setStartTime(){
        startTime = System.currentTimeMillis();
    }

    public static String getFolderPath(){
        return Multi.FOLDER_PATH;
    }

    public static String[] listFiles(String folderPath){
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        String[] ret = new String[listOfFiles.length];

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                ret[i] = listOfFiles[i].getName();
            } else if (listOfFiles[i].isDirectory()) {
                ret[i] = "~" + listOfFiles[i].getName();
            }
        }
        return ret;
    }

    public static void selfLs() {
        String[] fileNames = Utils.listFiles(getFolderPath());
        for (String fileName : fileNames) {
            System.out.println("```" + fileName);
        }
    }

    public static String getDataFromCommand(Actions type, String command) {
        Matcher matcher;
        switch (type) {
            case GET:
                matcher = getPattern.matcher(command);
                break;
            case POST:
                matcher = postPattern.matcher(command);
                break;
            case PIECE:
                matcher = piecePattern.matcher(command);
                break;
            case FRIEND:
                matcher = friendPattern.matcher(command);
                break;
            case BROADCAST:
                matcher = broadcastPattern.matcher(command);
                break;
            default:
                matcher = getPattern.matcher(command);
        }
        matcher.find();
        return matcher.group();
    }

    public static void showProgress(Actions action, long fileLen, long bytesRemainToRead, long startTime){
        double duration = (double) (System.currentTimeMillis() - startTime) / 1000.0d;
        double speed = ((double) (fileLen - bytesRemainToRead) / duration / 1024.0d);
        double eta = (bytesRemainToRead / 1024.0d / speed);
        double etaM = Math.floor(eta / 60.0d);
        double etaS = Math.floor(eta - etaM * 60);
        double percent = ((fileLen - bytesRemainToRead) * 100 / fileLen);
        String progressStr = action==Actions.UPLOAD?"Uploading: ":"Downloading: " + (fileLen - bytesRemainToRead) + "/" + fileLen + " . " + String.format("%.2f", percent) + "% . " + String.format("%.2f", speed) + "kbps . ETA = " + String.format("%.0f", etaM) + "m" + String.format("%.0f", etaS) + "s";
        System.out.print(progressStr);
        for (int i = 0; i < progressStr.length(); i++) System.out.print("\b");
    }

    public static long checksum(byte[] bytes){
        Checksum checksum = new Adler32();
        checksum.update(bytes, 0, bytes.length);
        return checksum.getValue();
    }
}
