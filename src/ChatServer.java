import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatServer {

    private static final int THREAD_POOL_SIZE = 3;

    private static final ExecutorService clientPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

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
                // Clientpool starter threadpoolen med et begrænsning på 3 tråde.
                clientPool.submit(()-> {
                    try (DataInputStream in = new DataInputStream(client.getInputStream());
                         DataOutputStream out = new DataOutputStream(client.getOutputStream())) {

                        System.out.println("START: " + Thread.currentThread().getName());;

                        Thread.sleep(10000);

                        System.out.println("END: " + Thread.currentThread().getName());

                    } catch (IOException e) {
                        System.out.println("Handler IO error: " + e.getMessage());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        try {
                            client.close();
                        } catch (IOException ignored) {
                        }
                        System.out.println("Closed connection to " + client.getRemoteSocketAddress());
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdownThreadPool();
        }
    }
    private static void shutdownThreadPool(){
        clientPool.shutdown();

        try {
            boolean finished = clientPool.awaitTermination(5,TimeUnit.SECONDS);

            if (!finished){
                System.out.println("Threads are still running. Forcing shutdown...");
                clientPool.shutdownNow();
            }
        } catch (InterruptedException exception){
            clientPool.shutdown();
            Thread.currentThread().interrupt();
        }
    }
}