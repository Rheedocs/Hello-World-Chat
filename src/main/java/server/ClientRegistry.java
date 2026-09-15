package server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import protocol.MessageParser;
import protocol.MessageSender;

/**
 * Holder trådsikkert styr på de brugere, der aktuelt er logget ind på serveren.
 * Klassen bruges til at finde klienterne igen ved brugernavn og sende meddelelser til andre online brugere.
 */
public class ClientRegistry {
    private final ConcurrentMap<String, MessageSender> clients = new ConcurrentHashMap<>();

    /**
     * Registrerer en klient med et brugernavn, hvis navnet endnu ikke er optaget.
     */
    public boolean register(String username, MessageSender client) {
        // putIfAbsent er atomisk og undgår en race, hvor to tråde begge finder et ledigt brugernavn.
        return clients.putIfAbsent(username, client) == null;
    }

    /**
     * Fjerner en klient fra registret, når den forlader chatten eller forbindelsen afbrydes.
     */
    public void unregister(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        clients.remove(username);
    }

    /**
     * Returnerer klienten for et bestemt brugernavn, eller null, hvis brugeren ikke er logget ind.
     */
    public MessageSender getClient(String username) {
        return clients.get(username);
    }

    /**
     * Kontrollerer, om et brugernavn allerede er registreret i den delte brugerliste.
     */
    public boolean isUsernameTaken(String username) {
        return username != null && clients.containsKey(username);
    }

    /**
     * Returnerer en kopi af alle aktiverede klienter i den aktuelle registrering.
     */
    public List<MessageSender> getConnectedClients() {
        return new ArrayList<>(clients.values());
    }

    /**
     * Sender en tekst til alle andre online brugere uden at gentage afsenderen.
     */
    public void broadcastExceptSender(String senderUsername, String target, String payload) {
        if (senderUsername == null) {
            return;
        }

        for (MessageSender client : clients.values()) {
            if (client == null) {
                continue;
            }
            String clientUsername = client.getUsername();
            if (clientUsername != null && !senderUsername.equals(clientUsername)) {
                client.sendServerMessage(MessageParser.TYPE_TEXT, senderUsername, target, payload);
            }
        }
    }
}