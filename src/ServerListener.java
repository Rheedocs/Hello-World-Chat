import java.io.BufferedReader;
import java.io.IOException;

public class ServerListener implements Runnable {
    private final BufferedReader input;

    public ServerListener(BufferedReader input) {
        this.input = input;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = input.readLine();
                if (message == null) {
                    break;
                }
                System.out.println("[SERVER] " + message);
            }
        } catch (IOException e) {
            System.out.println("Server listener lukket: " + e.getMessage());
        }
    }
}
