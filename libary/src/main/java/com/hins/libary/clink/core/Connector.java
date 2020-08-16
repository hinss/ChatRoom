package com.hins.libary.clink.core;


import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * Connector 类代表一个连接 抽象出的Connector类
 */
public class Connector {

    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;
    }
}
