import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageParser {
    private static final DateTimeFormatter SERVER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Message parseClientMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Beskeden er tom.");
        }

        String[] parts = rawMessage.split("\\|", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Fejlformateret besked: brug TYPE|TARGET|PAYLOAD.");
        }

        return new Message(parts[0], parts[1], parts[2]);
    }

    public static String formatClientMessage(String type, String target, String payload) {
        return type + "|" + target + "|" + payload;
    }

    public static String formatServerMessage(String type, String sender, String target, String payload) {
        String timestamp = LocalDateTime.now().format(SERVER_TIME_FORMAT);
        return timestamp + "|" + type + "|" + sender + "|" + target + "|" + payload;
    }
}
