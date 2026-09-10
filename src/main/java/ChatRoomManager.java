
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatRoomManager {
    public static final String DEFAULT_ROOM = "lobby";

    private final ConcurrentMap<String, Set<String>> roomMembers = new ConcurrentHashMap<>();

    public ChatRoomManager() {
        roomMembers.putIfAbsent(DEFAULT_ROOM, ConcurrentHashMap.newKeySet());
    }

    public String getDefaultRoom() {
        return DEFAULT_ROOM;
    }

    public void addUserToRoom(String username, String roomName) {
        if (username == null || username.isBlank()) {
            return;
        }

        String normalizedRoom = normalizeRoom(roomName);
        roomMembers.computeIfAbsent(normalizedRoom, key -> ConcurrentHashMap.newKeySet()).add(username);
    }

    public void moveUserToRoom(String username, String targetRoomName) {
        if (username == null || username.isBlank()) {
            return;
        }

        String targetRoom = normalizeRoom(targetRoomName);
        String currentRoom = findRoomForUser(username);

        if (currentRoom != null && !currentRoom.equals(targetRoom)) {
            Set<String> currentMembers = roomMembers.get(currentRoom);
            if (currentMembers != null) {
                currentMembers.remove(username);
                if (currentMembers.isEmpty()) {
                    roomMembers.remove(currentRoom, currentMembers);
                }
            }
        }

        addUserToRoom(username, targetRoom);
    }

    public void removeUser(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        String room = findRoomForUser(username);
        if (room == null) {
            return;
        }

        Set<String> members = roomMembers.get(room);
        if (members != null) {
            members.remove(username);
            if (members.isEmpty()) {
                roomMembers.remove(room, members);
            }
        }
    }

    public Set<String> getMembers(String roomName) {
        return roomMembers.getOrDefault(normalizeRoom(roomName), Collections.emptySet());
    }

    public String getUserRoom(String username) {
        return findRoomForUser(username);
    }

    public String getRoomNameForTarget(String target) {
        if (target == null || target.isBlank() || "all".equalsIgnoreCase(target)) {
            return DEFAULT_ROOM;
        }
        return normalizeRoom(target);
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
