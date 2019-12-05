import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class SockClient {

    private String serverIP, clientIP;
    private Socket clientSocket;
    private OutputStream os;
    private InputStream is;
    private DataInputStream in;
    private DataOutputStream out;
    private OnCloseListener callback;
    private AtomicBoolean isBusy;

    public interface OnCloseListener {
        void onClientClose(SockClient sockClient);
    }

    public SockClient(Socket socket) throws IOException {
        this.clientSocket = socket;
        this.os = clientSocket.getOutputStream();
        this.out = new DataOutputStream(new BufferedOutputStream(this.os));
        this.is = clientSocket.getInputStream();
        this.in = new DataInputStream(new BufferedInputStream(this.is));
        this.isBusy = new AtomicBoolean(false);
        this.clientIP = this.clientSocket.getInetAddress().getHostAddress();
    }

    public SockClient(Socket socket, OnCloseListener callback) throws IOException {
        this(socket);
        this.callback = callback;
    }

    public SockClient(String serverIP, int port) throws IOException {
        this(new Socket(serverIP, port));
        this.serverIP = serverIP;
    }
    public SockClient(String serverIP, int port, OnCloseListener callback) throws IOException {
        this(serverIP, port);
        this.callback = callback;
    }
    public void acquire(){
        while(!this.isBusy.compareAndSet(false, true)){}
    }
    public void releaseLock(){
        this.isBusy.set(false);
    }
    public SockClient write(byte[] bytes, int offset, int len) throws IOException {
        this.out.write(bytes, offset, len);
        return this;
    }
    public SockClient write(boolean b) throws IOException {
        this.out.writeBoolean(b);
        return this;
    }
    public SockClient write(int num) throws IOException {
        this.out.write(num);
        return this;
    }
    public SockClient write(long num) throws IOException {
        this.out.writeLong(num);
        return this;
    }
    public SockClient write(String s) throws IOException {
        this.out.writeUTF(s);
        return this;
    }
    public SockClient writeBytes(String s) throws IOException {
        this.out.writeBytes(s);
        return this;
    }
    public SockClient writeBytes(byte[] bytes) throws IOException {
        this.writeBytes(new String(bytes));
        return this;
    }
    public void send() throws IOException {
        this.out.flush();
        this.releaseLock();
    }
    public int read(byte[] bytes) throws IOException {
        return this.in.read(bytes);
    }
    public int read(byte[] bytes, int offset, int len) throws IOException {
        return this.in.read(bytes, offset, len);
    }
    public String readString()throws IOException {
        return this.in.readUTF();
    }
    public int readInt()throws IOException {
        return this.in.readInt();
    }
    public long readLong()throws IOException {
        return this.in.readLong();
    }
    public boolean readBool() throws IOException {
        return this.in.readBoolean();
    }
    public void close() throws IOException {
        this.in.close();
        this.out.close();
        this.clientSocket.close();
        if (this.callback!=null){
            this.callback.onClientClose(this);
        }
    }
    public String getClientAddress(){
        return this.clientSocket.getRemoteSocketAddress().toString();
    }

    public String getIP(){
        return clientIP;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SockClient){
            return this.clientSocket.equals(((SockClient)o).getClientSocket());
        }
        return super.equals(o);
    }
}
