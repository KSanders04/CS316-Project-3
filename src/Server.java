import java.io.*;
import java.net.*;

public class Server {
    private static final int PORT = 3000;
    private static final String DIRECTORY = "ServerFiles";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true){
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
                    command = command.toUpperCase();
                    String[] parts = command.split(" ", 2);
                    String operation = parts[0];

                    switch (operation.toUpperCase()) {
                        case "LIST":
                            listFiles(out);
                            break;
                        case "DELETE":
                            if(parts.length >= 2) {
                                deleteFile(out, parts[1]);
                            }
                            break;
                        case "RENAME":
                            if(parts.length >= 2) {
                                renameFile(out, parts[1]);
                            }
                            break;
                        case "DOWNLOAD":
                            if(parts.length >= 2) {
                                sendFile(parts[1], dataOut);
                            }
                            break;
                        case "UPLOAD":
                            if(parts.length >= 2) {
                                receiveFile(parts[1], dataIn, out);
                            } else {
                                out.print("F");
                            }
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
                out.println("File deleted successfully.");
            } else {
                out.println("File deletion failed.");
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

            if (oldFile.exists()) {
                if (oldFile.renameTo(newFile)) {
                    out.println("File renamed successfully");
                } else {
                    out.println("Rename failed");
                }
            } else {
                out.println("File does not exist");
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
            file.delete();
        }

        private void receiveFile(String fileName, DataInputStream dataIn, PrintWriter out) throws IOException {
            File directory = new File(DIRECTORY);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(DIRECTORY, fileName);

            long fileSize = dataIn.readLong();

            if (fileSize < 0) {
                out.println("F");
                return;
            }

            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;

                while (totalBytesRead < fileSize && (bytesRead = dataIn.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                out.println("S");
                System.out.println("File uploaded successfully: " + file.getAbsoluteFile());
            } catch (IOException e) {
                out.println("F");
                e.printStackTrace();
            }
        }
    }
}