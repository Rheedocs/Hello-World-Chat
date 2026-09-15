package server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

// Files mockes, så testen ikke opretter mapper på disken og kun tester beslutningen i metoden.
public class ChatServerTest {

    @Test
    public void ensureSharedFilesDirectoryExists_folderMissing_createsFolder() throws IOException {
        // Arrange
        Path shared = ChatServer.getSharedFilesDirectory();
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
            files.when(() -> Files.notExists(shared)).thenReturn(true);

            // Act
            ChatServer.ensureSharedFilesDirectoryExists();

            // Assert
            files.verify(() -> Files.createDirectories(shared));
        }
    }

    @Test
    public void ensureSharedFilesDirectoryExists_folderExists_doesNotCreateFolder() throws IOException {
        // Arrange
        Path shared = ChatServer.getSharedFilesDirectory();
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
            files.when(() -> Files.notExists(shared)).thenReturn(false);

            // Act
            ChatServer.ensureSharedFilesDirectoryExists();

            // Assert
            files.verify(() -> Files.createDirectories(any(Path.class)), never());
        }
    }
}