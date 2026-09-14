
package protocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import domain.Message;

public class MessageParser {
    private static final DateTimeFormatter SERVER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FIELD_SEPARATOR = "|";
    private static final int CLIENT_MESSAGE_PART_COUNT = 3;
    private static final int SERVER_MESSAGE_PART_COUNT = 5;

    public static Message parseClientMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Beskeden er tom.");
        }

        String[] parts = rawMessage.split("\\|", CLIENT_MESSAGE_PART_COUNT);
        if (parts.length != CLIENT_MESSAGE_PART_COUNT) {
            throw new IllegalArgumentException("Fejlformateret besked: brug TYPE|TARGET|PAYLOAD.");
        }

        return new Message(parts[0], parts[1], parts[2]);
    }

    public static String formatClientMessage(String type, String target, String payload) {
        return type + FIELD_SEPARATOR + target + FIELD_SEPARATOR + payload;
    }

    public static String formatServerMessage(String type, String sender, String target, String payload) {
        String timestamp = LocalDateTime.now().format(SERVER_TIME_FORMAT);
        return timestamp + FIELD_SEPARATOR + type + FIELD_SEPARATOR + sender + FIELD_SEPARATOR + target + FIELD_SEPARATOR + payload;
    }
}
