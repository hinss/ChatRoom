package com.hins.libary.clink.core;


import com.hins.libary.clink.box.StringReceivePacket;
import com.hins.libary.clink.box.StringSendPacket;
import com.hins.libary.clink.core.impl.SocketChannelAdapter;
import com.hins.libary.clink.core.impl.async.AsyncReceiveDispather;
import com.hins.libary.clink.core.impl.async.AsyncSendDispather;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * Connector 类代表一个连接 抽象出的Connector类
 */
public class Connector implements Closeable,SocketChannelAdapter.OnChannelStatusChangedListener {

    private UUID key = UUID.randomUUID();
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

    protected void onReceiveNewMessage(String str){
        System.out.println(key.toString() + ": "+ str);
    }

    private ReceiveDispather.ReceivePacketCallback receivePacketCallback = new ReceiveDispather.ReceivePacketCallback() {
        @Override
        public void onReceivePacketCompalted(ReceivePacket receivePacket) {
          if(receivePacket instanceof StringReceivePacket){
              String msg = ((StringReceivePacket) receivePacket).string();
              onReceiveNewMessage(msg);
          }
        }
    };
}
