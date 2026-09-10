
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private static final int THREAD_POOL_SIZE = 3;
    private static final ExecutorService clientPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final ClientRegistry clientRegistry = new ClientRegistry();
    private static final ChatRoomManager chatRoomManager = new ChatRoomManager();

    public static void main(String[] args) {
        int port = 5000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Ugyldig port, bruger 5000");
            }
        }

        System.out.println("Starter ChatServer på port " + port);
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = server.accept();
                System.out.println("Forbindelse accepteret fra " + clientSocket.getRemoteSocketAddress());

                try {
                    clientPool.submit(new ClientHandler(clientSocket, clientRegistry, chatRoomManager));
                } catch (IOException e) {
                    System.out.println("Kunne ikke starte ClientHandler: " + e.getMessage());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Serverfejl: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdownThreadPool();
        }
    }

    private static void shutdownThreadPool() {
        clientPool.shutdown();

        try {
            boolean finished = clientPool.awaitTermination(5, TimeUnit.SECONDS);
            if (!finished) {
                System.out.println("Tråde kører stadig. Tvinger lukning...");
                clientPool.shutdownNow();
            }
        } catch (InterruptedException exception) {
            clientPool.shutdown();
            Thread.currentThread().interrupt();
        }
    }
}