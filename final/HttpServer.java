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
    
                if (requestLine != null && !requestLine.isEmpty()) {
                    String[] tokens = requestLine.split(" ");
                    String method = tokens[0];
                    String fileName = tokens[1].substring(1);
    
                    if ("GET".equals(method)) {
                        handleGetRequest(out, fileName);
                    } else if ("DELETE".equals(method)) {
                        handleDeleteRequest(out, fileName);
                    } else if ("POST".equals(method)) {
                        handlePostRequest(in, out, fileName);
                    } else {
                        sendResponse(out, "HTTP/1.1 405 Method Not Allowed\r\n", "text/plain", "405 Method Not Allowed\r\n");
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
    
        private void handleGetRequest(OutputStream out, String fileName) throws IOException {
            File file = new File(ROOT_DIRECTORY, fileName);
            if (file.exists() && !file.isDirectory()) {
                String contentType = getContentType(fileName);
                sendResponse(out, "HTTP/1.1 200 OK\r\n", contentType, file);
            } else {
                sendResponse(out, "HTTP/1.1 404 Not Found\r\n", "None", "");
            }
        }
    
        private void handleDeleteRequest(OutputStream out, String fileName) throws IOException {
            File file = new File(ROOT_DIRECTORY, fileName);
            if (file.exists() && file.delete()) {
                sendResponse(out, "HTTP/1.1 200 OK\r\n", "text/plain", "File deleted successfully\r\n");
            } else {
                sendResponse(out, "HTTP/1.1 404 Not Found\r\n", "None", "");
            }
        }
    
        private void handlePostRequest(BufferedReader in, OutputStream out, String fileName) throws IOException {
            StringBuilder body = new StringBuilder();
            String line;
            int contentLength = 0;
            
            while (!(line = in.readLine()).isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.split(" ")[1]);
                }
            }
            
            char[] bodyChars = new char[contentLength];
            in.read(bodyChars, 0, contentLength);
            body.append(bodyChars);
            
            File file = new File(ROOT_DIRECTORY, fileName);
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(body.toString());
            }
    
            sendResponse(out, "HTTP/1.1 200 OK\r\n", "text/plain", "File created/updated successfully\r\n");
        }
    
        private void sendResponse(OutputStream out, String status, String contentType, String responseBody) throws IOException {
            if (responseBody.isEmpty()) {
                out.write(status.getBytes());
                out.write(("Content-Type: None\r\n").getBytes());
                out.write("Content-Length: 0\r\n".getBytes());
                out.write("\r\n".getBytes());
            } else {
                out.write(status.getBytes());
                out.write(("Content-Type: " + contentType + "\r\n").getBytes());
                out.write(("Content-Length: " + responseBody.length() + "\r\n").getBytes());
                out.write("\r\n".getBytes());
                out.write(responseBody.getBytes());
            }
            out.flush();
        }
    
        private void sendResponse(OutputStream out, String status, String contentType, File file) throws IOException {
            out.write(status.getBytes());
            out.write(("Content-Type: " + contentType + "\r\n").getBytes());
            out.write(("Content-Length: " + file.length() + "\r\n").getBytes());
            out.write("\r\n".getBytes());
            out.flush();
    
            try (FileInputStream fileIn = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
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
