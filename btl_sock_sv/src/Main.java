import java.io.*;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        SockServer sockServer = null;

//            System.out.print("Enter server ip: ");
        String ip = "192.168.98.2"; //scanner.nextLine();
//            String ip = "127.0.0.1";'
        try {
            sockServer = new SockServer(ip, 1259);
            System.out.println("[SERVER] Listening " + sockServer.getServerIP() + ":" + sockServer.getServerPort());
        } catch (UnknownHostException e) {
            System.out.println("[ERROR] Server bind error, check server ip");
        } catch (IOException e) {
            System.out.println("[ERROR] Server bind error, check server ip and port!");
            return;
        }
        new Thread(new SockServerTask.AcceptClientThread(sockServer)).start();
        while (true) {
            System.out.print(">");
            String command = scanner.nextLine();
            if (command.contains("ls")) {
                Utils.selfLs();
            } else if (command.contains("post")) {
                try {
                    SockServerTask.broastcastFile(sockServer, Utils.getDataFromCommand(Utils.Actions.POST, command));
                } catch (IOException e) {
                    System.out.println("[BROADCAST ERROR] File broadcast IO Error!");
                    continue;
                } catch (InterruptedException e) {
                    System.out.println("[BROADCAST ERROR] File broadcast interupted!");
                    continue;
                }
            } else {
                System.out.println("Invalid command!");
            }
        }
    }
}
