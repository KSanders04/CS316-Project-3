import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Client <server_ip> <port>");
            return;
        }

        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(serverIp, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             DataInputStream dataIn = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server at " + serverIp + ":" + serverPort);

            while (true) {
                System.out.print("Please choose an option:\n" +
                        "1. List files\n" +
                        "2. Delete a file\n" +
                        "3. Rename a file\n" +
                        "4. Download a file from server\n" +
                        "5. Upload a file to server\n" +
                        ">> ");
                String command = scanner.nextLine();
                if (command.equalsIgnoreCase("EXIT")) break;

                String[] parts = command.split(" ", 2);
                String operation = parts[0].toUpperCase();

                out.println(command);

                switch (operation) {
                    case "LIST":
                        String response;
                        while (!(response = in.readLine()).equals("S END")) {
                            System.out.println(response);
                        }
                        break;
                    case "DELETE":
                    case "RENAME":
                    case "DOWNLOAD":
                        receiveFile(parts[1], dataIn);
                        break;
                    case "UPLOAD":
                        System.out.println("Server: " + in.readLine());
                        sendFile(parts[1], dataOut);
                        break;
                    default:
                        System.out.println("Invalid command");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(String fileName, DataInputStream dataIn) throws IOException {
        long fileSize = dataIn.readLong();
        if (fileSize == -1) {
            System.out.println("File not found on server.");
            return;
        }

        File file = new File("client_files", fileName);
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dataIn.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("File downloaded successfully.");
    }

    private static void sendFile(String fileName, DataOutputStream dataOut) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("File not found.");
            return;
        }

        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                dataOut.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("File uploaded successfully."); //comment
    }
}
