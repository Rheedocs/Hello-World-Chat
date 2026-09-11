import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            port = parsePort(args[1]);
        }

        System.out.println("Forbinder til " + host + ":" + port);
        try (Socket socket = new Socket(host, port);
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             Scanner scanner = new Scanner(System.in)) {

            AtomicBoolean connectionLost = new AtomicBoolean(false);
            BlockingQueue<String> serverMessages = new ArrayBlockingQueue<>(64);
            AtomicBoolean loginPhase = new AtomicBoolean(true);
            Thread listener = new Thread(new ServerListener(serverInput, socket, connectionLost, serverMessages, loginPhase));
            listener.start();

            System.out.println("Kommandoer: /w <bruger> <besked>, /join <rum>, /quit. Skriv bare almindelig tekst for at chatte i dit nuværende rum.");

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
                String username = readUsername(inputQueue);
                if (username == null) {
                    break;
                }
                username = username.trim();
                if (username.isBlank()) {
                    continue;
                }

                out.println(MessageParser.formatClientMessage(COMMAND_LOGIN, "", username));
                loggedIn = waitForLoginResult(serverMessages, loginPhase);
                if (!loggedIn) {
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

    private static int parsePort(String rawPort) {
        try {
            return Integer.parseInt(rawPort);
        } catch (NumberFormatException e) {
            System.err.println("Ugyldig port, bruger " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }

    private static String readUsername(BlockingQueue<String> inputQueue) {
        System.out.print("Indtast brugernavn: ");
        String username = null;
        try {
            username = inputQueue.take();
        } catch (InterruptedException e) {
            // ignoreres
        }
        if (username != null) {
            username = username.trim();
        }
        return username;
    }

    private static boolean waitForLoginResult(BlockingQueue<String> serverMessages, AtomicBoolean loginPhase) {
        while (true) {
            try {
                String serverMessage = serverMessages.take();
                String[] parts = serverMessage.split("\\|", 5);
                if (parts.length < 2) {
                    continue;
                }
                String status = parts[1].toUpperCase();
                if (MESSAGE_TYPE_OK.equals(status)) {
                    loginPhase.set(false);
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
            String slashCommand = parseSlashCommand(trimmed);
            if (slashCommand != null) {
                out.println(slashCommand);
                if (slashCommand.startsWith(COMMAND_PRIVATE + "|")) {
                    String[] parts = slashCommand.split("\\|", 3);
                    if (parts.length >= 3) {
                        System.out.println("Du hvisker til " + parts[1] + ": " + parts[2]);
                    }
                }
                if (slashCommand.startsWith(COMMAND_QUIT + "|")) {
                    break;
                }
                continue;
            }

            String upper = trimmed.isEmpty() ? "" : trimmed.split("\\|", 2)[0].toUpperCase();
            boolean isCommand = upper.equals(COMMAND_PRIVATE) || upper.equals(COMMAND_JOIN_ROOM)
                    || upper.equals(COMMAND_QUIT) || upper.equals(COMMAND_LOGIN) || upper.equals(COMMAND_TEXT);

            if (isCommand && trimmed.contains("|")) {
                out.println(trimmed);
                if (upper.equals(COMMAND_QUIT)) {
                    break;
                }
                continue;
            }

            String textMessage = MessageParser.formatClientMessage(COMMAND_TEXT, DEFAULT_ROOM, line);
            out.println(textMessage);
            System.out.println("Du: " + line);
        }
    }

    private static void stopInputThread(Thread inputThread) {
        inputThread.interrupt();
        try {
            inputThread.join(200);
        } catch (InterruptedException ignored) {
        }
    }
}