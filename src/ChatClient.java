import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5001;
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

            Thread listener = new Thread(new ServerListener(serverInput));
            listener.setDaemon(true);
            listener.start();

            System.out.print("Indtast brugernavn: ");
            String username = scanner.nextLine().trim();
            if (!username.isBlank()) {
               out.println(MessageParser.formatClientMessage("LOGIN", "", username));
            }

            while (true) {
               String line = scanner.nextLine();
               if (line == null) {
                   break;
               }

               if (line.equalsIgnoreCase("/quit")) {
                   out.println(MessageParser.formatClientMessage("QUIT", "", ""));
                   break;
               }

               out.println(MessageParser.formatClientMessage("TEXT", "all", line));
            }

        } catch (IOException e) {
            System.err.println("Clientfejl: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Client lukker.");
    }
}