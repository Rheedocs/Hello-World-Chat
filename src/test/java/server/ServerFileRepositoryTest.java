package server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ServerFileRepositoryTest {

    @Test
    public void listFilesShouldReturnOnlyRegularFiles() throws IOException {
        Path rootDirectory = Files.createTempDirectory("server-files");
        Files.writeString(rootDirectory.resolve("b.txt"), "B");
        Files.writeString(rootDirectory.resolve("a.txt"), "A");
        Files.createDirectory(rootDirectory.resolve("submap"));

        ServerFileRepository repository = new ServerFileRepository(rootDirectory);

        assertEquals(List.of("a.txt", "b.txt"), repository.listFiles());
    }

    @Test
    public void readFileShouldReturnFileContent() throws IOException {
        Path rootDirectory = Files.createTempDirectory("server-files");
        Files.writeString(rootDirectory.resolve("hello.txt"), "Hej fra serveren");

        ServerFileRepository repository = new ServerFileRepository(rootDirectory);

        assertEquals("Hej fra serveren", new String(repository.readFile("hello.txt"), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    public void readFileShouldRejectPathTraversal() throws IOException {
        Path rootDirectory = Files.createTempDirectory("server-files");
        ServerFileRepository repository = new ServerFileRepository(rootDirectory);

        assertThrows(SecurityException.class, () -> repository.readFile("../secret.txt"));
    }

    @Test
    public void readFileShouldRejectMissingFile() throws IOException {
        Path rootDirectory = Files.createTempDirectory("server-files");
        ServerFileRepository repository = new ServerFileRepository(rootDirectory);

        assertThrows(IOException.class, () -> repository.readFile("missing.txt"));
    }
}
