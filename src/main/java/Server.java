import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private final Map<String, ClientThread> clients = new HashMap<>();
    private int port;
    private String serverName;
    private Set<String> bannedPhrases;

    public Server(String configFilePath) throws IOException {
        loadConfig(configFilePath);
    }

    public static void main(String[] args) {
        String configFilePath = "serverConfig.txt";
        try {
            Server server = new Server(configFilePath);
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    private void loadConfig(String configFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    switch (key) {
                        case "port":
                            port = Integer.parseInt(value);
                            break;
                        case "serverName":
                            serverName = value;
                            break;
                        case "bannedPhrases":
                            bannedPhrases = new HashSet<>(Arrays.asList(value.split(",")));
                            break;
                        default:
                            System.err.println("Unknown config key: " + key);
                    }
                }
            }
        }
    }

    public void start() {
        System.out.println(serverName + " is running on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientThread(clientSocket, this).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public void broadcast(String message, ClientThread sender, Set<String> recipients) {
        synchronized (clients) {
            for (Map.Entry<String, ClientThread> entry : clients.entrySet()) {
                String recipientName = entry.getKey();

                if (recipients.contains("*")) {
                    if (recipientName.equals(sender.getClientName())) {
                        sender.sendMessage("[Me]: To: [Everyone] >> " + message);
                    } else {
                        entry.getValue().sendMessage("From: [" + sender.getClientName() + "] To: [Everyone] >> " + message);
                    }
                } else if (recipients.contains(recipientName)) {
                    if (recipientName.equals(sender.getClientName())) {
                        sender.sendMessage("[Me]: To: " + recipients + " >> " + message);
                    } else {
                        entry.getValue().sendMessage("From: [" + sender.getClientName() + "] To: " + recipients + " >> " + message);
                    }
                }
            }
        }
    }



    public void broadcastExcept(String message, ClientThread sender, Set<String> exceptions) {
        synchronized (clients) {
            for (Map.Entry<String, ClientThread> entry : clients.entrySet()) {
                String recipientName = entry.getKey();

                if (!exceptions.contains(recipientName)) {
                    if (recipientName.equals(sender.getClientName())) {
                        sender.sendMessage("[Me]: To: [Everyone] except: " + exceptions + " >> " + message);
                    } else {
                        entry.getValue().sendMessage("From: [" + sender.getClientName() + "] To: [Everyone] except: " + exceptions + " >> " + message);
                    }
                }
            }
        }
    }



    public boolean checkForBanned(String message) {
        for (String phrase : bannedPhrases) {
            if (message.contains(phrase))
                return false;
        }
        return true;
    }

    public void clientJoin(String name, ClientThread handler) {
        synchronized (clients) {
            clients.put(name, handler);
        }
        clientNotify(name + " has joined the chat.");
    }

    public void clientQuit(String name) {
        synchronized (clients) {
            clients.remove(name);
        }
        clientNotify(name + " has left the chat.");
    }

    public void clientNotify(String notification) {
        synchronized (clients) {
            for (ClientThread handler : clients.values())
                handler.sendMessage(notification);
        }
    }

    public Set<String> getBannedPhrases() {
        return bannedPhrases;
    }

    public Set<String> getClientNames() {
        synchronized (clients) {
            return new HashSet<>(clients.keySet());
        }
    }
}
