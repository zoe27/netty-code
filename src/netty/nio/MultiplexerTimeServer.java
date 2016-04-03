package netty.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable {

    private Selector selector;
    
    private ServerSocketChannel server;
    
    private volatile boolean stop;
    
    public MultiplexerTimeServer(int port) {

        try {
            selector = Selector.open();
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port), 1024);
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("the time server is start in port :" + port);
        } catch (Exception e) {
            System.exit(1);
        }
    }
    
    public void stop(){
        this.stop = true;
    }
    
    
    @Override
    public void run() {
        
        while(!stop){
            try {
                selector.select(1000);
                Set<SelectionKey> selectKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        handleInput(key);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
    
    
    private void handleInput(SelectionKey key) throws IOException{
        if (key.isValid()) {
            if (key.isAcceptable()) {
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                sc.register(selector, SelectionKey.OP_READ);
            }
            if (key.isReadable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer readBf = ByteBuffer.allocate(1024);
                int readBytes = sc.read(readBf);
                if (readBytes > 0) {
                    readBf.flip();
                    byte[] bytes = new byte[readBf.remaining()];
                    readBf.get(bytes);
                    String body = new String(bytes, "utf-8");
                    System.out.println("the server receive order :" + body);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? 
                            new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                    doWrite(sc, currentTime);
                }else if (readBytes < 0) {
                    key.cancel();
                    sc.close();
                }else {
                    
                }
            }
        }
    }

    private void doWrite(SocketChannel channel, String response) throws IOException{
        if (response != null && response.trim().length() > 0) {
            byte[] bytes = response.getBytes();
            ByteBuffer writeByte = ByteBuffer.allocate(bytes.length);
            writeByte.put(bytes);
            writeByte.flip();
            channel.write(writeByte);
        }
        
    }
}
