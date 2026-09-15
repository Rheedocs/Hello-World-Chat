package server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class ChatServerTest {

    @Test
    public void ensureSharedFilesDirectoryShouldExist() throws IOException {
        ChatServer.ensureSharedFilesDirectoryExists();

        assertTrue(Files.isDirectory(ChatServer.getSharedFilesDirectory()));
    }
}
