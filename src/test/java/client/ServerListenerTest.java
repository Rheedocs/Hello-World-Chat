package client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class ServerListenerTest {
    private static final long THREAD_TIMEOUT_MS = 2000L;
    private static final int QUEUE_CAPACITY = 64;

    @Test
    public void formatForDisplay_textMessage_returnsSenderAndPayload() {
        // Arrange
        String raw = "2026-09-11 13:00:00|TEXT|alice|all|Hej alle";

        // Act
        String result = ServerListener.formatForDisplay(raw);

        // Assert
        assertEquals("alice: Hej alle", result);
    }

    @Test
    public void formatForDisplay_privateMessage_returnsPrivatePrefix() {
        // Arrange
        String raw = "2026-09-11 13:00:00|PRIVATE|bob|alice|Hej Alice";

        // Act
        String result = ServerListener.formatForDisplay(raw);

        // Assert
        assertEquals("[Privat fra bob]: Hej Alice", result);
    }

    @Test
    public void formatForDisplay_errorMessage_returnsErrorPrefix() {
        // Arrange
        String raw = "2026-09-11 13:00:00|ERROR|server|client|Brugernavn er optaget";

        // Act
        String result = ServerListener.formatForDisplay(raw);

        // Assert
        assertEquals("[Fejl]: Brugernavn er optaget", result);
    }

    @Test
    public void formatForDisplay_okWithEmptyPayload_returnsDefaultInfo() {
        // Arrange
        String raw = "2026-09-11 13:00:00|OK|server|alice|";

        // Act
        String result = ServerListener.formatForDisplay(raw);

        // Assert
        assertEquals("[Info]: OK", result);
    }

    @Test
    public void formatForDisplay_payloadContainsPipe_keepsWholePayload() {
        // Arrange
        String raw = "2026-09-11 13:00:00|TEXT|alice|all|a|b|c";

        // Act
        String result = ServerListener.formatForDisplay(raw);

        // Assert
        assertEquals("alice: a|b|c", result);
    }

    @Test
    public void formatForDisplay_tooFewFields_returnsRawMessage() {
        // Arrange
        String raw = "TEXT|all|hej";

        // Act
        String result = ServerListener.formatForDisplay(raw);

        // Assert
        assertEquals(raw, result);
    }

    @Test
    public void formatForDisplay_null_returnsEmptyString() {
        // Act
        String result = ServerListener.formatForDisplay(null);

        // Assert
        assertEquals("", result);
    }

    @Test
    public void formatForDisplay_blank_returnsEmptyString() {
        // Act
        String result = ServerListener.formatForDisplay("   ");

        // Assert
        assertEquals("", result);
    }

    @Test
    public void run_loginPhase_queuesServerMessages() throws Exception {
        // Arrange
        String serverLine = "2026-09-11 13:00:00|OK|server|alice|";
        BlockingQueue<String> serverMessages = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        ServerListener listener = new ServerListener(
                new BufferedReader(new StringReader(serverLine + System.lineSeparator())),
                new Socket(),
                new AtomicBoolean(false),
                serverMessages,
                new AtomicBoolean(true));

        // Act
        runAndWait(listener);

        // Assert
        assertEquals(serverLine, serverMessages.poll());
    }

    @Test
    public void run_afterLogin_doesNotQueueMessagesEvenBeyondQueueCapacity() throws Exception {
        // Arrange
        // Flere linjer end køen kan rumme: hvis de blev lagt i køen, ville tråden blokere for evigt.
        String manyLines = ("2026-09-11 13:00:00|TEXT|bob|all|hej" + System.lineSeparator()).repeat(QUEUE_CAPACITY + 11);
        BlockingQueue<String> serverMessages = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        ServerListener listener = new ServerListener(
                new BufferedReader(new StringReader(manyLines)),
                new Socket(),
                new AtomicBoolean(false),
                serverMessages,
                new AtomicBoolean(false));

        // Act
        Thread thread = runAndWait(listener);

        // Assert
        assertFalse(thread.isAlive());
        assertTrue(serverMessages.isEmpty());
    }

    @Test
    public void run_streamEnds_setsConnectionLost() throws Exception {
        // Arrange
        AtomicBoolean connectionLost = new AtomicBoolean(false);
        ServerListener listener = new ServerListener(
                new BufferedReader(new StringReader("")),
                new Socket(),
                connectionLost);

        // Act
        runAndWait(listener);

        // Assert
        assertTrue(connectionLost.get());
    }

    private static Thread runAndWait(ServerListener listener) throws InterruptedException {
        Thread thread = new Thread(listener);
        thread.start();
        thread.join(THREAD_TIMEOUT_MS);
        return thread;
    }
}