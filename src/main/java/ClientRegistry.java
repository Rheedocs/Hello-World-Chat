
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientRegistry {
    private final ConcurrentMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public boolean register(String username, ClientHandler clientHandler) {
        return clients.putIfAbsent(username, clientHandler) == null;
    }

    public void unregister(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        clients.remove(username);
    }

    public ClientHandler getClient(String username) {
        return clients.get(username);
    }

    public boolean isUsernameTaken(String username) {
        return username != null && clients.containsKey(username);
    }

    public List<ClientHandler> getConnectedClients() {
        return new ArrayList<>(clients.values());
    }

    public void broadcastExceptSender(String senderUsername, String target, String payload) {
        for (ClientHandler client : clients.values()) {
            if (!senderUsername.equals(client.getUsername())) {
                client.sendServerMessage("TEXT", senderUsername, target, payload);
            }
        }
    }
}
