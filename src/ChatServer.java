import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    public static void main(String[] args) {
        int port = 5000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port, using 5000");
            }
        }

        System.out.println("Starting ChatServer on port " + port);
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket client = server.accept();
                System.out.println("Accepted connection from " + client.getRemoteSocketAddress());

                // Start a handler thread that creates DataInputStream/DataOutputStream and keeps the connection open.
                Thread handler = new Thread(() -> {
                    try (DataInputStream in = new DataInputStream(client.getInputStream());
                         DataOutputStream out = new DataOutputStream(client.getOutputStream())) {
                        // No protocol yet: just keep the connection alive until client disconnects.
                        while (!client.isClosed()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Handler IO error: " + e.getMessage());
                    } finally {
                        try {
                            client.close();
                        } catch (IOException ignored) {
                        }
                        System.out.println("Closed connection to " + client.getRemoteSocketAddress());
                    }
                });
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
