package com.hins.libary.clink.core;


import com.hins.libary.clink.box.*;
import com.hins.libary.clink.core.impl.SocketChannelAdapter;
import com.hins.libary.clink.core.impl.async.AsyncReceiveDispather;
import com.hins.libary.clink.core.impl.async.AsyncSendDispather;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * Connector 类代表一个连接 抽象出的Connector类
 */
public abstract class Connector implements Closeable,SocketChannelAdapter.OnChannelStatusChangedListener {

    protected UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    private SendDispather sendDispather;
    private ReceiveDispather receiveDispather;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;

        IoContext ioContext = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel,ioContext.getIoProvider(),this);

        this.sender = adapter;
        this.receiver = adapter;

        this.sendDispather = new AsyncSendDispather(sender);
        this.receiveDispather = new AsyncReceiveDispather(receiver,receivePacketCallback);

        // 启动接收
        receiveDispather.start();



    }

    public void send(String msg){

        SendPacket sendPacket = new StringSendPacket(msg);
        // Packet -> IOArgs 的转换 才能丢到Sender 发送
        sendDispather.send(sendPacket);

    }

    public void send(FileSendPacket fileSendPacket){

        sendDispather.send(fileSendPacket);

    }


    @Override
    public void close() throws IOException {
        receiveDispather.close();
        sendDispather.close();
        sender.close();
        receiver.close();
        channel.close();
    }

    @Override
    public void onChannelClosed(SocketChannel socketChannel) {



    }

    protected void onReceivedPacket(ReceivePacket receivePacket){
        System.out.println(key.toString() + ":[New Packet]-Type "+ receivePacket.type() + ", Length:" + receivePacket.length());
    }

    protected abstract File createNewReceiveFile();

    private ReceiveDispather.ReceivePacketCallback receivePacketCallback = new ReceiveDispather.ReceivePacketCallback() {

        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {

            switch (type){
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile());
                case Packet.TYPE_STREAM_DIRECT:
                    return new BytesReceivePacket(length);
                default:
                    throw new UnsupportedOperationException("Unsupported type");
            }
        }

        @Override
        public void onReceivePacketCompalted(ReceivePacket receivePacket) {
            onReceivedPacket(receivePacket);
        }
    };
}
