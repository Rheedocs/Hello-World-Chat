package server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ConcurrentFileDownloadTest {
    private static final int FILE_SIZE_BYTES = 64 * 1024;
    private static final int CLIENT_COUNT = 10;

    @Test
    public void readFile_manyClientsSameFile_allReceiveIdenticalContent(@TempDir Path tempDir) throws Exception {
        // Arrange
        byte[] content = new byte[FILE_SIZE_BYTES];
        new Random(42).nextBytes(content);
        Files.write(tempDir.resolve("bigfile.dat"), content);
        ServerFileRepository repository = new ServerFileRepository(tempDir);
        Callable<byte[]> download = () -> repository.readFile("bigfile.dat");
        ExecutorService pool = Executors.newFixedThreadPool(CLIENT_COUNT);

        // Act
        List<Future<byte[]>> results = pool.invokeAll(Collections.nCopies(CLIENT_COUNT, download));
        pool.shutdown();

        // Assert
        assertTrue(results.stream().allMatch(result -> Arrays.equals(content, result.resultNow())));
    }
}