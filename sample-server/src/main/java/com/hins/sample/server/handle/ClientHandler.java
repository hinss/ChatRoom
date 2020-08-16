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

public class ClientHandler {
    private final Connector connector;
    private final SocketChannel socketChannel;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerNotify clientHandlerNotify;
    private String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerNotify clientHandlerNotify) throws IOException {
        this.socketChannel = socketChannel;

        connector = new Connector(){
            @Override
            public void onChannelClosed(SocketChannel socketChannel) {
                super.onChannelClosed(socketChannel);
                exitBySelf();
            }

            @Override
            protected void onReceiveNewMessage(String str) {
                super.onReceiveNewMessage(str);
                // 收到消息回调
                clientHandlerNotify.onMsgReturn(str,ClientHandler.this);

            }
        };
        connector.setup(socketChannel);



        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new ClientWriteHandler(writeSelector);

        this.clientHandlerNotify = clientHandlerNotify;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接：" + clientInfo);
    }

    public String getClientInfo(){
        return clientInfo;
    }

    public void exit() {
        CloseUtils.close(connector);
        writeHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("客户端已退出：" + clientInfo);
    }

    public void send(String str) {
        writeHandler.send(str);
    }

    private void exitBySelf() {
        exit();
        clientHandlerNotify.onSelfClosed(this);
    }

    public interface ClientHandlerNotify {
        void onSelfClosed(ClientHandler handler);

        void onMsgReturn(String msg, ClientHandler handler);
    }

    class ClientWriteHandler {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final ExecutorService executorService;

        ClientWriteHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        void exit() {
            done = true;
            CloseUtils.close(selector);
            executorService.shutdownNow();
        }

        void send(String str) {
            executorService.execute(new WriteRunnable(str));
        }

        class WriteRunnable implements Runnable {
            private final String msg;

            WriteRunnable(String msg) {
                this.msg = msg + "\n";
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }

                // 清空
                byteBuffer.clear();
                // 往byteBuffer 放数据
                byteBuffer.put(msg.getBytes());
                // 反转操作 重点
                byteBuffer.flip();

                //判断是否还有数据剩余
                while(!done && byteBuffer.hasRemaining()){
                    try {
                        int len = socketChannel.write(byteBuffer);
                        // len = 0 合法
                        if(len < 0){
                            System.out.println("客户端已经无法发送数据了!");
                            ClientHandler.this.exitBySelf();
                            break;
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }



            }
        }
    }
}
