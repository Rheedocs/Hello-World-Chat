package server;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class ClientRegistryTest {

    @Test
    public void registerWithDuplicateUsernameShouldFailSecondTime() {
        ClientRegistry registry = new ClientRegistry();
        ClientHandler firstClient = mock(ClientHandler.class);
        ClientHandler secondClient = mock(ClientHandler.class);

        boolean firstRegistration = registry.register("alice", firstClient);
        boolean secondRegistration = registry.register("alice", secondClient);

        assertTrue(firstRegistration);
        assertFalse(secondRegistration);
    }

    @Test
    public void unregisterUnknownUsernameShouldNotThrow() {
        ClientRegistry registry = new ClientRegistry();

        assertDoesNotThrow(() -> registry.unregister("ghost"));
        assertNull(registry.getClient("ghost"));
    }

    @Test
    public void getClientForUnknownUsernameShouldReturnNull() {
        ClientRegistry registry = new ClientRegistry();

        assertNull(registry.getClient("missing-user"));
    }
}
