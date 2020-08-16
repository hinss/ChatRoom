package com.hins.sample.server;


import com.hins.libary.clink.utils.CloseUtils;
import com.hins.sample.server.handle.ClientHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public class TCPServer implements ClientHandler.ClientHandlerNotify {
    private final int port;
    private ClientListener mListener;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardExecutorService;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;


    public TCPServer(int port) {

        this.port = port;
        this.forwardExecutorService = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    public boolean start() {
        try {
            // 开启selector选择器
            this.selector = Selector.open();
            // 开启服务端Socket Channel
            ServerSocketChannel server = ServerSocketChannel.open();
            // 设置为非阻塞模式
            server.configureBlocking(false);
            // 绑定本地 TCP端口
            server.bind(new InetSocketAddress(port));
            // 注册客户端连接到达监听
            server.register(selector, SelectionKey.OP_ACCEPT);

            this.serverSocketChannel = server;

            System.out.println("服务器信息：" + serverSocketChannel.getLocalAddress().toString());

            // 启动客户端监听线程
            ClientListener listener = this.mListener = new ClientListener();
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (mListener != null) {
            mListener.exit();
        }

        CloseUtils.close(serverSocketChannel);
        CloseUtils.close(selector);

        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }
            clientHandlerList.clear();
        }

        //停止线程池
        forwardExecutorService.shutdownNow();
    }

    public synchronized void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.send(str);
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public synchronized void onMsgReturn(String msg, ClientHandler selfHandler) {

        forwardExecutorService.execute(() -> {
            synchronized (TCPServer.this){

                for (ClientHandler clientHandler : clientHandlerList) {

                    if(!clientHandler.equals(selfHandler)){
                        clientHandler.send(msg);
                    }
                }
            }
        });

    }

    private class ClientListener extends Thread {
        private boolean done = false;

        @Override
        public void run() {
            super.run();

            // 成员变量转局部变量
            Selector selector = TCPServer.this.selector;

            System.out.println("服务器准备就绪～");
            // 等待客户端连接
            do {
                try {
                    // Selects a set of keys whose corresponding channels are ready for I/O * operations.
                    // 唤醒状态 select 会等于0 如果有事件到达会返回事件数量
                    if(selector.select() == 0){
                     if(done){
                         break ;
                     }
                        continue;
                    }

                    Iterator<SelectionKey> selectionKeyIterator = selector.selectedKeys().iterator();
                    while(selectionKeyIterator.hasNext()){
                        if(done){
                            break;
                        }

                        SelectionKey key = selectionKeyIterator.next();
                        selectionKeyIterator.remove();

                        //检查当前key的状态是否我们关注的客户端到达状态
                        if(key.isAcceptable()){

                            //得到开始注册的Channel
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();

                            //非阻塞方式得到客户端
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            try {
                                // 客户端构建异步线程
                                ClientHandler clientHandler = new ClientHandler(socketChannel,TCPServer.this);
                                // 添加同步处理
                                synchronized (TCPServer.this) {
                                    clientHandlerList.add(clientHandler);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("客户端连接异常：" + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } while (!done);

            System.out.println("服务器已关闭！");
        }

        void exit() {
            done = true;
            //唤醒当前的阻塞selector
            selector.wakeup();
        }
    }
}
