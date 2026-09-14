
package server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import protocol.MessageSender;

public class ClientRegistry {
    private final ConcurrentMap<String, MessageSender> clients = new ConcurrentHashMap<>();

    public boolean register(String username, MessageSender client) {
        return clients.putIfAbsent(username, client) == null;
    }

    public void unregister(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        clients.remove(username);
    }

    public MessageSender getClient(String username) {
        return clients.get(username);
    }

    public boolean isUsernameTaken(String username) {
        return username != null && clients.containsKey(username);
    }

    public List<MessageSender> getConnectedClients() {
        return new ArrayList<>(clients.values());
    }

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
                client.sendServerMessage("TEXT", senderUsername, target, payload);
            }
        }
    }
}
