
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class MessageParserTest {

    @Test
    public void parseClientMessageShouldSplitTypeTargetAndPayload() {
        Message message = MessageParser.parseClientMessage("LOGIN||bob");

        assertEquals("LOGIN", message.getType());
        assertEquals("", message.getTarget());
        assertEquals("bob", message.getPayload());
    }

    @Test
    public void formatClientMessageShouldUseProtocolFormat() {
        String formatted = MessageParser.formatClientMessage("TEXT", "room42", "Hej alle");

        assertEquals("TEXT|room42|Hej alle", formatted);
    }

    @Test
    public void formatServerMessageShouldIncludeTimestampAndFields() {
        String formatted = MessageParser.formatServerMessage("TEXT", "alice", "room42", "Hej");

        assertNotNull(formatted);
        String[] parts = formatted.split("\\|", 5);
        assertEquals(5, parts.length);
        assertEquals("TEXT", parts[1]);
        assertEquals("alice", parts[2]);
        assertEquals("room42", parts[3]);
        assertEquals("Hej", parts[4]);
    }
}
