import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 3000;
    private static final String DIRECTORY = "ServerFiles";
    private static volatile boolean isRunning = true; // Flag for server status
    private static ExecutorService threadPool = Executors.newCachedThreadPool(); // Thread pool
    private static ServerSocket serverSocket; // Server socket

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);

            // Start the thread that accepts clients
            Thread acceptThread = new Thread(Server::acceptClients);
            acceptThread.start();

            // Start the thread that listens for admin input
            Thread adminThread = new Thread(Server::monitorAdminInput);
            adminThread.start();

            acceptThread.join(); // Wait for accept thread to finish before exiting

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            shutdownServer();
        }
    }

    private static void acceptClients() {
        System.out.println("Server started. Type 'q' to shut down.");
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                if (!isRunning) { // Prevent accepting new clients after shutdown
                    clientSocket.close();
                    break;
                }
                new ClientHandler(clientSocket).start();
            } catch (IOException e) {
                if (!isRunning) {
                    System.out.println("Server socket closed, stopping accept loop.");
                } else {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void monitorAdminInput() {
        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            while (isRunning) {
                String command = consoleReader.readLine();
                if ("q".equalsIgnoreCase(command)) {
                    System.out.println("Shutting down server...");
                    isRunning = false;
                    shutdownServer();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static volatile boolean shuttingDown = false; // Prevent multiple shutdowns

    private static void shutdownServer() {
        if (shuttingDown) return; // If already shutting down, don't proceed
        shuttingDown = true;

        try {
            isRunning = false;

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Close server socket to break accept()
            }

            threadPool.shutdownNow(); // Stop all running tasks
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("Forcing shutdown...");
                threadPool.shutdownNow();
            }

            System.out.println("Server has been fully shut down.");
        } catch (IOException | InterruptedException e) {
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

                    switch (operation) {
                        case "LIST":
                            listFiles(out);
                            break;
                        case "DELETE":
                            if (parts.length >= 2) {
                                deleteFile(out, parts[1]);
                            }
                            break;
                        case "RENAME":
                            if (parts.length >= 2) {
                                renameFile(out, parts[1]);
                            }
                            break;
                        case "DOWNLOAD":
                            if (parts.length >= 2) {
                                threadPool.execute(() -> sendFile(parts[1], dataOut));
                            }
                            break;
                        case "UPLOAD":
                            if (parts.length >= 2) {
                                threadPool.execute(() -> receiveFile(parts[1], dataIn, out));
                            } else {
                                out.println("F");
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

        private void sendFile(String fileName, DataOutputStream dataOut) {
            File file = new File(DIRECTORY, fileName);
            try {
                if (!file.exists()) {
                    dataOut.writeLong(-1);
                    return;
                }

                dataOut.writeLong(file.length()); // Send file size first

                try (FileInputStream fileIn = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        dataOut.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void receiveFile(String fileName, DataInputStream dataIn, PrintWriter out) {
            File directory = new File(DIRECTORY);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(DIRECTORY, fileName);
            try {
                long fileSize = dataIn.readLong(); // Read file size
                if (fileSize < 0) {
                    out.println("F");
                    return;
                }

                try (FileOutputStream fileOut = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytesRead = 0;

                    while (totalBytesRead < fileSize &&
                            (bytesRead = dataIn.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                        fileOut.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }
                }

                out.println("S"); // Success response
                System.out.println("File uploaded successfully: " + file.getAbsolutePath());
            } catch (IOException e) {
                out.println("F");
                e.printStackTrace();
            }
        }
    }
}
