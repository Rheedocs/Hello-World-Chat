package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Holder serverens delte filkatalog og beskytter læsning af filer mod path traversal og samtidige adgangsfejl.
 * Klassen bruges til at liste filer og hente bestemt indhold inden for den godkendte rodmappe.
 */
public class ServerFileRepository {
    private final Path rootDirectory;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Opretter en repository, der er låst til en konkret rodmappe og validerer, at den faktisk er en mappe.
     */
    public ServerFileRepository(Path rootDirectory) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "Rodmappen kan ikke være null.").toAbsolutePath().normalize();
        if (!Files.isDirectory(this.rootDirectory)) {
            throw new IllegalArgumentException("Serverens delte mappe findes ikke: " + this.rootDirectory);
        }
    }

    /**
     * Returnerer alle almindelige, ikke skjulte filer i rodmappen sorteret alfabetisk.
     */
    public List<String> listFiles() throws IOException {
        lock.readLock().lock();
        try (Stream<Path> files = Files.list(rootDirectory)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    // Skjulte filer som .gitkeep er kun til Git og skal ikke vises for brugeren.
                    .filter(name -> !name.startsWith("."))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Læser indholdet af en sikker fil i rodmappen og returnerer det som bytes.
     */
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

    /**
     * Returnerer sandt, hvis den angivne fil findes inde i rodmappen og er en almindelig fil.
     */
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
        // Vi sammenligner med den absoluttere rodmappe for at afvise path traversal som ../outside.txt.
        if (!candidate.startsWith(rootDirectory)) {
            throw new SecurityException("Adgang til filen er ikke tilladt: " + fileName);
        }

        return candidate;
    }
}