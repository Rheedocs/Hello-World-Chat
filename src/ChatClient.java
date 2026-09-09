import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ChatClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5000;
        if (args.length > 0) host = args[0];
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port, using 5000");
            }
        }

        System.out.println("Connecting to " + host + ":" + port);
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            System.out.println("Connected to server: " + socket.getRemoteSocketAddress());
            System.out.println("DataInputStream/DataOutputStream created. Keeping connection open for 10 seconds...");

            // No messages sent yet; just keep the connection open briefly to demonstrate connection establishment.
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Client exiting.");
    }
}
