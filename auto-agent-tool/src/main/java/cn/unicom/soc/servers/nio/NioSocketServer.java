package cn.unicom.soc.servers.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * @Description
 * @Author chenss
 * @CreateTime 2024-08-09 15:27:45
 * @ModifyTime
 */
public class NioSocketServer {
    private ServerSocketChannel serverSocketChannel;
    private final int port;
    private int bufLength = 1024 * 1024 * 10;

    public NioSocketServer(int port) {
        this.port = port;
    }

    public void setBufLength(int bufLength) {
        this.bufLength = bufLength;
    }

    public void connect() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(new InetSocketAddress(this.port));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private volatile boolean close;
    private Selector selector;

    public void receiveData(DataSink dataSink) {
        try {
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (!close) {
                try {
                    //选择已就绪的key
                    selector.select();
                    //获取已就绪的key的集合
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey readyKey = iterator.next();
                        iterator.remove();
                        //判断当前已就绪key是否是连接通道的key
                        if (readyKey.isAcceptable()) {
                            //接收客户端的连接
                            SocketChannel accept = serverSocketChannel.accept();
                            accept.configureBlocking(false);
                            System.out.println("connect");
                            accept.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(bufLength));
                        } else if (readyKey.isReadable()) {
                            SocketChannel channel = (SocketChannel) readyKey.channel();
                            ByteBuffer buffer = (ByteBuffer) readyKey.attachment();
                            readLine(channel, buffer, dataSink);
                            channel.close();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final String LINE_SEPARATOR = System.lineSeparator();
    private final ByteBuffer writeBuffer = ByteBuffer.allocate(8192000);

    public void readLine(SocketChannel channel, ByteBuffer buffer, DataSink dataSink) throws IOException {
        final int MAX_LINE_LENGTH = bufLength * 2;
        final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        buffer.clear();
        StringBuilder sb = new StringBuilder();
        int lineFeedCount = 0;
        try {
            while (channel.isConnected()) {
                int bytesRead = channel.read(buffer);
                if (bytesRead == -1) {
                    // 客户端正常关闭连接
                    if (sb.length() > 0) {
                        writeBuffer.clear();
                        writeBuffer.put(dataSink
                                .sendDataAndStatus(sb.toString()).getBytes(StandardCharsets.UTF_8));
                        writeBuffer.flip();
                        channel.write(writeBuffer);
                        channel.close();
                    }
                    break;
                }

                buffer.flip();
                try {
                    CharBuffer charBuffer = decoder.decode(buffer);
                    while (charBuffer.hasRemaining()) {
                        char c = charBuffer.get();
                        if (sb.length() <= MAX_LINE_LENGTH) {
                            if (c == LINE_SEPARATOR.charAt(lineFeedCount)) {
                                lineFeedCount = (lineFeedCount + 1) % LINE_SEPARATOR.length();
                                String data = sb.toString();
                                if (lineFeedCount == 0) {
                                    if (!data.trim().isEmpty()) {
                                        writeBuffer.clear();
                                        writeBuffer.put(dataSink.sendDataAndStatus(data).getBytes(StandardCharsets.UTF_8));
                                        writeBuffer.flip();
                                        channel.write(writeBuffer);
                                        channel.close();
                                    }
                                    sb = new StringBuilder();
                                }
                                continue;
                            } else {
                                lineFeedCount = 0;
                            }
                            sb.append(c);
                        }
                    }
                } catch (Exception e) {
                    // 处理解码异常
                    if (!channel.isOpen()) {
                        break;
                    }
                    e.printStackTrace();
                }
                buffer.clear();
            }
        } catch (Exception e) {
            // 客户端强制断开连接
            if (channel.isOpen()) {
                channel.close();
            }
        }
    }

    public void close() {
        this.close = true;
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
