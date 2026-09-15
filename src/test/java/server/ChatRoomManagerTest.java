package server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ChatRoomManagerTest {

    @Test
    public void getMembers_newManager_defaultRoomExistsAndIsEmpty() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();

        // Act & Assert
        assertTrue(manager.getMembers(ChatRoomManager.DEFAULT_ROOM).isEmpty());
    }

    @Test
    public void addUserToRoom_validUser_userIsMemberOfRoom() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();

        // Act
        manager.addUserToRoom("alice", ChatRoomManager.DEFAULT_ROOM);

        // Assert
        assertEquals(ChatRoomManager.DEFAULT_ROOM, manager.getUserRoom("alice"));
        assertTrue(manager.getMembers(ChatRoomManager.DEFAULT_ROOM).contains("alice"));
    }

    @Test
    public void addUserToRoom_blankUsername_isIgnored() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();

        // Act
        manager.addUserToRoom("  ", ChatRoomManager.DEFAULT_ROOM);

        // Assert
        assertTrue(manager.getMembers(ChatRoomManager.DEFAULT_ROOM).isEmpty());
    }

    @Test
    public void addUserToRoom_roomNameWithSpaces_trimsRoomName() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();

        // Act
        manager.addUserToRoom("alice", "  dev  ");

        // Assert
        assertEquals("dev", manager.getUserRoom("alice"));
    }

    @Test
    public void moveUserToRoom_newRoom_removesUserFromOldRoom() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();
        manager.addUserToRoom("bob", ChatRoomManager.DEFAULT_ROOM);

        // Act
        manager.moveUserToRoom("bob", "dev");

        // Assert
        assertFalse(manager.getMembers(ChatRoomManager.DEFAULT_ROOM).contains("bob"));
        assertTrue(manager.getMembers("dev").contains("bob"));
        assertEquals("dev", manager.getUserRoom("bob"));
    }

    @Test
    public void moveUserToRoom_lastUserLeavesRoom_roomIsRemoved() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();
        manager.addUserToRoom("bob", "dev");

        // Act
        manager.moveUserToRoom("bob", "games");

        // Assert
        assertTrue(manager.getMembers("dev").isEmpty());
    }

    @Test
    public void moveUserToRoom_sameRoom_userStaysInRoom() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();
        manager.addUserToRoom("bob", "dev");

        // Act
        manager.moveUserToRoom("bob", "dev");

        // Assert
        assertEquals("dev", manager.getUserRoom("bob"));
    }

    @Test
    public void removeUser_existingUser_userIsRemovedFromRoom() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();
        manager.addUserToRoom("alice", "dev");

        // Act
        manager.removeUser("alice");

        // Assert
        assertNull(manager.getUserRoom("alice"));
        assertTrue(manager.getMembers("dev").isEmpty());
    }

    @Test
    public void removeUser_unknownUser_doesNotThrow() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();

        // Act
        manager.removeUser("ghost");

        // Assert
        assertNull(manager.getUserRoom("ghost"));
    }

    @Test
    public void getMembers_unknownRoom_returnsEmptySet() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();

        // Act & Assert
        assertTrue(manager.getMembers("unknown-room").isEmpty());
    }

    @Test
    public void getRoomNameForTarget_all_returnsDefaultRoom() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();

        // Act
        String result = manager.getRoomNameForTarget("ALL");

        // Assert
        assertEquals(ChatRoomManager.DEFAULT_ROOM, result);
    }

    @Test
    public void getRoomNameForTarget_null_returnsDefaultRoom() {
        // Arrange
        ChatRoomManager manager = new ChatRoomManager();

        // Act
        String result = manager.getRoomNameForTarget(null);

        // Assert
        assertEquals(ChatRoomManager.DEFAULT_ROOM, result);
    }
}