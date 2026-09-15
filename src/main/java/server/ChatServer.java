
package server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Starter chatserveren og accepterer klientforbindelser i en fast trådpulje.
 * Serveren holder styr på registrerede brugere, chatrum og filoverførsel i den delte mappe.
 */
public class ChatServer {
    private static final int DEFAULT_PORT = 5000;
    private static final int THREAD_POOL_SIZE = 3;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    private static final Path SHARED_FILES_DIRECTORY = Paths.get("server_files");
    private static final ExecutorService clientPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final ClientRegistry clientRegistry = new ClientRegistry();
    private static final ChatRoomManager chatRoomManager = new ChatRoomManager();

    /**
     * Returnerer den delte filmappe, som serveren bruger til list/get-operationer.
     */
    public static Path getSharedFilesDirectory() {
        return SHARED_FILES_DIRECTORY;
    }

    /**
     * Opretter den delte filmappe, hvis den endnu ikke findes på disken.
     */
    public static void ensureSharedFilesDirectoryExists() throws IOException {
        if (Files.notExists(SHARED_FILES_DIRECTORY)) {
            // Mappen oprettes én gang ved start, så alle klienter deler samme filkatalog.
            Files.createDirectories(SHARED_FILES_DIRECTORY);
        }
    }

    /**
     * Starter serveren, opretter filrepository og accepterer klientforbindelser i en uendelig løkke.
     */
    public static void main(String[] args) {
        configureUtf8Console();

        try {
            ensureSharedFilesDirectoryExists();
        } catch (IOException e) {
            System.err.println("Kunne ikke oprette server_files-mappen: " + e.getMessage());
            return;
        }

        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Ugyldig port, bruger " + DEFAULT_PORT);
            }
        }

        System.out.println("Starter ChatServer på port " + port);
        // Create the shared file repository used for file transfer features
        ServerFileRepository fileRepository;
        try {
            fileRepository = new ServerFileRepository(getSharedFilesDirectory());
        } catch (IllegalArgumentException e) {
            System.err.println("Kunne ikke initialisere filrepository: " + e.getMessage());
            return;
        }

        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = server.accept();
                System.out.println("Forbindelse accepteret fra " + clientSocket.getRemoteSocketAddress());

                try {
                    clientPool.submit(new ClientHandler(clientSocket, clientRegistry, chatRoomManager, fileRepository));
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

    private static void configureUtf8Console() {
        try {
            System.setOut(new PrintStream(new BufferedOutputStream(System.out), true, StandardCharsets.UTF_8.name()));
            System.setErr(new PrintStream(new BufferedOutputStream(System.err), true, StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            System.err.println("Kunne ikke aktivere UTF-8 på konsollen: " + e.getMessage());
        }
    }

    private static void shutdownThreadPool() {
        clientPool.shutdown();

        try {
            boolean finished = clientPool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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