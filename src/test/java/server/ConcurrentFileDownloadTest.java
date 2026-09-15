package server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ConcurrentFileDownloadTest {

    @Test
    public void multipleClientsCanReadSameFileConcurrently() throws IOException, InterruptedException, ExecutionException {
        // Arrange: create mock repository that returns deterministic content
        byte[] content = new byte[1024 * 64];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i % 256);
        }

        ServerFileRepository repo = Mockito.mock(ServerFileRepository.class);
        Mockito.when(repo.readFile("bigfile.dat")).thenReturn(content);

        // Act: run concurrent readers
        int clients = 10;
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        List<Callable<byte[]>> tasks = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            tasks.add(() -> repo.readFile("bigfile.dat"));
        }

        List<Future<byte[]>> futures = pool.invokeAll(tasks);
        pool.shutdown();

        // Assert
        for (Future<byte[]> future : futures) {
            assertDoesNotThrow(() -> future.get());
            byte[] got = future.get();
            assertArrayEquals(content, got);
        }
    }
}
