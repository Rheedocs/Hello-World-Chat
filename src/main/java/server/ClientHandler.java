package server;

import domain.Message;
import protocol.MessageParser;
import protocol.MessageSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Håndterer én klientforbindelse fra login til chat, rum, private beskeder og filoperationer.
 * Klassen parser og distribuerer hver modtagen protokolbesked for den konkrete klient.
 */
public class ClientHandler implements Runnable, MessageSender {
    private static final String SERVER_USER = "server";
    private static final String TARGET_ALL = "all";

    private final Socket socket;
    private final ClientRegistry clientRegistry;
    private final ChatRoomManager chatRoomManager;
    private final ServerFileRepository fileRepository;
    private final BufferedReader input;
    private final PrintWriter output;
    private String username;
    private String currentRoom;
    private boolean connected = true;

    /**
     * Opretter en handler til en konkret socket og binder den til de delte serverregistre.
     */
    public ClientHandler(Socket socket, ClientRegistry clientRegistry, ChatRoomManager chatRoomManager, ServerFileRepository fileRepository) throws IOException {
        this.socket = socket;
        this.clientRegistry = clientRegistry;
        this.chatRoomManager = chatRoomManager;
        this.fileRepository = fileRepository;
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /**
     * Returnerer det aktuelle brugernavn for denne klient, hvis den allerede er logget ind.
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Læser klientens meddelelser i en løkke og håndterer hver enkelt protokolbesked.
     */
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

    /**
     * Sender en server-besked tilbage til den tilknyttede klient i standard protokolformat.
     */
    public void sendServerMessage(String type, String sender, String target, String payload) {
        output.println(MessageParser.formatServerMessage(type, sender, target, payload));
    }

    private void handleClientMessage(String rawMessage) {
        try {
            Message message = MessageParser.parseClientMessage(rawMessage);
            String type = message.getType().toUpperCase();

            switch (type) {
                case MessageParser.TYPE_LOGIN:
                    handleLogin(message);
                    break;
                case MessageParser.TYPE_JOIN_ROOM:
                    handleJoinRoom(message);
                    break;
                case MessageParser.TYPE_TEXT:
                    handleText(message);
                    break;
                case MessageParser.TYPE_PRIVATE:
                    handlePrivate(message);
                    break;
                case MessageParser.TYPE_QUIT:
                    sendServerMessage(MessageParser.TYPE_OK, SERVER_USER, currentTarget(), "Du er nu logget ud.");
                    disconnect();
                    break;
                case MessageParser.TYPE_LIST_FILES:
                    handleListFiles();
                    break;
                case MessageParser.TYPE_GET_FILE:
                    handleGetFile(message);
                    break;
                default:
                    sendServerMessage(MessageParser.TYPE_ERROR, SERVER_USER, currentTarget(), "Ukendt kommando: " + type);
                    break;
            }
        } catch (IllegalArgumentException e) {
            sendServerMessage(MessageParser.TYPE_ERROR, SERVER_USER, currentTarget(), e.getMessage());
        }
    }

    /**
     * Sender en fejl og returnerer false, hvis klienten ikke er logget ind endnu.
     */
    private boolean requireLogin() {
        if (username == null) {
            sendServerMessage(MessageParser.TYPE_ERROR, SERVER_USER, "", "Du skal logge ind først.");
            return false;
        }
        return true;
    }

    /**
     * Returnerer brugernavnet som modtager, eller en tom streng før login.
     */
    private String currentTarget() {
        return username == null ? "" : username;
    }

    private void handleLogin(Message message) {
        String requestedUsername = message.getPayload();
        if (requestedUsername == null || requestedUsername.isBlank()) {
            sendServerMessage(MessageParser.TYPE_ERROR, SERVER_USER, "", "Brugernavnet kan ikke være tomt.");
            return;
        }

        if (!clientRegistry.register(requestedUsername, this)) {
            sendServerMessage(MessageParser.TYPE_ERROR, SERVER_USER, requestedUsername, "Brugernavnet er optaget.");
            return;
        }

        username = requestedUsername;
        currentRoom = chatRoomManager.getDefaultRoom();
        chatRoomManager.addUserToRoom(username, currentRoom);
        sendServerMessage(MessageParser.TYPE_OK, SERVER_USER, username, "");
        System.out.println("Brugeren " + username + " loggede ind i rummet " + currentRoom + ".");
    }

    private void handleJoinRoom(Message message) {
        if (!requireLogin()) {
            return;
        }

        String target = message.getTarget();
        if (target == null || target.isBlank()) {
            sendServerMessage(MessageParser.TYPE_ERROR, SERVER_USER, username, "Rumnavnet kan ikke være tomt.");
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

        sendServerMessage(MessageParser.TYPE_OK, SERVER_USER, username, "Du er nu i rummet " + currentRoom);
    }

    private void handleText(Message message) {
        if (!requireLogin()) {
            return;
        }

        String roomName = message.getTarget();
        String payload = message.getPayload();
        if (payload == null) {
            payload = "";
        }

        if (roomName == null || roomName.isBlank() || TARGET_ALL.equalsIgnoreCase(roomName)) {
            roomName = currentRoom;
        }

        String normalizedRoom = chatRoomManager.getRoomNameForTarget(roomName);
        if (currentRoom == null) {
            currentRoom = normalizedRoom;
        }

        for (String memberName : chatRoomManager.getMembers(normalizedRoom)) {
            if (!username.equals(memberName)) {
                MessageSender member = clientRegistry.getClient(memberName);
                if (member != null) {
                    member.sendServerMessage(MessageParser.TYPE_TEXT, username, normalizedRoom, payload);
                }
            }
        }
    }

    private void handlePrivate(Message message) {
        if (!requireLogin()) {
            return;
        }

        String recipient = message.getTarget();
        String payload = message.getPayload();
        if (recipient == null || recipient.isBlank()) {
            sendServerMessage(MessageParser.TYPE_ERROR, SERVER_USER, username, "Modtager mangler.");
            return;
        }

        MessageSender target = clientRegistry.getClient(recipient);
        if (target == null) {
            sendServerMessage(MessageParser.TYPE_ERROR, SERVER_USER, username, "Brugeren " + recipient + " er ikke online.");
            return;
        }

        if (payload == null) payload = "";
        target.sendServerMessage(MessageParser.TYPE_PRIVATE, username, recipient, payload);
    }

    private void handleListFiles() {
        if (!requireLogin()) {
            return;
        }

        try {
            List<String> files = fileRepository.listFiles();
            String payload = String.join(",", files);
            sendServerMessage(MessageParser.TYPE_FILE_LIST, SERVER_USER, username, payload);
        } catch (IOException e) {
            sendServerMessage(MessageParser.TYPE_FILE_ERROR, SERVER_USER, username, "Kunne ikke liste filer: " + e.getMessage());
        }
    }

    private void handleGetFile(Message message) {
        if (!requireLogin()) {
            return;
        }

        String fileName = message.getTarget();
        if (!validateRequestedFile(fileName)) {
            return;
        }

        try {
            byte[] data = fileRepository.readFile(fileName);
            sendFileDataResponse(fileName, data);
        } catch (SecurityException se) {
            sendServerMessage(MessageParser.TYPE_FILE_ERROR, SERVER_USER, username, "Adgang nægtet: " + se.getMessage());
        } catch (IOException ioe) {
            sendServerMessage(MessageParser.TYPE_FILE_ERROR, SERVER_USER, username, "Kunne ikke læse filen: " + ioe.getMessage());
        }
    }

    private boolean validateRequestedFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            sendServerMessage(MessageParser.TYPE_FILE_ERROR, SERVER_USER, username, "Filnavn mangler.");
            return false;
        }

        // Afviser filnavne med '|', da de ellers ville ødelægge FILEDATA payloaden (filnavn|base64).
        if (fileName.contains("|")) {
            // Sender FILEERROR med tomt target efter det aftalte mønster: FILEERROR||Ugyldigt filnavn
            sendServerMessage(MessageParser.TYPE_FILE_ERROR, SERVER_USER, "", "Ugyldigt filnavn");
            return false;
        }

        return true;
    }

    private String buildFilePayload(String fileName, byte[] data) {
        String base64 = Base64.getEncoder().encodeToString(data);
        // Payload består af filnavn og base64 indhold adskilt af én pipe, så klienten kan dele dem op igen.
        return fileName + "|" + base64;
    }

    private void sendFileDataResponse(String fileName, byte[] data) {
        String payload = buildFilePayload(fileName, data);
        sendServerMessage(MessageParser.TYPE_FILE_DATA, SERVER_USER, username, payload);
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