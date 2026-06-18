package cn.unicom.soc.servers.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description
 * @Author chenss
 * @CreateTime 2025-06-04 15:51:37
 * @ModifyTime
 */
public class NioSocketClient {
    private final String host;
    private final int port;
    private final ByteBuffer writeBuffer = ByteBuffer.allocate(8192);
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192000);
    private SocketChannel socketChannel;
    private Selector selector;

    public NioSocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }


    public String sendAndGet(String data) {
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(host, port));
            while (!close.get()) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    if (key.isConnectable()) {
                        handleConnect(key);
                    } else if (key.isWritable()) {
                         handleWrite(key, data);
                    } else if (key.isReadable()) {
                        return handleRead(key);
                    }
                }
            }
        } catch (Exception e) {
            return "get data error :"+e.getMessage();
        }
        return null;
    }

    private void handleConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.finishConnect()) {
            channel.register(key.selector(), SelectionKey.OP_WRITE);
        }
    }

    private void handleWrite(SelectionKey key, String data) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        writeBuffer.clear();
        writeBuffer.put((data+"\n").getBytes(StandardCharsets.UTF_8));
        writeBuffer.flip();
        channel.write(writeBuffer);
        channel.register(key.selector(), SelectionKey.OP_READ);
    }

    private String handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        readBuffer.clear();
        int bytesRead = channel.read(readBuffer);
        String response ="";
        if (bytesRead > 0) {
            readBuffer.flip();
            response = StandardCharsets.UTF_8.decode(readBuffer).toString();
        }
        channel.close();
        closeResources();
        return response;
    }

    private final AtomicBoolean close=new AtomicBoolean();

    private void closeResources() {
        try {
            if (socketChannel != null) {
                socketChannel.close();
            }
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            System.err.println("Resource close error: " + e.getMessage());
        }
    }

    public void close() {
        close.set(true);
        closeResources();
    }

    public static void main(String[] args) {
        NioSocketClient nioSocketClient = new NioSocketClient("localhost", 13221);
        System.out.println(nioSocketClient.sendAndGet(""));
//        System.out.println(nioSocketClient.sendAndGet("netflow日志文件在：/data/data1/agent/0710netflow/目录下的csv文件里，帮我逐个识别一下每个文件里是否有疑似攻击行为的上一跳IP，攻击ip为：47.109.195.74,139.129.37.64,123.56.250.88,39.96.184.76,47.95.21.173,47.93.42.153,39.105.183.77,47.117.130.111,39.96.185.110,47.100.87.133,112.74.95.215,59.110.215.109,47.94.98.97,47.93.223.223,39.106.85.41,39.96.165.179,47.93.62.116,59.110.13.105,47.122.84.251,47.110.36.48,8.134.48.71,114.215.197.90,123.56.201.19,101.69.218.49,121.43.254.55,101.200.90.246,120.26.140.254,118.31.185.185,101.201.102.170,39.96.168.113,39.96.175.110"));
    }
}
