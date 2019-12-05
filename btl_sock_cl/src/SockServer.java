import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class SockServer implements SockClient.OnCloseListener {
    public final static int BACKLOG = 50;
    public static final String FOLDER_PATH = "H:\\dl\\sv\\";


    private int serverPort;
    private ServerSocket serverSocket;
    private ArrayList<SockClient> clients;

    public SockServer() throws IOException {
        this.serverSocket = new ServerSocket();
        this.serverPort = serverSocket.getLocalPort();
        this.clients = new ArrayList<>();
    }
    public SockServer(int serverPort) throws IOException {
        this.serverPort = serverPort;
        this.serverSocket = new ServerSocket(serverPort, BACKLOG);
        this.clients = new ArrayList<>();
    }
    public SockServer(String ip, int serverPort) throws IOException {
        this.serverPort = serverPort;
        this.serverSocket = new ServerSocket(serverPort, BACKLOG, InetAddress.getByName(ip));
        this.clients = new ArrayList<>();
    }

    public SockClient accept() throws IOException {
        Socket socket = this.serverSocket.accept();
        SockClient newClient = new SockClient(socket, this);
        clients.add(newClient);
        return newClient;
    }

    public synchronized SockClient getClient(int id){
        return clients.get(id);
    }

    public synchronized ArrayList<SockClient> getClients() {
        return clients;
    }

    public String getServerIP() throws UnknownHostException {
        InetAddress address = InetAddress.getLocalHost();
        return address.toString();
    }

    public int getServerPort() {
        return serverPort;
    }

    @Override
    public synchronized void onClientClose(SockClient sockClient) {
        clients.remove(sockClient);
    }
}
