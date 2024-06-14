import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private static final int PORT = 8080;
    private static final String ROOT_DIRECTORY = System.getProperty("user.dir");

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 OutputStream out = clientSocket.getOutputStream()) {

                String requestLine = in.readLine();
                System.out.println("Received request: " + requestLine);

                if (requestLine.startsWith("GET")) {
                    String[] tokens = requestLine.split(" ");
                    String fileName = tokens[1].substring(1);
                    File file = new File(ROOT_DIRECTORY, fileName);
                    System.out.println("Looking for file: " + file.getPath());

                    if (file.exists() && !file.isDirectory()) {
                        String contentType = getContentType(fileName);

                        out.write(("HTTP/1.1 200 OK\r\n").getBytes());
                        out.write(("Content-Type: " + contentType + "\r\n").getBytes());
                        out.write(("Content-Length: " + file.length() + "\r\n").getBytes());
                        out.write(("\r\n").getBytes());
                        out.flush();

                        try (FileInputStream fileIn = new FileInputStream(file)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = fileIn.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                            out.flush();
                        }
                    } else {
                        out.write(("HTTP/1.1 404 Not Found\r\n").getBytes());
                        out.write(("Content-Type: text/plain\r\n").getBytes());
                        out.write(("\r\n").getBytes());
                        out.write(("404 File Not Found\r\n").getBytes());
                        out.flush();
                    }
                }

                while (in.ready()) {
                    in.readLine();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String getContentType(String fileName) {
            if (fileName.endsWith(".png")) {
                return "image/png";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (fileName.endsWith(".txt")) {
                return "text/plain";
            } else {
                return "application/octet-stream";
            }
        }
    }
}
