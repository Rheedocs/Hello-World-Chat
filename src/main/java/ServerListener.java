import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerListener implements Runnable {
    private final BufferedReader input;
    private final Socket socket;
    private final AtomicBoolean connectionLost;

    public ServerListener(BufferedReader input, Socket socket, AtomicBoolean connectionLost) {
        this.input = input;
        this.socket = socket;
        this.connectionLost = connectionLost;
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
                System.out.println("[SERVER] " + message);
            }
        } catch (IOException e) {
            connectionLost.set(true);
            closeSocket();
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
