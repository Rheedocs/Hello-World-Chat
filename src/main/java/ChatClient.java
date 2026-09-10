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
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5000;
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Ugyldig port, bruger 5000");
            }
        }

        System.out.println("Forbinder til " + host + ":" + port);
        try (Socket socket = new Socket(host, port);
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             Scanner scanner = new Scanner(System.in)) {

            AtomicBoolean connectionLost = new AtomicBoolean(false);
            Thread listener = new Thread(new ServerListener(serverInput, socket, connectionLost));
            listener.start();

            // Separat tråd der læser brugerinput og lægger det i en kø,
            // så hovedloopet ikke blokerer uendeligt og kan tjekke connectionLost.
            BlockingQueue<String> inputQueue = new ArrayBlockingQueue<>(64);
            Thread inputThread = new Thread(() -> {
                try {
                    while (true) {
                        String l = scanner.nextLine();
                        inputQueue.put(l);
                    }
                } catch (InterruptedException e) {
                    // afsluttes normalt
                } catch (java.util.NoSuchElementException e) {
                    // scanner lukket / EOF
                }
            });
            inputThread.setDaemon(true);
            inputThread.start();

            System.out.print("Indtast brugernavn: ");
            String username = null;
            try {
                username = inputQueue.poll(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // ignoreres
            }
            if (username != null) {
                username = username.trim();
            }
            if (username != null && !username.isBlank()) {
                out.println(MessageParser.formatClientMessage("LOGIN", "", username));
            }

            while (true) {
                String line;
                try {
                    line = inputQueue.poll(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    break;
                }

                if (line == null) {
                    // Ingen input indenfor timeout, tjek om forbindelsen er tabt
                    if (connectionLost.get()) {
                        System.out.println("Forbindelsen til serveren blev afbrudt.");
                        break;
                    }
                    continue;
                }

                String trimmed = line.trim();
                String upper = trimmed.isEmpty() ? "" : trimmed.split("\\|", 2)[0].toUpperCase();
                boolean isCommand = upper.equals("PRIVATE") || upper.equals("JOIN_ROOM")
                        || upper.equals("QUIT") || upper.equals("LOGIN") || upper.equals("TEXT");

                if (isCommand && trimmed.contains("|")) {
                    // Send den rå kommandolinje, serveren parser TYPE|TARGET|PAYLOAD
                    out.println(trimmed);
                    if (upper.equals("QUIT")) {
                        // Ingen synkron læsning her, ServerListener printer bekræftelsen
                        break;
                    }
                    continue;
                }

                // Almindelig tekst uden prefix sendes som TEXT til nuværende rum
                out.println(MessageParser.formatClientMessage("TEXT", "all", line));
            }

            inputThread.interrupt();
            try {
                inputThread.join(200);
            } catch (InterruptedException ignored) {
            }

        } catch (IOException e) {
            System.err.println("Clientfejl: " + e.getMessage());
        }

        System.out.println("Client lukker.");
    }
}