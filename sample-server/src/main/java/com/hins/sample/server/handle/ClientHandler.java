package com.hins.sample.server.handle;



import com.hins.libary.clink.core.Connector;
import com.hins.libary.clink.utils.CloseUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler extends Connector{
    private final ClientHandlerNotify clientHandlerNotify;
    private String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerNotify clientHandlerNotify) throws IOException {
        this.clientHandlerNotify = clientHandlerNotify;
        this.clientInfo = socketChannel.getRemoteAddress().toString();

        System.out.println("新客户端连接：" + clientInfo);

        setup(socketChannel);

    }

    public void exit() {
        CloseUtils.close(this);
        System.out.println("客户端已退出：" + clientInfo);
    }

    @Override
    public void onChannelClosed(SocketChannel socketChannel) {
        super.onChannelClosed(socketChannel);
        exitBySelf();
    }

    private void exitBySelf() {
        exit();
        clientHandlerNotify.onSelfClosed(this);
    }

    @Override
    protected void onReceiveNewMessage(String str) {
        super.onReceiveNewMessage(str);
        clientHandlerNotify.onMsgReturn(str,this);
    }

    public interface ClientHandlerNotify {
        void onSelfClosed(ClientHandler handler);

        void onMsgReturn(String msg, ClientHandler handler);
    }

}
