import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerListener implements Runnable {
    private final BufferedReader input;
    private final Socket socket;
    private final AtomicBoolean connectionLost;
    private final java.util.concurrent.BlockingQueue<String> serverMessages;
    private final AtomicBoolean loginPhase;

    public ServerListener(BufferedReader input, Socket socket, AtomicBoolean connectionLost) {
        this(input, socket, connectionLost, null, new AtomicBoolean(true));
    }

    public ServerListener(BufferedReader input, Socket socket, AtomicBoolean connectionLost,
            java.util.concurrent.BlockingQueue<String> serverMessages) {
        this(input, socket, connectionLost, serverMessages, new AtomicBoolean(true));
    }

    public ServerListener(BufferedReader input, Socket socket, AtomicBoolean connectionLost,
            java.util.concurrent.BlockingQueue<String> serverMessages, AtomicBoolean loginPhase) {
        this.input = input;
        this.socket = socket;
        this.connectionLost = connectionLost;
        this.serverMessages = serverMessages;
        this.loginPhase = loginPhase;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = input.readLine();
                if (message == null) {
                    connectionLost.set(true);
                    closeSocket();
                    break;
                }
                if (serverMessages != null && loginPhase.get()) {
                    serverMessages.put(message);
                    continue;
                }
                System.out.println(formatForDisplay(message));
            }
        } catch (IOException e) {
            connectionLost.set(true);
            closeSocket();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static String formatForDisplay(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return "";
        }

        String[] parts = rawMessage.split("\\|", 5);
        if (parts.length < 5) {
            return rawMessage;
        }

        String type = parts[1].toUpperCase();
        String sender = parts[2];
        String payload = parts[4];

        switch (type) {
            case "TEXT":
                return sender + ": " + payload;
            case "PRIVATE":
                return "[Privat fra " + sender + "]: " + payload;
            case "ERROR":
                return "[Fejl]: " + payload;
            case "OK":
                if (payload == null || payload.isBlank()) {
                    return "[Info]: OK";
                }
                return "[Info]: " + payload;
            default:
                return payload;
        }
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
