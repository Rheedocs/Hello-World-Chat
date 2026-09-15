package server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ConcurrentFileDownloadTest {

    @Test
    public void multipleClientsCanReadSameFileConcurrently(@TempDir Path tempDir) throws IOException, InterruptedException, ExecutionException {
        byte[] content = new byte[1024 * 64];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i % 256);
        }

        Path bigFile = tempDir.resolve("bigfile.dat");
        Files.write(bigFile, content);

        ServerFileRepository repo = new ServerFileRepository(tempDir);

        int clients = 10;
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        List<Callable<byte[]>> tasks = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            tasks.add(() -> repo.readFile("bigfile.dat"));
        }

        List<Future<byte[]>> futures = pool.invokeAll(tasks);
        pool.shutdown();

        for (Future<byte[]> future : futures) {
            byte[] got = future.get();
            assertArrayEquals(content, got);
        }
    }
}
