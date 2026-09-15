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

public class ServerListener implements Runnable {
    private final BufferedReader input;
    private final Socket socket;
    private final AtomicBoolean connectionLost;
    private final java.util.concurrent.BlockingQueue<String> serverMessages;
    private final AtomicBoolean loginPhase;

    public ServerListener(BufferedReader input, Socket socket, AtomicBoolean connectionLost) {
        this(input, socket, connectionLost, null, new AtomicBoolean(true));
    }

    public ServerListener(BufferedReader input, Socket socket, AtomicBoolean connectionLost,
            java.util.concurrent.BlockingQueue<String> serverMessages) {
        this(input, socket, connectionLost, serverMessages, new AtomicBoolean(true));
    }

    public ServerListener(BufferedReader input, Socket socket, AtomicBoolean connectionLost,
            java.util.concurrent.BlockingQueue<String> serverMessages, AtomicBoolean loginPhase) {
        this.input = input;
        this.socket = socket;
        this.connectionLost = connectionLost;
        this.serverMessages = serverMessages;
        this.loginPhase = loginPhase;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String message = input.readLine();
                if (message == null) {
                    connectionLost.set(true);
                    closeSocket();
                    break;
                }
                if (serverMessages != null && loginPhase.get()) {
                    serverMessages.put(message);
                    continue;
                }

                // Try to parse server message and handle special file-related messages
                try {
                    java.util.concurrent.atomic.AtomicReference<String> display = new java.util.concurrent.atomic.AtomicReference<>();
                    display.set(processServerMessage(message));
                    System.out.println(display.get());
                } catch (Exception e) {
                    // Fallback to simple display on any parse/handling error
                    System.out.println(formatForDisplay(message));
                }
            }
        } catch (IOException e) {
            connectionLost.set(true);
            closeSocket();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String processServerMessage(String rawMessage) throws IOException {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return "";
        }

        // Parse into server message components
        var msg = MessageParser.parseServerMessage(rawMessage);
        String type = msg.getType().toUpperCase();
        String sender = rawMessage.split("\\|", 5)[2]; // keep original sender for display
        String payload = msg.getPayload();

        switch (type) {
            case "FILELIST":
                if (payload == null || payload.isBlank()) {
                    return "[Info]: Ingen filer på serveren.";
                }
                return "Filer på serveren: " + payload.replace(',', ',');
            case "FILEERROR":
                return "[Filfejl fra " + sender + "]: " + payload;
            case "FILEDATA":
                // Expect payload format: filename|<base64>
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
