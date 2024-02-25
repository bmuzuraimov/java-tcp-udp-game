import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPClient{
    private UIUpdateListener uiUpdateListener;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread tcp_thread;
    private String studio_address;
    private int studio_port;
    private String my_address;
    public TCPClient(UIUpdateListener listener) throws UnknownHostException {
        this.uiUpdateListener = listener;
        this.my_address = InetAddress.getLocalHost().getHostAddress();
    }
    public DataInputStream getIn(){
        return this.in;
    }
    public DataOutputStream getOut(){
        return this.out;
    }
    public String getMy_address(){
        return this.my_address;
    }
    public void setStudio_address(String studio_address){
        this.studio_address = studio_address;
    }
    public void setStudio_port(int studio_port){
        this.studio_port = studio_port;
    }
    private void receive(int[][] data) {
        try {
            while(this.in!=null) {
                if(this.in.available() > 0) {
                    int type = this.in.readInt();
                    switch (type){
                        case 0:
                            receiveChatMessage();
                            break;
                        case 1:
                            receivePixelMessage();
                            break;
                        case 2:
                            receiveInitialStateMessage(data);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading from server: " + e.getMessage());
        } finally {
            closeConnections();
        }
    }

    public void closeConnections() {
        try {
            if (this.tcp_thread != null && this.tcp_thread.isAlive()) {
                this.tcp_thread.interrupt();
            }
            if (this.out != null) {
                this.out.close();
            }
            if (this.in != null) {
                this.in.close();
            }
            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing connections: " + e.getMessage());
        }
    }

    public void connect_studio(int[][] data) {
        try {
            // Close previous connections if they exist
            closeConnections();
            // Create new socket connection
            System.out.println("Connecting to server: " + this.studio_address + ":" + this.studio_port);
            this.socket = new Socket(this.studio_address, this.studio_port);

            // Initialize input and output streams
            this.in = new DataInputStream(this.socket.getInputStream());
            this.out = new DataOutputStream(this.socket.getOutputStream());
            System.out.println("Connected to server, I/O streams initialized");

            // Start a new thread for receiving data
            this.tcp_thread = new Thread(() -> {
                receive(data);
            });
            this.tcp_thread.start();
        } catch (Exception e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }

    private void receivePixelMessage() throws IOException{
        int color = this.in.readInt();
        int x = this.in.readInt();
        int y = this.in.readInt();
        uiUpdateListener.paintPixel(color, x, y);
    }
    private void receiveChatMessage() throws IOException{
        // Read and print username
        int usernameLength = this.in.readInt();
        byte[] usernameBuffer = new byte[usernameLength];
        this.in.readFully(usernameBuffer, 0, usernameLength);
        String username = new String(usernameBuffer, 0, usernameLength);

        // Read and print address
        int addressLength = this.in.readInt();
        byte[] addressBuffer = new byte[addressLength];
        this.in.readFully(addressBuffer, 0, addressLength);
        String address = new String(addressBuffer, 0, addressLength);

        // Read and print the chat message
        int messageLength = this.in.readInt();
        byte[] messageBuffer = new byte[messageLength];
        this.in.readFully(messageBuffer, 0, messageLength);
        String message = new String(messageBuffer, 0, messageLength);

        if(address.equals(this.my_address)){
            uiUpdateListener.appendChatMessage(message, true);
        }else{
            uiUpdateListener.appendChatMessage(username + ": " + message, false);
        }
    }

    private void receiveInitialStateMessage(int[][] data) throws IOException {
        for (int x = 0; x < data.length; x++) {
            for (int y = 0; y < data[0].length; y++) {
                data[x][y] = this.in.readInt();
            }
        }
        SwingUtilities.invokeLater(() -> {
            uiUpdateListener.repaintPaintPanel();
        });
    }
}
