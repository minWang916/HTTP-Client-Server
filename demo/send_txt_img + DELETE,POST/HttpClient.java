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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.swing.*;

public class HttpClient {
    private JFrame frame;
    private JTextField urlField;
    private JTextArea responseArea;
    private JLabel imageLabel;
    private ExecutorService threadPool;
    private JComboBox<String> methodComboBox;
    private JTextArea requestBodyArea;

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
        threadPool = Executors.newFixedThreadPool(10);
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 950, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        frame.getContentPane().add(panel, BorderLayout.NORTH);

        JLabel lblUrl = new JLabel("URL:");
        panel.add(lblUrl);

        urlField = new JTextField();
        panel.add(urlField);
        urlField.setColumns(25);

        methodComboBox = new JComboBox<>(new String[]{"GET", "POST", "DELETE"});
        panel.add(methodComboBox);

        JButton btnRequest = new JButton("Send Request");
        btnRequest.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                threadPool.submit(() -> sendRequest(urlField.getText(), methodComboBox.getSelectedItem().toString()));
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

        requestBodyArea = new JTextArea(5, 50);
        requestBodyArea.setBorder(BorderFactory.createTitledBorder("Request Body"));
        JScrollPane requestBodyScrollPane = new JScrollPane(requestBodyArea);
        frame.getContentPane().add(requestBodyScrollPane, BorderLayout.EAST);
    }

    private void sendRequest(String urlString, String method) {
        responseArea.setText("");
        imageLabel.setIcon(null);

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);

            if ("POST".equals(method)) {
                connection.setDoOutput(true);
                try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream())) {
                    out.write(requestBodyArea.getText());
                }
            }

            int responseCode = connection.getResponseCode();
            responseArea.append("Response Code: " + responseCode + "\n");

            String contentType = connection.getContentType();
            int contentLength = connection.getContentLength();

            if (contentType != null && (contentType.contains("image/png") || contentType.contains("image/jpeg")) && contentLength > 0) {
                byte[] imageBytes = new byte[contentLength];
                try (InputStream in = connection.getInputStream()) {
                    in.read(imageBytes);
                }

                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                Image scaledImage = image.getScaledInstance(frame.getWidth(), -1, Image.SCALE_SMOOTH);
                ImageIcon imageIcon = new ImageIcon(scaledImage);
                imageLabel.setIcon(imageIcon);
            } else {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine).append("\n");
                    }
                    responseArea.append(response.toString());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            responseArea.append("Error: " + e.getMessage());
        }
    }
}
