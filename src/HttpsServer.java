import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpsServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        while (true) {
            Socket socket = serverSocket.accept();
            InputStream inputStream = socket.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = bufferedReader.readLine();
            System.out.println(line);
            String[] tokens = line.split(" ");
      System.out.println(tokens[0]);
      System.out.println(tokens[1]);
            String method = tokens[0];
            String path = tokens[1];
            if (method.equals("GET")) {
                if (path.equals("/")) {
                    path = "/index.html";
                }
                File file = new File("web" + path);
                if (file.exists()) {
                    FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
                    int size = fileInputStream.available();
                    byte[] bytes = new byte[size];
                    fileInputStream.read(bytes);

                    socket.getOutputStream().write(bytes);
                } else {
                    socket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n".getBytes());
                    socket.getOutputStream().write("Content-Type: text/html\r\n".getBytes());
                    socket.getOutputStream().write("\r\n".getBytes());
                    socket.getOutputStream().write("<html><body><h1>404 Not Found</h1></body></html>".getBytes());

                }
            } else if (method.equals("POST")) {
                StringBuilder stringBuilder = new StringBuilder();
                while (true) {
                    line = bufferedReader.readLine();
                    if (line.isEmpty()) {
                        break;
                    }
                    stringBuilder.append(line);
                }
                String body = stringBuilder.toString();
                System.out.println(body);
                socket.getOutputStream().write("HTTP/1.1 200 OK\r\n".getBytes());
                socket.getOutputStream().write("Content-Type: text/html\r\n".getBytes());
                socket.getOutputStream().write("\r\n".getBytes());
                socket.getOutputStream().write("<html><body><h1>200 OK</h1></body></html>".getBytes());
            }
            socket.close();
        }
    }
}
