import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
public class Client {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Please provide <serverIP> and <serverPort>");
            return;
        }
        int serverPort = Integer.parseInt(args[1]);
        Scanner keyboard = new Scanner(System.in);
        String filename = keyboard.nextLine();
        SocketChannel channel = SocketChannel.open();
        channel.connect(
                new InetSocketAddress(args[0], serverPort)
        );

        while (true){
            Scanner scan = new Scanner(System.in);
            System.out.println("Please choose an option:\n" +
                    "1. List files\n" +
                    "2. Delete a file\n" +
                    "3. Rename a file\n" +
                    "4. Download a file from server\n" +
                    "5. Upload a file to server\n" +
                    ">> ");
            switch (){

            }
        }
        ByteBuffer buffer = ByteBuffer.wrap(filename.getBytes());
        channel.write(buffer);
        channel.shutdownOutput();

        FileOutputStream fs = new FileOutputStream(
                "ClientFiles/"+filename, true
        );
        FileChannel fc = fs.getChannel();
        ByteBuffer fileContent =
                ByteBuffer.allocate(1024);
        while(channel.read(fileContent) >=0) {
            fileContent.flip();
            fc.write(fileContent);
            fileContent.clear();
        }
        fs.close();
        channel.close();
    }
}
