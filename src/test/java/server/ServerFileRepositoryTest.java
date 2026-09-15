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

// Klassens eneste ansvar er filsystemet, så vi tester mod rigtige filer i en @TempDir i stedet for at mocke.
public class ServerFileRepositoryTest {

    @Test
    public void constructor_pathIsNotDirectory_throwsIllegalArgument(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path file = Files.writeString(tempDir.resolve("fil.txt"), "x");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new ServerFileRepository(file));
    }

    @Test
    public void constructor_null_throwsNullPointer() {
        assertThrows(NullPointerException.class, () -> new ServerFileRepository(null));
    }

    @Test
    public void listFiles_filesAndSubfolder_returnsOnlyRegularFilesSorted(@TempDir Path tempDir) throws IOException {
        // Arrange
        Files.writeString(tempDir.resolve("b.txt"), "B");
        Files.writeString(tempDir.resolve("a.txt"), "A");
        Files.createDirectories(tempDir.resolve("submap"));
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        // Act
        List<String> result = repository.listFiles();

        // Assert
        assertEquals(List.of("a.txt", "b.txt"), result);
    }

    @Test
    public void listFiles_hiddenFile_isNotListed(@TempDir Path tempDir) throws IOException {
        // Arrange
        Files.writeString(tempDir.resolve(".gitkeep"), "");
        Files.writeString(tempDir.resolve("a.txt"), "A");
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        // Act
        List<String> result = repository.listFiles();

        // Assert
        assertEquals(List.of("a.txt"), result);
    }

    @Test
    public void listFiles_emptyFolder_returnsEmptyList(@TempDir Path tempDir) throws IOException {
        // Arrange
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        // Act
        List<String> result = repository.listFiles();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    public void readFile_existingFile_returnsContent(@TempDir Path tempDir) throws IOException {
        // Arrange
        Files.writeString(tempDir.resolve("hello.txt"), "Hej fra serveren", StandardCharsets.UTF_8);
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        // Act
        byte[] result = repository.readFile("hello.txt");

        // Assert
        assertEquals("Hej fra serveren", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    public void readFile_pathTraversal_throwsSecurityException(@TempDir Path tempDir) {
        // Arrange
        // Filen behøver ikke findes: stien skal afvises, før der overhovedet læses.
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        // Act & Assert
        assertThrows(SecurityException.class, () -> repository.readFile("../outside-secret.txt"));
    }

    @Test
    public void readFile_missingFile_throwsIOException(@TempDir Path tempDir) {
        // Arrange
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        // Act & Assert
        assertThrows(IOException.class, () -> repository.readFile("missing.txt"));
    }

    @Test
    public void readFile_directory_throwsIOException(@TempDir Path tempDir) throws IOException {
        // Arrange
        Files.createDirectories(tempDir.resolve("submap"));
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        // Act & Assert
        assertThrows(IOException.class, () -> repository.readFile("submap"));
    }

    @Test
    public void readFile_blankName_throwsIllegalArgument(@TempDir Path tempDir) {
        // Arrange
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> repository.readFile("  "));
    }

    @Test
    public void containsFile_existingFile_returnsTrue(@TempDir Path tempDir) throws IOException {
        // Arrange
        Files.writeString(tempDir.resolve("keep.txt"), "gemt");
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        // Act & Assert
        assertTrue(repository.containsFile("keep.txt"));
    }

    @Test
    public void containsFile_missingFile_returnsFalse(@TempDir Path tempDir) {
        // Arrange
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        // Act & Assert
        assertFalse(repository.containsFile("missing.txt"));
    }

    @Test
    public void containsFile_pathTraversal_returnsFalse(@TempDir Path tempDir) {
        // Arrange
        ServerFileRepository repository = new ServerFileRepository(tempDir);

        // Act & Assert
        assertFalse(repository.containsFile("../outside.txt"));
    }
}