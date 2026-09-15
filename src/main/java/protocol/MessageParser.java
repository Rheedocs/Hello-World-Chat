package protocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import domain.Message;

/**
 * Parser og formatterer de tekstbaserede protokolbeskeder mellem klient og server.
 * Klassen bruger en fast separator og de samme felter i hele chatten, så begge sider kan fortolke meddelelsen ens.
 */
public class MessageParser {
    private static final DateTimeFormatter SERVER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FIELD_SEPARATOR = "|";
    private static final int CLIENT_MESSAGE_PART_COUNT = 3;
    private static final int SERVER_MESSAGE_PART_COUNT = 5;

    // Fælles protokoltyper, så klient og server bruger præcis de samme strenge.
    public static final String TYPE_LOGIN = "LOGIN";
    public static final String TYPE_JOIN_ROOM = "JOIN_ROOM";
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_PRIVATE = "PRIVATE";
    public static final String TYPE_QUIT = "QUIT";
    public static final String TYPE_OK = "OK";
    public static final String TYPE_ERROR = "ERROR";

    // Filoverførsel bruger disse protokoltyper i klient/server-kommunikationen.
    public static final String TYPE_LIST_FILES = "LISTFILES";
    public static final String TYPE_GET_FILE = "GETFILE";
    public static final String TYPE_FILE_LIST = "FILELIST";
    public static final String TYPE_FILE_DATA = "FILEDATA";
    public static final String TYPE_FILE_ERROR = "FILEERROR";

    /**
     * Parser en klientbesked i formatet TYPE|TARGET|PAYLOAD og returnerer den tilhørende value object.
     */
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

    /**
     * Formaterer en klientbesked med type, mål og payload i den fælles tekstprotokol.
     */
    public static String formatClientMessage(String type, String target, String payload) {
        return type + FIELD_SEPARATOR + target + FIELD_SEPARATOR + payload;
    }

    /**
     * Formaterer en serverbesked med tidsstempel, type, afsender, mål og payload.
     */
    public static String formatServerMessage(String type, String sender, String target, String payload) {
        String timestamp = LocalDateTime.now().format(SERVER_TIME_FORMAT);
        return timestamp + FIELD_SEPARATOR + type + FIELD_SEPARATOR + sender + FIELD_SEPARATOR + target + FIELD_SEPARATOR + payload;
    }

    // Hjælpermetoderne holder filoverførselskommandoerne konsistente i hele protokollen.
    /**
     * Bygger en LISTFILES-forespørgsel til serveren.
     */
    public static String formatClientListFiles() {
        return formatClientMessage(TYPE_LIST_FILES, "", "");
    }

    /**
     * Bygger en GETFILE-forespørgsel for et bestemt filnavn.
     */
    public static String formatClientGetFile(String fileName) {
        if (fileName == null) fileName = "";
        return formatClientMessage(TYPE_GET_FILE, fileName, "");
    }

    /**
     * Parser en server-besked i formatet TIMESTAMP|TYPE|SENDER|TARGET|PAYLOAD og returnerer den relevante beskedmodel.
     */
    public static Message parseServerMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Beskeden er tom.");
        }

        // Vi splitter kun op til 5 felter, så payloaden kan indeholde resten af linjen uden at blive beskåret.
        String[] parts = rawMessage.split("\\|", SERVER_MESSAGE_PART_COUNT);
        if (parts.length != SERVER_MESSAGE_PART_COUNT) {
            throw new IllegalArgumentException("Fejlformateret serverbesked: forventer TIMESTAMP|TYPE|SENDER|TARGET|PAYLOAD.");
        }

        return new Message(parts[1], parts[3], parts[4]);
    }
}