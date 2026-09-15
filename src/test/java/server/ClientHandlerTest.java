package server;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import protocol.MessageSender;

// Socket og filrepository mockes, så vi tester ClientHandlers protokollogik uden netværk og filsystem.
public class ClientHandlerTest {
    private static final String NEWLINE = "\n";

    private ClientRegistry clientRegistry;
    private ChatRoomManager chatRoomManager;
    private ServerFileRepository fileRepository;
    private ByteArrayOutputStream clientOutput;

    @BeforeEach
    public void setUp() {
        clientRegistry = new ClientRegistry();
        chatRoomManager = new ChatRoomManager();
        fileRepository = mock(ServerFileRepository.class);
        clientOutput = new ByteArrayOutputStream();
    }

    @Test
    public void run_textBeforeLogin_sendsLoginRequiredError() throws IOException {
        // Act
        String output = runWithInput("TEXT|all|hej");

        // Assert
        assertTrue(output.contains("|ERROR|server||Du skal logge ind først."));
    }

    @Test
    public void run_validLogin_sendsOk() throws IOException {
        // Act
        String output = runWithInput("LOGIN||alice");

        // Assert
        assertTrue(output.contains("|OK|server|alice|"));
    }

    @Test
    public void run_blankUsername_sendsError() throws IOException {
        // Act
        String output = runWithInput("LOGIN||   ");

        // Assert
        assertTrue(output.contains("|ERROR|server||Brugernavnet kan ikke være tomt."));
    }

    @Test
    public void run_usernameTaken_sendsError() throws IOException {
        // Arrange
        clientRegistry.register("alice", mock(MessageSender.class));

        // Act
        String output = runWithInput("LOGIN||alice");

        // Assert
        assertTrue(output.contains("|ERROR|server|alice|Brugernavnet er optaget."));
    }

    @Test
    public void run_malformedMessage_sendsFormatError() throws IOException {
        // Act
        String output = runWithInput("bare noget tekst");

        // Assert
        assertTrue(output.contains("|ERROR|server||Fejlformateret besked"));
    }

    @Test
    public void run_unknownCommand_sendsUnknownCommandError() throws IOException {
        // Act
        String output = runWithInput("LOGIN||alice", "FOO|x|y");

        // Assert
        assertTrue(output.contains("|ERROR|server|alice|Ukendt kommando: FOO"));
    }

    @Test
    public void run_lowercaseCommand_isHandledCaseInsensitive() throws IOException {
        // Act
        String output = runWithInput("login||alice");

        // Assert
        assertTrue(output.contains("|OK|server|alice|"));
    }

    @Test
    public void run_privateToOnlineUser_forwardsMessageToRecipient() throws IOException {
        // Arrange
        MessageSender bob = mock(MessageSender.class);
        clientRegistry.register("bob", bob);

        // Act
        runWithInput("LOGIN||alice", "PRIVATE|bob|hej bob");

        // Assert
        verify(bob).sendServerMessage("PRIVATE", "alice", "bob", "hej bob");
    }

    @Test
    public void run_privateToOfflineUser_sendsNotOnlineError() throws IOException {
        // Act
        String output = runWithInput("LOGIN||alice", "PRIVATE|bob|hej");

        // Assert
        assertTrue(output.contains("|ERROR|server|alice|Brugeren bob er ikke online."));
    }

    @Test
    public void run_textInRoom_forwardsOnlyToOtherMembers() throws IOException {
        // Arrange
        MessageSender bob = mock(MessageSender.class);
        clientRegistry.register("bob", bob);
        chatRoomManager.addUserToRoom("bob", ChatRoomManager.DEFAULT_ROOM);

        // Act
        runWithInput("LOGIN||alice", "TEXT|all|hej alle");

        // Assert
        verify(bob).sendServerMessage("TEXT", "alice", ChatRoomManager.DEFAULT_ROOM, "hej alle");
    }

    @Test
    public void run_listFiles_sendsCommaSeparatedFileList() throws IOException {
        // Arrange
        when(fileRepository.listFiles()).thenReturn(List.of("a.txt", "b.txt"));

        // Act
        String output = runWithInput("LOGIN||alice", "LISTFILES||");

        // Assert
        assertTrue(output.contains("|FILELIST|server|alice|a.txt,b.txt"));
    }

    @Test
    public void run_getExistingFile_sendsBase64FileData() throws IOException {
        // Arrange
        when(fileRepository.readFile("hej.txt")).thenReturn("Hej".getBytes(StandardCharsets.UTF_8));

        // Act
        String output = runWithInput("LOGIN||alice", "GETFILE|hej.txt|");

        // Assert
        assertTrue(output.contains("|FILEDATA|server|alice|hej.txt|SGVq"));
    }

    @Test
    public void run_getFileWithoutName_sendsFileError() throws IOException {
        // Act
        String output = runWithInput("LOGIN||alice", "GETFILE||");

        // Assert
        assertTrue(output.contains("|FILEERROR|server|alice|Filnavn mangler."));
    }

    @Test
    public void run_getFileOutsideFolder_sendsAccessDenied() throws IOException {
        // Arrange
        when(fileRepository.readFile("../hemmelig.txt")).thenThrow(new SecurityException("../hemmelig.txt"));

        // Act
        String output = runWithInput("LOGIN||alice", "GETFILE|../hemmelig.txt|");

        // Assert
        assertTrue(output.contains("|FILEERROR|server|alice|Adgang nægtet"));
    }

    @Test
    public void run_getMissingFile_sendsReadError() throws IOException {
        // Arrange
        when(fileRepository.readFile("mangler.txt")).thenThrow(new IOException("Filen findes ikke"));

        // Act
        String output = runWithInput("LOGIN||alice", "GETFILE|mangler.txt|");

        // Assert
        assertTrue(output.contains("|FILEERROR|server|alice|Kunne ikke læse filen"));
    }

    @Test
    public void run_quit_sendsLogoutAndUnregistersUser() throws IOException {
        // Act
        String output = runWithInput("LOGIN||alice", "QUIT||");

        // Assert
        assertTrue(output.contains("|OK|server|alice|Du er nu logget ud."));
        assertNull(clientRegistry.getClient("alice"));
    }

    @Test
    public void run_connectionClosed_removesUserFromRegistryAndRoom() throws IOException {
        // Act
        runWithInput("LOGIN||alice");

        // Assert
        assertNull(clientRegistry.getClient("alice"));
        assertNull(chatRoomManager.getUserRoom("alice"));
    }

    // Kører handleren synkront: run() slutter, når input er læst, ligesom når klienten lukker forbindelsen.
    private String runWithInput(String... lines) throws IOException {
        String input = String.join(NEWLINE, lines) + NEWLINE;
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        when(socket.getOutputStream()).thenReturn(clientOutput);

        new ClientHandler(socket, clientRegistry, chatRoomManager, fileRepository).run();

        return clientOutput.toString(StandardCharsets.UTF_8);
    }
}