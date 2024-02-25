import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.io.*;

public class Server{

    private ServerSocket tcp_server;
    private ArrayList<Socket> list = new ArrayList();
    private Thread udp_thread;
    private Thread tcp_thread;
    private String studio_address;
    private int studio_port;
    private int[][] data = new int[80][80];
    public boolean is_server = false;
    public Server(String studio_address, int studio_port) {
        this.studio_address = studio_address;
        this.studio_port = studio_port;
    }

    public void setData(int[][] data) {
        this.data = data;
    }
    public void start_server(){
        tcp_thread = new Thread(() -> {
            server();
        });
        tcp_thread.start();
    }
    private void server() {
        try {
            tcp_server = new ServerSocket(this.studio_port);

            while (true) {
                is_server = true;
                System.out.println("Listening TCP connections...");
                Socket clientSocket = tcp_server.accept();
                Thread t = new Thread(() -> {
                    synchronized (list) {
                        list.add(clientSocket);
                    }
                    try {
                        serve(clientSocket);
                    } catch (IOException e) {

                    }
                    synchronized (list) {
                        list.remove(clientSocket);
                    }
                });
                t.start();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void listen_clients(){
        udp_thread = new Thread(this::startUDPServer);
        udp_thread.start();
    }
    public void stop_server(){
        try{
            if(tcp_server!=null) {
                tcp_server.close();
            }
            if(udp_thread!=null) {
                udp_thread.interrupt();
            }
            if(tcp_thread!=null) {
                tcp_thread.interrupt();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    private void startUDPServer() {
        try (DatagramSocket udp_server = new DatagramSocket(5555)) {
            while (true) {
                DatagramPacket receivedPacket = new DatagramPacket(new byte[1024], 1024);
                udp_server.receive(receivedPacket);
                String content = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                if ("get_studio".equals(content)) {
                    String reply = this.studio_address + ":" + studio_port;
                    System.out.println(reply);
                    DatagramPacket packet = new DatagramPacket(reply.getBytes(), reply.length(), receivedPacket.getAddress(), receivedPacket.getPort());
                    udp_server.send(packet);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleChatMessage(DataInputStream in) throws IOException {
        int usernameLength = in.readInt();
        byte[] usernameBuffer = new byte[usernameLength];
        in.read(usernameBuffer, 0, usernameLength);

        // Read and print address
        int addressLength = in.readInt();
        byte[] addressBuffer = new byte[addressLength];
        in.read(addressBuffer, 0, addressLength);

        // Read and print the chat message
        int messageLength = in.readInt();
        byte[] messageBuffer = new byte[messageLength];
        in.read(messageBuffer, 0, messageLength);

        synchronized(list) {

            for (int i=0; i < list.size();i++) {
                try {
                    Socket s = list.get(i);
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());

                    // Send a message indicator
                    out.writeInt(0);

                    // Send the username
                    out.writeInt(usernameLength);
                    out.write(usernameBuffer);

                    // Send the address
                    out.writeInt(addressLength);
                    out.write(addressBuffer);

                    // send the actual chat message
                    out.writeInt(messageLength);
                    out.write(messageBuffer);
                    out.flush();
                }catch(IOException e){
                    System.out.println("The client is disconnected already!");
                }

            }
        }
    }
    private void handlePixelMessage(DataInputStream in) throws IOException{
        int color = in.readInt();
        int x = in.readInt();
        int y = in.readInt();
        data[x][y]=color;
        System.out.printf("%d - (%d, %d)\n", color, x, y);
        synchronized (list){
            for (Socket s : list) {
                try {
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                    out.writeInt(1);
                    out.writeInt(color);
                    out.writeInt(x);
                    out.writeInt(y);
                    out.flush();
                }catch (IOException ex){
                    //if we go here, it means the client is disconnected
                    ex.printStackTrace();
                }
            }
        }
    }

    private void sendInitialState(Socket clientSocket) throws IOException {
        // Send the initial state of the canvas to the new client
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        out.writeInt(2); // Set the message type to 2 for initial state

        // Iterate over the entire canvas data and send each pixel's color information
        for (int x = 0; x < data.length; x++) {
            for (int y = 0; y < data[0].length; y++) {
                out.writeInt(data[x][y]);
            }
        }
        out.flush();
    }
    private void serve(Socket clientSocket) throws IOException{
        DataInputStream in = new DataInputStream(clientSocket.getInputStream());
        while(true) {
            int type = in.readInt();
            switch (type){
                case 0: //chat message
                    handleChatMessage(in);
                    break;
                case 1:
                    handlePixelMessage(in);
                    break;
                case 2:
                    sendInitialState(clientSocket);
                    break;
                case 3:
                    paintArea(in);
                    break;
                default:
                    //TODO: something else???
            }
        }
    }

    private void paintArea(DataInputStream in) throws IOException{
        int color = in.readInt();
        int x = in.readInt();
        int y = in.readInt();
        data[x][y]=color;
        System.out.printf("%d - (%d, %d)\n", color, x, y);
        synchronized (list){
            for (Socket s : list) {
                try {
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());
                    out.writeInt(1);
                    out.writeInt(color);
                    out.writeInt(x);
                    out.writeInt(y);
                    out.flush();
                }catch (IOException ex){
                    //if we go here, it means the client is disconnected
                    ex.printStackTrace();
                }
            }
        }
    }
}
