package me.maxwell.asyncmodule;


import io.airlift.airline.*;

import java.io.Console;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Scanner;

public class ModuleCli {
    public static void main(String[] args) throws IOException {
//        InetSocketAddress address = new InetSocketAddress("localhost", 9527);
//        try (SocketChannel socketChannel = SocketChannel.open()){
//            socketChannel.connect(address);
//
//            ByteBuffer retBuffer = ByteBuffer.allocate(2048);
//            while(true) {
//                Console console = System.console();
//                if(console == null) {
//                    break;
//                }
//
//                console.printf("\"[module system]> ");
//                String command = console.readLine();
//                if("exit".equals(command)) {
//                    break;
//                }
//
//                byte[] bytes = command.getBytes();
//                socketChannel.write(ByteBuffer.wrap(bytes));
//                socketChannel.read(retBuffer);
//                System.out.println(new String(retBuffer.array()));
//                retBuffer.clear();
//            }
//        }

        while(true) {
            Console console = System.console();
            if(console == null) {
                break;
            }

            console.printf("[module system]>");
            String command = console.readLine();
            if("exit".equals(command)) {
                break;
            }
            console.printf(command + "\n");
        }
    }

    public static class GitCommand implements Runnable {
        @Option(type = OptionType.GLOBAL, name = "-v", description = "Verbose mode")
        public boolean verbose;

        public void run() {
            System.out.println(getClass().getSimpleName());
        }
    }

    @Command(name = "add", description = "Add file contents to the index")
    public static class Add extends GitCommand
    {
        @Arguments(description = "Patterns of files to be added")
        public List<String> patterns;

        @Option(name = "-i", description = "Add modified contents interactively.")
        public boolean interactive;
    }

    @Command(name = "show", description = "Gives some information about the remote <name>")
    public static class RemoteShow extends GitCommand
    {
        @Option(name = "-n", description = "Do not query remote heads")
        public boolean noQuery;

        @Arguments(description = "Remote to show")
        public String remote;
    }

    @Command(name = "add", description = "Adds a remote")
    public static class RemoteAdd extends GitCommand
    {
        @Option(name = "-t", description = "Track only a specific branch")
        public String branch;

        @Arguments(description = "Remote repository to add")
        public List<String> remote;
    }
}
