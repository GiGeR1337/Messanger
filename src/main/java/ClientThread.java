import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClientThread extends Thread {
    private final Socket socket;
    private final Server server;
    private String clientName;
    private BufferedReader in;
    private PrintWriter out;

    public ClientThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Enter your username:");

            while (true) {
                clientName = in.readLine();
                if (clientName == null || clientName.isEmpty())
                    continue;

                synchronized (server.getClientNames()) {
                    if (!clientName.contains(" ")) {
                        if (!server.getClientNames().contains(clientName)) {
                            server.clientJoin(clientName, this);
                            break;
                        } else
                            out.println("Invalid name, try another one: ");
                    } else
                        out.println("Name cannot contain spaces, try another one: ");
                }
            }

            out.println("All clients connected right now: " + server.getClientNames());
            out.println("====================================================");
            out.println("To send a message use: /s <recipient(s)> <message>");
            out.println("Use: /c for listing all commands");
            out.println("====================================================");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/b")) {
                    out.println("====================================================");
                    out.println("Banned phrases: " + server.getBannedPhrases());
                    out.println("====================================================");
                } else if (message.startsWith("/q")) {
                    out.println("Bye!");
                    break;
                } else if (message.startsWith("/u")) {
                    out.println("====================================================");
                    out.println("List of clients: " + server.getClientNames());
                    out.println("====================================================");
                } else if (message.startsWith("/c")) {
                    out.println("====================================================");
                    out.println("Commands: \n" +
                            "/b - Banned phrases\n" +
                            "/s - Send <recipient(s)> <message>\n" +
                            "/e - Exclude <exception(s)> <message>\n" +
                            "/q - Quit from the server\n" +
                            "/c - List of commands\n" +
                            "/u - List of clients");
                    out.println("====================================================");
                } else if (message.startsWith("/s")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length < 3) {
                        out.println("Invalid command. Usage: /s <recipient(s)> <message>");
                    } else {
                        String recipientsRaw = parts[1];
                        String content = parts[2];
                        Set<String> recipients = new HashSet<>();
                        if (recipientsRaw.equals("*")) {
                            recipients.add("*");
                        } else {
                            recipients.addAll(Arrays.asList(recipientsRaw.split(",")));
                        }

                        if (server.checkForBanned(content)) {
                            server.broadcast(content, this, recipients);
                        } else {
                            out.println("Message contains banned phrases and was not sent.");
                            out.println("Banned phrases: " + server.getBannedPhrases());
                        }
                    }
                } else if (message.startsWith("/e")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length < 3) {
                        out.println("Invalid command. Usage: /e <exception(s)> <message>");
                    } else {
                        String exceptionsRaw = parts[1];
                        String content = parts[2];
                        Set<String> exceptions = new HashSet<>(Arrays.asList(exceptionsRaw.split(",")));

                        if (server.checkForBanned(content)) {
                            server.broadcastExcept(content, this, exceptions);
                        } else {
                            out.println("Message contains banned phrases and was not sent.");
                            out.println("Banned phrases: " + server.getBannedPhrases());
                        }
                    }
                } else
                    out.println("Unknown command.");
            }
        } catch (IOException e) {
            System.err.println("Error with client: " + clientName + ": " + e.getMessage());
        } finally {
            server.clientQuit(clientName);
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Failed to close client socket: " + e.getMessage());
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getClientName() {
        return clientName;
    }
}