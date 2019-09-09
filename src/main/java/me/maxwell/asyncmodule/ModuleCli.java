package me.maxwell.asyncmodule;


import java.io.Console;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Scanner;

public class ModuleCli {
    public static void main(String[] args) throws IOException {
        /*InetSocketAddress address = new InetSocketAddress("localhost", 9527);
        try (SocketChannel socketChannel = SocketChannel.open()){
            socketChannel.connect(address);

            ByteBuffer retBuffer = ByteBuffer.allocate(2048);
            while(true) {
                Console console = System.console();
                if(console == null) {
                    break;
                }

                console.printf("\"[module system]> ");
                String command = console.readLine();
                if("exit".equals(command)) {
                    break;
                }

                byte[] bytes = command.getBytes();
                socketChannel.write(ByteBuffer.wrap(bytes));
                socketChannel.read(retBuffer);
                System.out.println(new String(retBuffer.array()));
                retBuffer.clear();
            }
        }*/

        Console console = System.console();
        if(console == null) {
            Scanner scanner = new Scanner(System.in);
            while(true) {
                System.out.println(System.in.getClass());
                System.out.print("[module system]>");
                String command = scanner.nextLine().trim();
                if(command.length() == 0) {
                    System.out.print("\n");
                    continue;
                }
                if("exit".equals(command)) {
                    break;
                }
                System.out.println(command);
            }
        }
        else {
            while(true) {
                console.printf("[module system]>");
                String command = console.readLine().trim();

                if(command.length() == 0) {
                    console.printf("\n");
                    continue;
                }
                if("exit".equals(command)) {
                    break;
                }

                console.printf(command + "\n");
            }
        }
    }
}
