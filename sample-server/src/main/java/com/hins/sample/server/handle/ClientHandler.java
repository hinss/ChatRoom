package com.hins.sample.server.handle;



import com.hins.libary.clink.utils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final SocketChannel socketChannel;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerNotify clientHandlerNotify;
    private String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerNotify clientHandlerNotify, ExecutorService forwardExecutor) throws IOException {
        this.socketChannel = socketChannel;

        //设置 非阻塞模式
        socketChannel.configureBlocking(false);

        Selector readSelector = Selector.open();
        socketChannel.register(readSelector, SelectionKey.OP_READ);
        this.readHandler = new ClientReadHandler(readSelector);

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
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("客户端已退出：" + clientInfo);
    }

    public void send(String str) {
        writeHandler.send(str);
    }

    public void readToPrint() {
        readHandler.start();
    }

    private void exitBySelf() {
        exit();
        clientHandlerNotify.onSelfClosed(this);
    }

    public interface ClientHandlerNotify {
        void onSelfClosed(ClientHandler handler);

        void onMsgReturn(String msg, ClientHandler handler);
    }

    class ClientReadHandler extends Thread {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;

        ClientReadHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
        }

        @Override
        public void run() {
            super.run();
            try {
                do {
                    // 客户端拿到一条数据
                    if(selector.select() == 0){
                        if(done){
                            break;
                        }
                        continue;
                    }

                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while(iterator.hasNext()){

                        if(done){
                            break;
                        }

                        SelectionKey key = iterator.next();
                        iterator.remove();

                        // 如果通道可读的
                        if(key.isReadable()){
                            SocketChannel client = (SocketChannel) key.channel();
                            //清空操作 然后读取
                            byteBuffer.clear();
                            //读取
                            int read = client.read(byteBuffer);
                            if(read > 0){
                                // 丢弃换行符
                                String str = new String(byteBuffer.array(),0,read - 1);

                                // 回调让TCP Server得到消息
                                clientHandlerNotify.onMsgReturn(str, ClientHandler.this);


                            }else{
                                System.out.println("客户端无法读取数据!");
                                 break;
                            }
                        }
                    }
                } while (!done);
            } catch (Exception e) {
                if (!done) {
                    System.out.println("连接异常断开");
                    ClientHandler.this.exitBySelf();
                }
            } finally {
                // 连接关闭
                CloseUtils.close(selector);
            }
        }

        void exit() {
            done = true;
            selector.wakeup();
            CloseUtils.close(selector);
        }
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
                this.msg = msg;
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
