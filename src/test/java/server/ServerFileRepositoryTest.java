package server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ServerFileRepositoryTest {

    @Test
    public void listFilesShouldReturnOnlyRegularFiles(@TempDir Path tempDir) throws IOException {
        Path fileA = tempDir.resolve("a.txt");
        Path fileB = tempDir.resolve("b.txt");
        Path subDir = tempDir.resolve("submap");

        Files.writeString(fileA, "A");
        Files.writeString(fileB, "B");
        Files.createDirectories(subDir);

        ServerFileRepository repository = new ServerFileRepository(tempDir);

        List<String> result = repository.listFiles();

        assertEquals(List.of("a.txt", "b.txt"), result);
    }

    @Test
    public void readFileShouldReturnFileContent(@TempDir Path tempDir) throws IOException {
        Path hello = tempDir.resolve("hello.txt");
        Files.writeString(hello, "Hej fra serveren", StandardCharsets.UTF_8);

        ServerFileRepository repository = new ServerFileRepository(tempDir);

        assertEquals("Hej fra serveren", new String(repository.readFile("hello.txt"), StandardCharsets.UTF_8));
    }

    @Test
    public void containsFileShouldReturnTrueForRealFilesAndFalseForTraversalAttempt(@TempDir Path tempDir) throws IOException {
        Path realFile = tempDir.resolve("keep.txt");
        Files.writeString(realFile, "gemt", StandardCharsets.UTF_8);

        ServerFileRepository repository = new ServerFileRepository(tempDir);

        assertTrue(repository.containsFile("keep.txt"));
        assertFalse(repository.containsFile("missing.txt"));
        assertFalse(repository.containsFile("../outside.txt"));
    }

    @Test
    public void readFileShouldRejectPathTraversal(@TempDir Path tempDir) {
        Path outsideFile = tempDir.getParent().resolve("outside-secret.txt");
        try {
            Files.writeString(outsideFile, "hemmeligt", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke oprette fil udenfor temp-mappen til traversal-test", e);
        }

        ServerFileRepository repository = new ServerFileRepository(tempDir);

        assertThrows(SecurityException.class, () -> repository.readFile("../outside-secret.txt"));
        assertFalse(repository.containsFile("../outside-secret.txt"));
    }

    @Test
    public void readFileShouldRejectMissingFile(@TempDir Path tempDir) {
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        assertThrows(IOException.class, () -> repository.readFile("missing.txt"));
    }
}
