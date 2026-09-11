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
                }
                System.out.println("[SERVER] " + message);
            }
        } catch (IOException e) {
            connectionLost.set(true);
            closeSocket();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
