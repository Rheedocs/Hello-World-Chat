package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

import protocol.MessageParser;

/**
 * Lytter på indkommende serverbeskeder og formaterer dem for menneskelæselig visning i klienten.
 * Klassen håndterer også fil-listing og download af data fra serveren med sikkerhedstjek mod path traversal.
 */
public class ServerListener implements Runnable {
    private final BufferedReader input;
    private final Socket socket;
    private final AtomicBoolean connectionLost;
    private final java.util.concurrent.BlockingQueue<String> serverMessages;
    private final AtomicBoolean loginPhase;

    /**
     * Opretter en listener uden en tilknyttet message-queue.
     */
    public ServerListener(BufferedReader input, Socket socket, AtomicBoolean connectionLost) {
        this(input, socket, connectionLost, null, new AtomicBoolean(true));
    }

    /**
     * Opretter en listener, der gemmer indkommende serverbeskeder i en queue under loginfasen.
     */
    public ServerListener(BufferedReader input, Socket socket, AtomicBoolean connectionLost,
            java.util.concurrent.BlockingQueue<String> serverMessages) {
        this(input, socket, connectionLost, serverMessages, new AtomicBoolean(true));
    }

    /**
     * Opretter en listener med alle nødvendige flags for login- og chatfase.
     */
    public ServerListener(BufferedReader input, Socket socket, AtomicBoolean connectionLost,
            java.util.concurrent.BlockingQueue<String> serverMessages, AtomicBoolean loginPhase) {
        this.input = input;
        this.socket = socket;
        this.connectionLost = connectionLost;
        this.serverMessages = serverMessages;
        this.loginPhase = loginPhase;
    }

    /**
     * Læser hele tiden serverkommunikationen og viser den i et læsbart format for brugeren.
     */
    @Override
    public void run() {
        try {
            while (true) {
                String message = readNextServerMessage();
                if (message == null) {
                    handleSocketClosed();
                    break;
                }
                if (serverMessages != null && loginPhase.get()) {
                    handleLoginQueue(message);
                    continue;
                }

                // Viser beskeden og håndterer filrelaterede beskeder særskilt
                displayIncomingMessage(message);
            }
        } catch (IOException e) {
            handleSocketClosed();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String readNextServerMessage() throws IOException {
        return input.readLine();
    }

    private void handleLoginQueue(String message) throws InterruptedException {
        serverMessages.put(message);
    }

    private void displayIncomingMessage(String message) {
        try {
            // Vi parser den samme serverbesked som før for at håndtere FILELIST/FILEDATA/FILEERROR.
            System.out.println(processServerMessage(message));
        } catch (Exception e) {
            // Falder tilbage til simpel visning, hvis parsing eller håndtering fejler
            System.out.println(formatForDisplay(message));
        }
    }

    private void handleSocketClosed() {
        connectionLost.set(true);
        closeSocket();
    }

    private String processServerMessage(String rawMessage) throws IOException {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return "";
        }

        // Opdeler beskeden i protokolfelter
        var msg = MessageParser.parseServerMessage(rawMessage);
        String type = msg.getType().toUpperCase();
        String sender = rawMessage.split("\\|", 5)[2]; // bevarer den oprindelige afsender til visning
        String payload = msg.getPayload();

        switch (type) {
            case "FILELIST":
                if (payload == null || payload.isBlank()) {
                    return "[Info]: Ingen filer på serveren.";
                }
                return "Filer på serveren: " + payload.replace(",", ", ");
            case "FILEERROR":
                return "[Filfejl fra " + sender + "]: " + payload;
            case "FILEDATA":
                // Forventet payload: filnavn|<base64>
                String[] parts = payload.split("\\|", 2);
                if (parts.length < 2) {
                    return "[Fejl]: Ugyldigt FILEDATA-payload.";
                }
                String fileName = parts[0];
                String base64 = parts[1];

                Path downloads = Paths.get("downloads").toAbsolutePath().normalize();
                if (Files.notExists(downloads)) {
                    Files.createDirectories(downloads);
                }

                Path out = downloads.resolve(fileName).normalize();
                if (!out.startsWith(downloads)) {
                    return "[Sikkerhedsfejl]: Ugyldigt filnavn modtaget: " + fileName;
                }

                byte[] data = Base64.getDecoder().decode(base64);
                Files.write(out, data);

                return "Modtaget fil '" + fileName + "' fra " + sender + ", gemt som: " + out.toString() + " (" + data.length + " bytes)";
            default:
                return formatForDisplay(rawMessage);
        }
    }

    static String formatForDisplay(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return "";
        }

        String[] parts = rawMessage.split("\\|", 5);
        if (parts.length < 5) {
            return rawMessage;
        }

        String type = parts[1].toUpperCase();
        String sender = parts[2];
        String payload = parts[4];

        switch (type) {
            case "TEXT":
                return sender + ": " + payload;
            case "PRIVATE":
                return "[Privat fra " + sender + "]: " + payload;
            case "ERROR":
                return "[Fejl]: " + payload;
            case "OK":
                if (payload == null || payload.isBlank()) {
                    return "[Info]: OK";
                }
                return "[Info]: " + payload;
            default:
                return payload;
        }
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
