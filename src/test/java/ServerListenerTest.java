import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class ServerListenerTest {

    @Test
    public void serverListenerShouldFormatTextPrivateAndErrorMessagesReadably() {
        assertEquals("alice: Hej alle", ServerListener.formatForDisplay("2026-09-11 13:00:00|TEXT|alice|all|Hej alle"));
        assertEquals("[Privat fra bob]: Hej Alice", ServerListener.formatForDisplay("2026-09-11 13:00:00|PRIVATE|bob|alice|Hej Alice"));
        assertEquals("[Fejl]: Brugernavn er optaget", ServerListener.formatForDisplay("2026-09-11 13:00:00|ERROR|server|client|Brugernavn er optaget"));
    }

    @Test
    public void serverListenerShouldNotQueueMessagesAfterLoginSuccess() throws Exception {
        AtomicBoolean connectionLost = new AtomicBoolean(false);
        BlockingQueue<String> serverMessages = new ArrayBlockingQueue<>(64);
        AtomicBoolean loginPhase = new AtomicBoolean(false);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 75; i++) {
            builder.append("TEXT|all|message-").append(i).append(System.lineSeparator());
        }

        ServerListener listener = new ServerListener(
                new BufferedReader(new StringReader(builder.toString())),
                new Socket(),
                connectionLost,
                serverMessages,
                loginPhase);

        Thread thread = new Thread(listener);
        thread.start();
        thread.join();

        assertTrue(connectionLost.get());
        assertTrue(serverMessages.isEmpty());
    }
}
