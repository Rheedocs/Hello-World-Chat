
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageParser {
    private static final DateTimeFormatter SERVER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FIELD_SEPARATOR = "|";
    private static final int CLIENT_MESSAGE_PART_COUNT = 3;
    private static final int SERVER_MESSAGE_PART_COUNT = 5;

    // File transfer related message types
    public static final String TYPE_LIST_FILES = "LISTFILES";
    public static final String TYPE_GET_FILE = "GETFILE";
    public static final String TYPE_FILE_LIST = "FILELIST";
    public static final String TYPE_FILE_DATA = "FILEDATA";
    public static final String TYPE_FILE_ERROR = "FILEERROR";

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

    // Convenience helpers for the file transfer protocol
    public static String formatClientListFiles() {
        return formatClientMessage(TYPE_LIST_FILES, "", "");
    }

    public static String formatClientGetFile(String fileName) {
        if (fileName == null) fileName = "";
        return formatClientMessage(TYPE_GET_FILE, fileName, "");
    }

    /**
     * Parse a server-side message (TIMESTAMP|TYPE|SENDER|TARGET|PAYLOAD) and return a Message
     * where type == TYPE and target == TARGET and payload == PAYLOAD.
     */
    public static Message parseServerMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Beskeden er tom.");
        }

        String[] parts = rawMessage.split("\\|", SERVER_MESSAGE_PART_COUNT);
        if (parts.length != SERVER_MESSAGE_PART_COUNT) {
            throw new IllegalArgumentException("Fejlformateret serverbesked: forventer TIMESTAMP|TYPE|SENDER|TARGET|PAYLOAD.");
        }

        return new Message(parts[1], parts[3], parts[4]);
    }
}
