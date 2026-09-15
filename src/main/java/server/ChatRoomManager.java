package server;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Holder trådsikkert styr på hvilke brugere, der er medlem af hvert chatrum.
 * Klassen flytter brugere mellem rum og finder deres aktuelle medlemskab uden brug af almindelige ikke-synkroniserede samlinger.
 */
public class ChatRoomManager {
    public static final String DEFAULT_ROOM = "lobby";
    private static final String ALL_TARGET = "all";

    private final ConcurrentMap<String, Set<String>> roomMembers = new ConcurrentHashMap<>();

    /**
     * Opretter standardrummet, så en ny bruger altid kan blive placeret i "lobby".
     */
    public ChatRoomManager() {
        roomMembers.putIfAbsent(DEFAULT_ROOM, ConcurrentHashMap.newKeySet());
    }

    /**
     * Returnerer navnet på det standardrum, der bruges, når en bruger logger ind.
     */
    public String getDefaultRoom() {
        return DEFAULT_ROOM;
    }

    /**
     * Tilføjer en bruger til et rum og opretter rummet, hvis det ikke allerede findes.
     */
    public void addUserToRoom(String username, String roomName) {
        if (username == null || username.isBlank()) {
            return;
        }

        String normalizedRoom = normalizeRoom(roomName);
        // computeIfAbsent gør oprettelsen af rummet trådsikker, så flere klienter kan starte samme rum samtidig.
        roomMembers.computeIfAbsent(normalizedRoom, key -> ConcurrentHashMap.newKeySet()).add(username);
    }

    /**
     * Flytter en bruger fra sit nuværende rum til et nyt rum og fjerner tomme rum efterfølgende.
     */
    public void moveUserToRoom(String username, String targetRoomName) {
        if (username == null || username.isBlank()) {
            return;
        }

        String targetRoom = normalizeRoom(targetRoomName);
        String currentRoom = findRoomForUser(username);

        if (currentRoom != null && !currentRoom.equals(targetRoom)) {
            removeUserFromRoom(username, currentRoom);
        }

        addUserToRoom(username, targetRoom);
    }

    /**
     * Fjerner en bruger fra det rum, hun aktuelt er medlem af, og rydder op i tomme rum.
     */
    public void removeUser(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String room = findRoomForUser(username);
        if (room == null) {
            return;
        }

        removeUserFromRoom(username, room);
    }

    /**
     * Returnerer de brugere, der er medlem af det angivne rum, eller en tom samling, hvis rummet ikke findes.
     */
    public Set<String> getMembers(String roomName) {
        return roomMembers.getOrDefault(normalizeRoom(roomName), Collections.emptySet());
    }

    /**
     * Returnerer det rum, som brugeren aktuelt er medlem af, eller null, hvis hun ikke er i et rum.
     */
    public String getUserRoom(String username) {
        return findRoomForUser(username);
    }

    /**
     * Normaliserer et mål i en chatbesked til et reelt rumnavn, så "all" altid bruges som lobby.
     */
    public String getRoomNameForTarget(String target) {
        if (target == null || target.isBlank() || ALL_TARGET.equalsIgnoreCase(target)) {
            return DEFAULT_ROOM;
        }
        return normalizeRoom(target);
    }

    private void removeUserFromRoom(String username, String roomName) {
        Set<String> members = roomMembers.get(roomName);
        if (members != null) {
            members.remove(username);
            // remove(key, value) sletter kun rummet, hvis det stadig er det samme tomme sæt.
            if (members.isEmpty()) {
                roomMembers.remove(roomName, members);
            }
        }
    }

    private String normalizeRoom(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            return DEFAULT_ROOM;
        }
        return roomName.trim();
    }

    private String findRoomForUser(String username) {
        for (String roomName : roomMembers.keySet()) {
            Set<String> members = roomMembers.get(roomName);
            if (members != null && members.contains(username)) {
                return roomName;
            }
        }
        return null;
    }
}