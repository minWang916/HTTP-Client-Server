import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class HttpClient {
    private JFrame frame;
    private JTextField urlField;
    private JTextArea responseArea;
    private JLabel imageLabel;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private InputStream socketIn;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                HttpClient window = new HttpClient();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public HttpClient() {
        initialize();
        setupConnection();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 450, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.NORTH);

        JLabel lblUrl = new JLabel("URL:");
        panel.add(lblUrl);

        urlField = new JTextField();
        panel.add(urlField);
        urlField.setColumns(25);

        JButton btnRequest = new JButton("Send Request");
        btnRequest.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendRequest();
            }
        });
        panel.add(btnRequest);

        responseArea = new JTextArea();
        responseArea.setEditable(false);

        JScrollPane textScrollPane = new JScrollPane(responseArea);
        frame.getContentPane().add(textScrollPane, BorderLayout.CENTER);

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        frame.getContentPane().add(imageLabel, BorderLayout.SOUTH);
    }

    private void setupConnection() {
        try {
            socket = new Socket("localhost", 8080);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            socketIn = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendRequest() {
        String urlString = urlField.getText();
        responseArea.setText("");
        imageLabel.setIcon(null);
    
        try {
            URL url = new URL(urlString);
            out.println("GET " + url.getPath() + " HTTP/1.1");
            out.println("Host: " + url.getHost());
            out.println("Connection: close");
            out.println();
    
            String inputLine;
            boolean isImage = false;
            String contentType = "";
            int contentLength = 0;
    
            while (!(inputLine = in.readLine()).isEmpty()) {
                responseArea.append(inputLine + "\n");
                if (inputLine.startsWith("Content-Type: ")) {
                    contentType = inputLine.split(" ")[1];
                    if (contentType.contains("image/png") || contentType.contains("image/jpeg")) {
                        isImage = true;
                    }
                }
                if (inputLine.startsWith("Content-Length: ")) {
                    contentLength = Integer.parseInt(inputLine.split(" ")[1]);
                }
            }
    
            if (isImage && contentLength > 0) {
                byte[] imageBytes = new byte[contentLength];
                int bytesRead = 0;
                while (bytesRead < contentLength) {
                    int result = socketIn.read(imageBytes, bytesRead, contentLength - bytesRead);
                    if (result == -1) break;
                    bytesRead += result;
                }
    
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                Image scaledImage = image.getScaledInstance(frame.getWidth(), -1, Image.SCALE_SMOOTH);
                ImageIcon imageIcon = new ImageIcon(scaledImage);
                imageLabel.setIcon(imageIcon);
            } else {
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine).append("\n");
                }
                responseArea.append(response.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
