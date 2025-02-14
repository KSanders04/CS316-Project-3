import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class Server {
    public static void main(String[] args) throws Exception {
        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(3000));

        //Caught Exception
        while(true) {
            //Accept the client side command call
            SocketChannel serverChannel = listenChannel.accept();
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            int bytesRead = serverChannel.read(buffer);
            buffer.flip();

            //Processes Request and returns desired request name
            byte[] requestBytes = new byte[bytesRead];
            buffer.get(requestBytes);
            String request = new String(requestBytes);
            System.out.println("Request received: " + request);

            //Formatting request for readability and determines command
            String[] parts = request.split("\\|");
            String command = parts[0]; //Reads the first byte of command, as instructed

            ByteBuffer responseBuffer;
            switch (command) {
                case "D": //Deletes files from local server
                    if(parts.length < 2) break;
                case "L": //List the file names that exist; Only command that only needs 1 input
                case "U": //Upload a file to the server
                    if(parts.length < 2) break;
                case "S": //Downloads a file from the server
                    if(parts.length < 2) break;
                case "R": //Renames a file from the server if it exists
                    if(parts.length < 2) break;
                default: //Default "if not any above command, return else" case
                    System.out.println("Invalid request.");
                    break;
            }
            serverChannel.close(); //Closes the serverChannel after determining case and following operations
        }
    }
}
