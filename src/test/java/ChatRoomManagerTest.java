
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class ChatRoomManagerTest {

    @Test
    public void defaultRoomShouldBeCreatedAndUsersShouldBeTracked() {
        ChatRoomManager manager = new ChatRoomManager();

        manager.addUserToRoom("alice", ChatRoomManager.DEFAULT_ROOM);

        assertEquals(ChatRoomManager.DEFAULT_ROOM, manager.getUserRoom("alice"));
        assertTrue(manager.getMembers(ChatRoomManager.DEFAULT_ROOM).contains("alice"));
    }

    @Test
    public void moveUserToRoomShouldRemoveFromOldRoom() {
        ChatRoomManager manager = new ChatRoomManager();
        manager.addUserToRoom("bob", ChatRoomManager.DEFAULT_ROOM);

        manager.moveUserToRoom("bob", "dev");

        Set<String> defaultMembers = manager.getMembers(ChatRoomManager.DEFAULT_ROOM);
        Set<String> devMembers = manager.getMembers("dev");
        assertFalse(defaultMembers.contains("bob"));
        assertTrue(devMembers.contains("bob"));
        assertEquals("dev", manager.getUserRoom("bob"));
    }

    @Test
    public void getMembersForUnknownRoomShouldReturnEmptySet() {
        ChatRoomManager manager = new ChatRoomManager();

        Set<String> members = manager.getMembers("unknown-room");

        assertNotNull(members);
        assertTrue(members.isEmpty());
    }

    @Test
    public void removeUserFromUnknownRoomShouldNotThrow() {
        ChatRoomManager manager = new ChatRoomManager();

        manager.removeUser("ghost");

        assertNull(manager.getUserRoom("ghost"));
    }
}
