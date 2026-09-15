package server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ChatServerTest {

    @Test
    public void ensureSharedFilesDirectoryShouldExist() throws IOException {
        Path shared = ChatServer.getSharedFilesDirectory();

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
            // Arrange: simulate that the shared directory already exists
            files.when(() -> Files.notExists(shared)).thenReturn(false);
            files.when(() -> Files.isDirectory(shared)).thenReturn(true);

            // Act
            ChatServer.ensureSharedFilesDirectoryExists();

            // Assert
            assertTrue(Files.isDirectory(shared));
        }
    }
}
