package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class ServerFileRepository {
    private final Path rootDirectory;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ServerFileRepository(Path rootDirectory) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "Rodmappen kan ikke være null.").toAbsolutePath().normalize();
        if (!Files.isDirectory(this.rootDirectory)) {
            throw new IllegalArgumentException("Serverens delte mappe findes ikke: " + this.rootDirectory);
        }
    }

    public List<String> listFiles() throws IOException {
        lock.readLock().lock();
        try (Stream<Path> files = Files.list(rootDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    public byte[] readFile(String fileName) throws IOException {
        Path filePath = resolveSafeFilePath(fileName);

        lock.readLock().lock();
        try {
            if (!Files.isRegularFile(filePath)) {
                throw new IOException("Filen findes ikke: " + fileName);
            }
            return Files.readAllBytes(filePath);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean containsFile(String fileName) {
        try {
            Path filePath = resolveSafeFilePath(fileName);
            return Files.isRegularFile(filePath);
        } catch (IllegalArgumentException | SecurityException e) {
            return false;
        }
    }

    private Path resolveSafeFilePath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Filnavnet kan ikke være tomt.");
        }

        Path candidate = rootDirectory.resolve(fileName).normalize();
        if (!candidate.startsWith(rootDirectory)) {
            throw new SecurityException("Adgang til filen er ikke tilladt: " + fileName);
        }

        return candidate;
    }
}
