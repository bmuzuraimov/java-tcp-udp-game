import javax.swing.*;
import java.net.*;

public class UDPClient {
    private DatagramSocket broadcast_socket;
    private Thread udp_thread;
    private boolean is_socket = false;

    public UDPClient() throws SocketException {
        this.broadcast_socket = new DatagramSocket();
    }

    public void start_socket() {
        try {
            is_socket = true;
            this.broadcast_socket = new DatagramSocket(5555);
        } catch (SocketException e) {
            System.err.println("Error starting the socket: " + e.getMessage());
            is_socket = false;
        }
    }
    public void stop_socket() {
        is_socket = false;
        if (this.broadcast_socket != null) {
            this.broadcast_socket.close();
        }
        if (udp_thread != null) {
            udp_thread.interrupt();
        }
    }

    public void broadcast() {
        if (!this.is_socket) {
            System.out.println("UDPClient is off!");
            return;
        }

        try {
            String msg = "get_studio";
            DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), InetAddress.getByName("255.255.255.255"), 5555);
            this.broadcast_socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listen_studio(JComboBox<String> studioDropdown) {
        udp_thread = new Thread(() -> udp_listen(studioDropdown));
        udp_thread.start();
    }

    private void udp_listen(JComboBox<String> studioDropdown) {
        try {
            DatagramPacket receivedPacket = new DatagramPacket(new byte[1024], 1024);
            while (this.is_socket) {
                this.broadcast_socket.receive(receivedPacket);
                String content = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                if (!content.equals("get_studio") && studioDropdown != null) {
                    SwingUtilities.invokeLater(() -> addUniqueItem(studioDropdown, content));
                }
            }
        } catch (Exception e) {
            if (is_socket) { // Only print stack trace if the socket was supposed to be open
                e.printStackTrace();
            }
        }
    }

    private void addUniqueItem(JComboBox<String> studioDropdown, String content) {
        if (DefaultComboBoxModel.class.isInstance(studioDropdown.getModel())) {
            DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) studioDropdown.getModel();
            if (model.getIndexOf(content) == -1) {
                model.addElement(content);
            }
        }
    }
}
