package server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import protocol.MessageSender;

public class ClientRegistryTest {

    @Test
    public void register_newUsername_returnsTrueAndClientCanBeFound() {
        // Arrange
        ClientRegistry registry = new ClientRegistry();
        MessageSender client = mock(MessageSender.class);

        // Act
        boolean registered = registry.register("alice", client);

        // Assert
        assertTrue(registered);
        assertSame(client, registry.getClient("alice"));
    }

    @Test
    public void register_duplicateUsername_returnsFalseAndKeepsFirstClient() {
        // Arrange
        ClientRegistry registry = new ClientRegistry();
        MessageSender firstClient = mock(MessageSender.class);
        MessageSender secondClient = mock(MessageSender.class);
        registry.register("alice", firstClient);

        // Act
        boolean registered = registry.register("alice", secondClient);

        // Assert
        assertFalse(registered);
        assertSame(firstClient, registry.getClient("alice"));
    }

    @Test
    public void register_sameUsernameConcurrently_onlyOneSucceeds() throws Exception {
        // Arrange
        ClientRegistry registry = new ClientRegistry();
        int threads = 20;
        Callable<Boolean> attempt = () -> registry.register("alice", mock(MessageSender.class));
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        // Act
        List<Future<Boolean>> results = pool.invokeAll(Collections.nCopies(threads, attempt));
        pool.shutdown();

        // Assert
        long successes = results.stream().filter(Future::resultNow).count();
        assertEquals(1, successes);
    }

    @Test
    public void register_afterUnregister_usernameCanBeReused() {
        // Arrange
        ClientRegistry registry = new ClientRegistry();
        registry.register("alice", mock(MessageSender.class));
        registry.unregister("alice");

        // Act
        boolean registered = registry.register("alice", mock(MessageSender.class));

        // Assert
        assertTrue(registered);
    }

    @Test
    public void unregister_unknownUsername_doesNotThrow() {
        // Arrange
        ClientRegistry registry = new ClientRegistry();

        // Act & Assert
        assertDoesNotThrow(() -> registry.unregister("ghost"));
    }

    @Test
    public void unregister_null_doesNotThrow() {
        // Arrange
        ClientRegistry registry = new ClientRegistry();

        // Act & Assert
        assertDoesNotThrow(() -> registry.unregister(null));
    }

    @Test
    public void getClient_unknownUsername_returnsNull() {
        // Arrange
        ClientRegistry registry = new ClientRegistry();

        // Act & Assert
        assertNull(registry.getClient("missing-user"));
    }

    @Test
    public void broadcastExceptSender_twoClients_onlyOtherClientReceivesMessage() {
        // Arrange
        ClientRegistry registry = new ClientRegistry();
        MessageSender alice = mock(MessageSender.class);
        MessageSender bob = mock(MessageSender.class);
        when(alice.getUsername()).thenReturn("alice");
        when(bob.getUsername()).thenReturn("bob");
        registry.register("alice", alice);
        registry.register("bob", bob);

        // Act
        registry.broadcastExceptSender("alice", "lobby", "hej");

        // Assert
        verify(bob).sendServerMessage("TEXT", "alice", "lobby", "hej");
        verify(alice, never()).sendServerMessage(anyString(), anyString(), anyString(), anyString());
    }
}