package protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import domain.Message;

public class MessageParserTest {

    @Test
    public void parseClientMessage_validLogin_splitsTypeTargetAndPayload() {
        // Arrange
        String raw = "LOGIN||bob";

        // Act
        Message message = MessageParser.parseClientMessage(raw);

        // Assert
        assertEquals("LOGIN", message.getType());
        assertEquals("", message.getTarget());
        assertEquals("bob", message.getPayload());
    }

    @Test
    public void parseClientMessage_payloadContainsPipe_keepsPipeInPayload() {
        // Arrange
        String raw = "TEXT|all|a|b";

        // Act
        Message message = MessageParser.parseClientMessage(raw);

        // Assert
        assertEquals("a|b", message.getPayload());
    }

    @Test
    public void parseClientMessage_null_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> MessageParser.parseClientMessage(null));
    }

    @Test
    public void parseClientMessage_blank_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> MessageParser.parseClientMessage("   "));
    }

    @Test
    public void parseClientMessage_singleField_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> MessageParser.parseClientMessage("LOGIN"));
    }

    @Test
    public void parseClientMessage_twoFields_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> MessageParser.parseClientMessage("LOGIN|bob"));
    }

    @Test
    public void formatClientMessage_validFields_usesProtocolFormat() {
        // Act
        String formatted = MessageParser.formatClientMessage("TEXT", "room42", "Hej alle");

        // Assert
        assertEquals("TEXT|room42|Hej alle", formatted);
    }

    @Test
    public void formatServerMessage_validFields_includesTimestampAndAllFields() {
        // Act
        String formatted = MessageParser.formatServerMessage("TEXT", "alice", "room42", "Hej");

        // Assert
        String[] parts = formatted.split("\\|", 5);
        assertEquals(5, parts.length);
        assertEquals("TEXT", parts[1]);
        assertEquals("alice", parts[2]);
        assertEquals("room42", parts[3]);
        assertEquals("Hej", parts[4]);
    }

    @Test
    public void formatClientListFiles_noArguments_returnsEmptyTargetAndPayload() {
        // Act
        String formatted = MessageParser.formatClientListFiles();

        // Assert
        assertEquals("LISTFILES||", formatted);
    }

    @Test
    public void formatClientGetFile_validFileName_placesFileNameInTarget() {
        // Act
        String formatted = MessageParser.formatClientGetFile("readme.md");

        // Assert
        assertEquals("GETFILE|readme.md|", formatted);
    }

    @Test
    public void formatClientGetFile_null_returnsEmptyTarget() {
        // Act
        String formatted = MessageParser.formatClientGetFile(null);

        // Assert
        assertEquals("GETFILE||", formatted);
    }

    @Test
    public void parseServerMessage_validFileList_returnsTypeTargetAndPayload() {
        // Arrange
        String raw = "2026-09-15 09:00:00|FILELIST|server||file1.txt,file2.txt";

        // Act
        Message message = MessageParser.parseServerMessage(raw);

        // Assert
        assertEquals("FILELIST", message.getType());
        assertEquals("", message.getTarget());
        assertEquals("file1.txt,file2.txt", message.getPayload());
    }

    @Test
    public void parseServerMessage_fileDataPayloadWithPipe_keepsWholePayload() {
        // Arrange
        String raw = "2026-09-15 09:00:00|FILEDATA|server|alice|fil.txt|SGVq";

        // Act
        Message message = MessageParser.parseServerMessage(raw);

        // Assert
        assertEquals("fil.txt|SGVq", message.getPayload());
    }

    @Test
    public void parseServerMessage_tooFewFields_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> MessageParser.parseServerMessage("TEXT|all|hej"));
    }

    @Test
    public void parseServerMessage_null_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> MessageParser.parseServerMessage(null));
    }
}