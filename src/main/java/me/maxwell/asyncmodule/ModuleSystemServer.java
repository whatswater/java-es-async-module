package me.maxwell.asyncmodule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class ModuleSystemServer {
    private ModuleSystem moduleSystem;

    public void startServer() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel ss = ServerSocketChannel.open();
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 9527);
        ss.bind(hostAddress);
        ss.configureBlocking(false);
        ss.register(selector, ss.validOps());

        while(true) {
            int n = selector.select();
            if(n == 0) {
                continue;
            }
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while(it.hasNext()) {
                SelectionKey key = it.next();

                if(key.isAcceptable()) {
                    SocketChannel client = ss.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                }
                else if(key.isReadable()) {
                    SelectableChannel channel = key.channel();
                    if(channel instanceof SocketChannel) {
                        SocketChannel socketChannel = (SocketChannel) channel;
                        ByteBuffer buffer = ByteBuffer.allocate(256);
                        socketChannel.read(buffer);

                        System.out.print(new String(buffer.array()));
                        buffer.flip();
                        socketChannel.write(buffer);
                    }
                }
                it.remove();
            }
        }
    }

    public void setModuleSystem(ModuleSystem moduleSystem) {
        this.moduleSystem = moduleSystem;
    }
}
