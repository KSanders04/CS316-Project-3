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

            while (true) {
                System.out.print("\nPlease choose an option:\n" +
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
                        while (!(response = in.readLine()).equals("END")) {
                            System.out.println(response);
                        }
                        break;
                    case "DELETE":
                        deleteFile(out, in);
                        break;
                    case "RENAME":
                        renameFile(out, in);
                        break;
                    case "DOWNLOAD":
                        receiveFile(out, dataIn);
                        break;
                    case "UPLOAD":
                        System.out.println("Server: " + in.readLine());
                        sendFile(out, dataOut);
                        break;
                    default:
                        System.out.println("Invalid command");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteFile(PrintWriter out, BufferedReader in) throws IOException {
        System.out.print("Enter file to delete: ");
        Scanner scanner = new Scanner(System.in);
        String fileToDelete = scanner.nextLine().trim(); // Remove extra spaces

        if (fileToDelete.isEmpty()) {
            System.out.println("Invalid input. Please enter a valid file name.");
            return; // Early return if the file name is empty
        }

        System.out.println("Sending delete request for: " + fileToDelete); // Debugging line
        out.println("DELETE " + fileToDelete);

        String deleteResponse = in.readLine(); // Read server's response
        System.out.println("Server response: " + deleteResponse); // Debugging line

        if ("File deleted successfully.".equals(deleteResponse)) {
            System.out.println("File deleted successfully.\n");
        } else if ("File deletion failed.".equals(deleteResponse)) {
            System.out.println("File deletion failed.\n");
        } else {
            System.out.println("Unexpected server response: " + deleteResponse + "\n");
        }
    }

    private static void renameFile(PrintWriter out, BufferedReader in) throws IOException {
        System.out.print("Enter old file name: ");
        Scanner scanner = new Scanner(System.in);
        String fileToRename = scanner.nextLine().trim(); // Remove extra spaces
        if (fileToRename.isEmpty()) {
            System.out.println("Invalid input. Please enter a valid file name.");
            return; // Early return if the file name is empty
        }
        System.out.print("Enter new name for file: ");
        String renamedFile = scanner.nextLine().trim();

        System.out.println("Sending rename request for: " + fileToRename); // Debugging line
        out.println("RENAME " + fileToRename + " " + renamedFile);

        String renameResponse = in.readLine(); // Read server's response
        System.out.println("Server response: " + renameResponse); // Debugging line
    }



    private static void receiveFile(PrintWriter out, DataInputStream dataIn) throws IOException {
        System.out.print("Enter a file to download: ");
        Scanner scanner = new Scanner(System.in);
        String fileToDown = scanner.nextLine().trim(); // Remove extra spaces

        out.println("DOWNLOAD " + fileToDown);

        long fileSize = dataIn.readLong();
        if (fileSize == -1) {
            System.out.println("File not found on server.");
            return;
        }

        File file = new File("ClientFiles", fileToDown);
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            long bytesReceived = 0;
            int bytesRead;

            while (bytesReceived < fileSize && (bytesRead = dataIn.read(buffer, 0, Math.min
                    (buffer.length, (int) (fileSize - bytesReceived)))) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                bytesReceived += bytesRead;
            }
        }
        System.out.println("File downloaded successfully.");
    }

    private static void sendFile(PrintWriter out, DataOutputStream dataOut) throws IOException {
        System.out.print("Enter a file to upload: ");
        Scanner scanner = new Scanner(System.in);
        String fileToUp = scanner.nextLine().trim(); // Remove extra spaces

        out.println("UPLOAD " + fileToUp);


        File file = new File(fileToUp);
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
        System.out.println("File uploaded successfully.");
    }


}