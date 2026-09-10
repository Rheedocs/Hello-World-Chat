import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ClientRegistry clientRegistry;
    private final BufferedReader input;
    private final PrintWriter output;
    private String username;
    private boolean connected = true;

    public ClientHandler(Socket socket, ClientRegistry clientRegistry) throws IOException {
        this.socket = socket;
        this.clientRegistry = clientRegistry;
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String messageLine = input.readLine();
                if (messageLine == null) {
                    break;
                }

                handleClientMessage(messageLine);
            }
        } catch (IOException e) {
            System.out.println("Fejl for klient " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void sendServerMessage(String type, String sender, String target, String payload) {
        output.println(MessageParser.formatServerMessage(type, sender, target, payload));
    }

    private void handleClientMessage(String rawMessage) {
        try {
            Message message = MessageParser.parseClientMessage(rawMessage);
            String type = message.getType();

            switch (type) {
                case "LOGIN":
                    handleLogin(message);
                    break;
                case "TEXT":
                    handleText(message);
                    break;
                case "QUIT":
                    disconnect();
                    break;
                default:
                    sendServerMessage("ERROR", "server", username == null ? "" : username, "Ukendt kommando: " + type);
                    break;
            }
        } catch (IllegalArgumentException e) {
            String target = username == null ? "" : username;
            sendServerMessage("ERROR", "server", target, e.getMessage());
        }
    }

    private void handleLogin(Message message) {
        String requestedUsername = message.getPayload();
        if (requestedUsername == null || requestedUsername.isBlank()) {
            sendServerMessage("ERROR", "server", "", "Brugernavnet kan ikke være tomt.");
            return;
        }

        if (!clientRegistry.register(requestedUsername, this)) {
            sendServerMessage("ERROR", "server", requestedUsername, "Brugernavnet er optaget.");
            return;
        }

        username = requestedUsername;
        sendServerMessage("OK", "server", username, "");
        System.out.println("Brugeren " + username + " loggede ind.");
    }

    private void handleText(Message message) {
        if (username == null) {
            sendServerMessage("ERROR", "server", "", "Du skal logge ind først.");
            return;
        }

        String target = message.getTarget();
        String payload = message.getPayload();
        if (payload == null) {
            payload = "";
        }

        if (target == null || target.isBlank() || "all".equalsIgnoreCase(target)) {
            clientRegistry.broadcastExceptSender(username, "all", payload);
        } else {
            sendServerMessage("ERROR", "server", username, "Private beskeder og chatrum er ikke implementeret endnu.");
        }
    }

    private void disconnect() {
        if (!connected) {
            return;
        }
        connected = false;

        if (username != null) {
            clientRegistry.unregister(username);
            System.out.println("Brugeren " + username + " forlod chatten.");
        }

        try {
            socket.close();
        } catch (IOException ignored) {
            // Ignorerer lukning af allerede lukkede sockets.
        }
    }
}
