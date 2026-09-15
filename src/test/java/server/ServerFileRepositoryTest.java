package server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ServerFileRepositoryTest {

    @Test
    public void listFilesShouldReturnOnlyRegularFiles() throws IOException {
        // Arrange
        Path rootDirectory = Paths.get("/fake/root");
        Path rootNormalized = rootDirectory.toAbsolutePath().normalize();
        Path fileA = rootNormalized.resolve("a.txt");
        Path fileB = rootNormalized.resolve("b.txt");
        Path subDir = rootNormalized.resolve("submap");

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
            files.when(() -> Files.isDirectory(rootNormalized)).thenReturn(true);
            files.when(() -> Files.list(rootNormalized)).thenReturn(Stream.of(fileB, fileA, subDir));
            files.when(() -> Files.isRegularFile(fileA)).thenReturn(true);
            files.when(() -> Files.isRegularFile(fileB)).thenReturn(true);
            files.when(() -> Files.isRegularFile(subDir)).thenReturn(false);

            // Act
            ServerFileRepository repository = new ServerFileRepository(rootDirectory);
            List<String> result = repository.listFiles();

            // Assert
            assertEquals(List.of("a.txt", "b.txt"), result);
        }
    }

    @Test
    public void readFileShouldReturnFileContent() throws IOException {
        // Arrange
        Path rootDirectory = Paths.get("/fake/root");
        Path rootNormalized = rootDirectory.toAbsolutePath().normalize();
        Path hello = rootNormalized.resolve("hello.txt");
        byte[] bytes = "Hej fra serveren".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
            files.when(() -> Files.isDirectory(rootNormalized)).thenReturn(true);
            files.when(() -> Files.isRegularFile(hello)).thenReturn(true);
            files.when(() -> Files.readAllBytes(hello)).thenReturn(bytes);

            ServerFileRepository repository = new ServerFileRepository(rootDirectory);

            // Act & Assert
            assertEquals("Hej fra serveren", new String(repository.readFile("hello.txt"), java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    @Test
    public void readFileShouldRejectPathTraversal() throws IOException {
        // Arrange
        Path rootDirectory = Paths.get("/fake/root");
        Path rootNormalized = rootDirectory.toAbsolutePath().normalize();
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
            files.when(() -> Files.isDirectory(rootNormalized)).thenReturn(true);

            ServerFileRepository repository = new ServerFileRepository(rootDirectory);

            // Act & Assert
            assertThrows(SecurityException.class, () -> repository.readFile("../secret.txt"));
        }
    }

    @Test
    public void readFileShouldRejectMissingFile() throws IOException {
        // Arrange
        Path rootDirectory = Paths.get("/fake/root");
        Path rootNormalized = rootDirectory.toAbsolutePath().normalize();
        Path missing = rootNormalized.resolve("missing.txt");

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
            files.when(() -> Files.isDirectory(rootNormalized)).thenReturn(true);
            files.when(() -> Files.isRegularFile(missing)).thenReturn(false);

            ServerFileRepository repository = new ServerFileRepository(rootDirectory);

            // Act & Assert
            assertThrows(IOException.class, () -> repository.readFile("missing.txt"));
        }
    }
}
