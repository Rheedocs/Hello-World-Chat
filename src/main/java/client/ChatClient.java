package client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import protocol.MessageParser;

/**
 * Klienten opretter forbindelsen til serveren, håndterer login og sender eller modtager chatbeskeder.
 * Klassen holder også styr på lokale slash-kommander, som ikke skal sendes videre til serveren.
 */
public class ChatClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;
    private static final int INPUT_QUEUE_SIZE = 64;
    private static final long USER_INPUT_TIMEOUT_MS = 30000L;
    private static final long CONNECTION_CHECK_TIMEOUT_MS = 500L;
    private static final String COMMAND_PRIVATE = "PRIVATE";
    private static final String COMMAND_JOIN_ROOM = "JOIN_ROOM";
    private static final String COMMAND_QUIT = "QUIT";
    private static final String COMMAND_LOGIN = "LOGIN";
    private static final String COMMAND_TEXT = "TEXT";
    private static final String MESSAGE_TYPE_OK = "OK";
    private static final String MESSAGE_TYPE_ERROR = "ERROR";
    private static final String DEFAULT_ROOM = "all";

    /**
     * Starter klienten, opretter forbindelse til serveren, logger brugeren ind og går ind i chat-løkken.
     */
    public static void main(String[] args) {
        configureUtf8Console();

        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            port = parsePort(args[1]);
        }

        System.out.println("Forbinder til " + host + ":" + port);
        // Scanner lukkes bevidst ikke: lukning blokerer, mens inputtråden venter på System.in.
        Scanner scanner = new Scanner(new InputStreamReader(new BufferedInputStream(System.in), StandardCharsets.UTF_8));
        try (Socket socket = new Socket(host, port);
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            AtomicBoolean connectionLost = new AtomicBoolean(false);
            BlockingQueue<String> serverMessages = new ArrayBlockingQueue<>(64);
            AtomicBoolean loginPhase = new AtomicBoolean(true);
            Thread listener = new Thread(new ServerListener(serverInput, socket, connectionLost, serverMessages, loginPhase));
            listener.start();

            printCommandHelp();

            BlockingQueue<String> inputQueue = new ArrayBlockingQueue<>(INPUT_QUEUE_SIZE);
            Thread inputThread = new Thread(() -> {
                try {
                    while (true) {
                        String line = scanner.nextLine();
                        inputQueue.put(line);
                    }
                } catch (InterruptedException e) {
                    // afsluttes normalt
                } catch (java.util.NoSuchElementException e) {
                    // scanner lukket / EOF
                }
            });
            inputThread.setDaemon(true);
            inputThread.start();

            boolean loggedIn = false;
            while (!loggedIn) {
                String username = readUsername(inputQueue, connectionLost);
                if (username == null) {
                    break;
                }
                if (username.isBlank()) {
                    continue;
                }

                out.println(MessageParser.formatClientMessage(COMMAND_LOGIN, "", username));
                loggedIn = waitForLoginResult(serverMessages, loginPhase, connectionLost);
                if (!loggedIn) {
                    if (connectionLost.get()) {
                        System.out.println("Forbindelsen til serveren blev afbrudt.");
                        break;
                    }
                    System.out.println("Login fejlede. Prøv igen.");
                }
            }

            if (loggedIn) {
                handleChatLoop(inputQueue, connectionLost, out);
            }
            stopInputThread(inputThread);

        } catch (IOException e) {
            System.err.println("Clientfejl: " + e.getMessage());
        }

        System.out.println("Client lukker.");
    }

    private static void configureUtf8Console() {
        try {
            System.setOut(new PrintStream(new BufferedOutputStream(System.out), true, StandardCharsets.UTF_8.name()));
            System.setErr(new PrintStream(new BufferedOutputStream(System.err), true, StandardCharsets.UTF_8.name()));
        } catch (Exception e) {
            System.err.println("Kunne ikke aktivere UTF-8 på konsollen: " + e.getMessage());
        }
    }

    private static int parsePort(String rawPort) {
        try {
            return Integer.parseInt(rawPort);
        } catch (NumberFormatException e) {
            System.err.println("Ugyldig port, bruger " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }

    private static String readUsername(BlockingQueue<String> inputQueue, AtomicBoolean connectionLost) {
        System.out.print("Indtast brugernavn: ");
        try {
            while (true) {
                // Poll med timeout, så vi opdager en lukket server, mens brugeren ikke har skrevet noget.
                String username = inputQueue.poll(CONNECTION_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (username != null) {
                    return username.trim();
                }
                if (connectionLost.get()) {
                    System.out.println();
                    System.out.println("Forbindelsen til serveren blev afbrudt.");
                    return null;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static boolean waitForLoginResult(BlockingQueue<String> serverMessages, AtomicBoolean loginPhase, AtomicBoolean connectionLost) {
        while (true) {
            try {
                String serverMessage = serverMessages.poll(CONNECTION_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (serverMessage == null) {
                    if (connectionLost.get()) {
                        return false;
                    }
                    continue;
                }
                String[] parts = serverMessage.split("\\|", 5);
                if (parts.length < 4) {
                    continue;
                }
                String status = parts[1].toUpperCase();
                if (MESSAGE_TYPE_OK.equals(status)) {
                    loginPhase.set(false);
                    System.out.println("Velkommen, " + parts[3] + "!");
                    return true;
                }
                if (MESSAGE_TYPE_ERROR.equals(status)) {
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private static void printCommandHelp() {
        System.out.println("Kommandoer: /w <bruger> <besked>, /join <rum>, /list, /get <filnavn>, /quit, /help. Skriv bare almindelig tekst for at chatte i dit nuværende rum.");
    }

    private static String parseSlashCommand(String line) {
        if (line == null) {
            return null;
        }

        String trimmed = line.trim();
        if (!trimmed.startsWith("/")) {
            return null;
        }

        String[] parts = trimmed.split("\\s+", 3);
        String command = parts[0].substring(1).toLowerCase();

        switch (command) {
            case "w":
            case "whisper":
                if (parts.length < 3) {
                    return null;
                }
                return MessageParser.formatClientMessage(COMMAND_PRIVATE, parts[1], parts[2]);
            case "list":
                return MessageParser.formatClientListFiles();
            case "get":
                if (parts.length < 2) {
                    return null;
                }
                return MessageParser.formatClientGetFile(parts[1]);
            case "join":
                if (parts.length < 2) {
                    return null;
                }
                return MessageParser.formatClientMessage(COMMAND_JOIN_ROOM, parts[1], "");
            case "quit":
                return MessageParser.formatClientMessage(COMMAND_QUIT, "", "");
            default:
                return null;
        }
    }

    private static void handleChatLoop(BlockingQueue<String> inputQueue, AtomicBoolean connectionLost, PrintWriter out) {
        while (true) {
            String line;
            try {
                line = inputQueue.poll(CONNECTION_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                break;
            }

            if (line == null) {
                if (connectionLost.get()) {
                    System.out.println("Forbindelsen til serveren blev afbrudt.");
                    break;
                }
                continue;
            }

            String trimmed = line.trim();
            if (handleLocalCommand(trimmed)) {
                continue;
            }

            String sentCommand = processSlashCommand(trimmed, out);
            if (sentCommand != null) {
                if (isQuitCommand(sentCommand)) {
                    break;
                }
                continue;
            }

            if (processRawProtocolCommand(trimmed, out)) {
                if (isQuitCommand(trimmed)) {
                    break;
                }
                continue;
            }

            String textMessage = MessageParser.formatClientMessage(COMMAND_TEXT, DEFAULT_ROOM, line);
            out.println(textMessage);
            System.out.println("Du: " + line);
        }
    }

    private static boolean handleLocalCommand(String trimmed) {
        if ("/help".equalsIgnoreCase(trimmed)) {
            // Denne kommando er lokal for klienten og skal ikke sendes videre til serveren.
            printCommandHelp();
            return true;
        }
        return false;
    }

    private static String processSlashCommand(String trimmed, PrintWriter out) {
        String slashCommand = parseSlashCommand(trimmed);
        if (slashCommand == null) {
            return null;
        }

        out.println(slashCommand);
        if (slashCommand.startsWith(COMMAND_PRIVATE + "|")) {
            String[] parts = slashCommand.split("\\|", 3);
            if (parts.length >= 3) {
                System.out.println("Du hvisker til " + parts[1] + ": " + parts[2]);
            }
        }
        return slashCommand;
    }

    private static boolean processRawProtocolCommand(String trimmed, PrintWriter out) {
        String upper = trimmed.isEmpty() ? "" : trimmed.split("\\|", 2)[0].toUpperCase();
        boolean isCommand = upper.equals(COMMAND_PRIVATE) || upper.equals(COMMAND_JOIN_ROOM)
                || upper.equals(COMMAND_QUIT) || upper.equals(COMMAND_LOGIN) || upper.equals(COMMAND_TEXT);

        if (isCommand && trimmed.contains("|")) {
            out.println(trimmed);
            return true;
        }
        return false;
    }

    private static boolean isQuitCommand(String protocolLine) {
        return protocolLine != null
                && protocolLine.toUpperCase().startsWith(COMMAND_QUIT + "|");
    }

    private static void stopInputThread(Thread inputThread) {
        inputThread.interrupt();
        try {
            inputThread.join(200);
        } catch (InterruptedException ignored) {
        }
    }
}