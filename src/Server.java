import java.io.*;
import java.net.*;

public class Server {
    private static final int PORT = 3000;
    private static final String DIRECTORY = "ServerFiles";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dataIn = new DataInputStream(socket.getInputStream())) {

                String command;
                while ((command = in.readLine()) != null) {
                    String[] parts = command.split(" ", 2);
                    String operation = parts[0];

                    switch (operation.toUpperCase()) {
                        case "LIST":
                            listFiles(out);
                            break;
                        case "DELETE":
                            deleteFile(out, parts[1]);
                            break;
                        case "RENAME":
                            renameFile(out, parts[1]);
                            break;
                        case "DOWNLOAD":
                            sendFile(parts[1], dataOut);
                            break;
                        case "UPLOAD":
                            receiveFile(parts[1], dataIn);
                            out.println("Upload complete");
                            break;
                        default:
                            out.println("Invalid command");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void listFiles(PrintWriter out) {
            File dir = new File(DIRECTORY);
            if (!dir.exists() || !dir.isDirectory()) {
                out.println("No files found");
                return;
            }
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    out.println(file.getName());
                }
            }
            out.println("END");
        }

        private void deleteFile(PrintWriter out, String fileName) {


            File file = new File(DIRECTORY, fileName);
            if (file.exists() && file.delete()) {
                out.println("File deleted successfully");
            } else {
                out.println("File not found or cannot be deleted");
            }
        }

        private void renameFile(PrintWriter out, String params) {
            String[] names = params.split(" ");
            if (names.length < 2) {
                out.println("Invalid rename command");
                return;
            }
            File oldFile = new File(DIRECTORY, names[0]);
            File newFile = new File(DIRECTORY, names[1]);
            if (oldFile.exists() && oldFile.renameTo(newFile)) {
                out.println("File renamed successfully");
            } else {
                out.println("Rename failed");
            }
        }

        private void sendFile(String fileName, DataOutputStream dataOut) throws IOException {
            File file = new File(DIRECTORY, fileName);
            if (!file.exists()) {
                dataOut.writeLong(-1);
                return;
            }
            dataOut.writeLong(file.length());
            try (FileInputStream fileIn = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
            }
        }

        private void receiveFile(String fileName, DataInputStream dataIn) throws IOException {
            File file = new File(DIRECTORY, fileName);
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = dataIn.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
