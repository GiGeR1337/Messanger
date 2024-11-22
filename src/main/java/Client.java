import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private Socket socket;
    private final String serverAddress;
    private final int serverPort;
    private BufferedReader in;
    private PrintWriter out;
    private final boolean isNicknameSet = false;

    public Client() {
        this.serverAddress = "127.0.0.1";
        this.serverPort = 12345;
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.initialize();
        client.start();
    }

    private void initialize() {
        //main frame
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 500);
        frame.setLayout(new BorderLayout());

        //center panel
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        //bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setEnabled(false);
        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(new SendListener());
        inputField.addActionListener(e -> {
            sendButton.doClick();
        });

        frame.setVisible(true);
    }

    private void start() {
        try {
            socket = new Socket(serverAddress, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            inputField.setEnabled(true);
            sendButton.setEnabled(true);

            //start message reader thread
            new Thread(new MessageReader()).start();

            appendMessage("Connected to the server. Please enter your name:");

        } catch (IOException e) {
            appendMessage("Failed to connect: " + e.getMessage());
        }
    }

    private void sendMessage(String message) {
        if (out != null && !message.isEmpty()) {
            out.println(message);
            inputField.setText("");
        }
    }

    private void appendMessage(String message) {
        messageArea.append(message + "\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    private class SendListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String message = inputField.getText().trim();
            sendMessage(message);
        }
    }

    private class MessageReader implements Runnable {
        @Override
        public void run() {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    appendMessage(response);
                }
            } catch (IOException e) {
                appendMessage("Connection lost: " + e.getMessage());
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                        frame.dispose();
                    }
                } catch (IOException e) {
                    appendMessage("Failed to close socket: " + e.getMessage());
                }
                appendMessage("Disconnected from server.");
            }
        }
    }
}