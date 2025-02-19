import java.io.File;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static java.lang.System.out;

public class Server {
    private static final String directory = "ServerFiles";

    public static void main(String[] args) throws Exception {
        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(3000));

        while(true) {
            SocketChannel serverChannel = listenChannel.accept();
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            int bytesRead = serverChannel.read(buffer);
            buffer.flip();

            byte[] requestBytes = new byte[bytesRead];
            buffer.get(requestBytes);
            String request = new String(requestBytes);
            out.println("Request received: " + request);

            String[] parts = request.split("\\|");
            String command = parts[0];

            ByteBuffer responseBuffer;
            switch (command) {
                case "L":
                    if (command == "L"){

                    }
                case "D":
                case "R":
                case "S":
                case "U":
                default:
                    out.println("Invalid request.");
                    break;
            }
            serverChannel.close();
        }
    }

    private void listFiles(PrintWriter out){
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()){
            System.out.println("s No files found");
            return;
        }
        File[] files = dir.listFiles();
        if (files != null){
            for(File file : files){
                System.out.println(file.getName());
            }
        }
    }

    private void deleteFile(PrintWriter out, String fileName){
        File file = new File(directory, fileName);
        if (file.exists() && file.delete()){
            out.println("File deleted successfully");
        }else{
            out.println("File not found");
        }
    }
    private void renameFile(PrintWriter out, String params){
        String[] names = params.split(" ");
        if (names.length < 2){
            out.println("Invalid rename command");
            return;
        }
    }
}
