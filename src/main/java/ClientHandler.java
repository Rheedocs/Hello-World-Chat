
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
    private final ChatRoomManager chatRoomManager;
    private final BufferedReader input;
    private final PrintWriter output;
    private String username;
    private String currentRoom;
    private boolean connected = true;

    public ClientHandler(Socket socket, ClientRegistry clientRegistry, ChatRoomManager chatRoomManager) throws IOException {
        this.socket = socket;
        this.clientRegistry = clientRegistry;
        this.chatRoomManager = chatRoomManager;
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
                case "JOIN_ROOM":
                    handleJoinRoom(message);
                    break;
                case "TEXT":
                    handleText(message);
                    break;
                case "PRIVATE":
                    handlePrivate(message);
                    break;
                case "QUIT":
                    // Send logout confirmation before closing the connection
                    sendServerMessage("OK", "server", username == null ? "" : username, "Du er nu logget ud.");
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
        currentRoom = chatRoomManager.getDefaultRoom();
        chatRoomManager.addUserToRoom(username, currentRoom);
        sendServerMessage("OK", "server", username, "");
        System.out.println("Brugeren " + username + " loggede ind i rummet " + currentRoom + ".");
    }

    private void handleJoinRoom(Message message) {
        if (username == null) {
            sendServerMessage("ERROR", "server", "", "Du skal logge ind først.");
            return;
        }

        String target = message.getTarget();
        if (target == null || target.isBlank()) {
            sendServerMessage("ERROR", "server", username, "Rumnavnet kan ikke være tomt.");
            return;
        }

        String previousRoom = currentRoom;
        chatRoomManager.moveUserToRoom(username, target);
        currentRoom = chatRoomManager.getUserRoom(username);

        if (currentRoom == null) {
            currentRoom = target;
        }

        if (previousRoom != null && !previousRoom.equals(currentRoom)) {
            System.out.println("Brugeren " + username + " flyttede fra " + previousRoom + " til " + currentRoom + ".");
        }

        sendServerMessage("OK", "server", username, "Du er nu i rummet " + currentRoom);
    }

    private void handleText(Message message) {
        if (username == null) {
            sendServerMessage("ERROR", "server", "", "Du skal logge ind først.");
            return;
        }

        String roomName = message.getTarget();
        String payload = message.getPayload();
        if (payload == null) {
            payload = "";
        }

        if (roomName == null || roomName.isBlank() || "all".equalsIgnoreCase(roomName)) {
            roomName = currentRoom;
        }

        String normalizedRoom = chatRoomManager.getRoomNameForTarget(roomName);
        if (currentRoom == null) {
            currentRoom = normalizedRoom;
        }

        for (String memberName : chatRoomManager.getMembers(normalizedRoom)) {
            if (!username.equals(memberName)) {
                ClientHandler member = clientRegistry.getClient(memberName);
                if (member != null) {
                    member.sendServerMessage("TEXT", username, normalizedRoom, payload);
                }
            }
        }
    }

    private void handlePrivate(Message message) {
        if (username == null) {
            sendServerMessage("ERROR", "server", "", "Du skal logge ind først.");
            return;
        }

        String recipient = message.getTarget();
        String payload = message.getPayload();
        if (recipient == null || recipient.isBlank()) {
            sendServerMessage("ERROR", "server", username, "Modtager mangler.");
            return;
        }

        ClientHandler target = clientRegistry.getClient(recipient);
        if (target == null) {
            sendServerMessage("ERROR", "server", username, "Brugeren " + recipient + " er ikke online.");
            return;
        }

        if (payload == null) payload = "";
        target.sendServerMessage("PRIVATE", username, recipient, payload);
    }

    private void disconnect() {
        if (!connected) {
            return;
        }
        connected = false;

        if (username != null) {
            chatRoomManager.removeUser(username);
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
