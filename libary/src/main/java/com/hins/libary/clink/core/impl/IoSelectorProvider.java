package com.hins.libary.clink.core.impl;

import com.hins.libary.clink.core.IoProvider;
import com.hins.libary.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: hins
 * @created: 2020-08-15 15:49
 * @desc:
 **/
public class IoSelectorProvider implements IoProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Selector readSelector;
    private final Selector writeSelector;

    /**
     * 是否处于 input/output 注册的过程
     */
    private final AtomicBoolean inRegisInput = new AtomicBoolean(false);
    private final AtomicBoolean inRegisOutput = new AtomicBoolean(false);


    /**
     * 装载 读/写的 selectionKey 以及相应的回调Runnable线程
     */
    private final Map<SelectionKey, Runnable> inputCallbackMap = new HashMap<SelectionKey, Runnable>();
    private final Map<SelectionKey, Runnable> outputCallbackMap = new HashMap<SelectionKey, Runnable>();

    /**
     * 线程池管理 读/写的线程操作
     */
    private final ExecutorService inputHandlePool;
    private final ExecutorService outputHandlePool;

    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();

        inputHandlePool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Input-Thread-"));
        outputHandlePool = Executors.newFixedThreadPool(4,
                new IoProviderThreadFactory("IoProvider-Output-Thread-"));
        
        startRead();
        startWrite();
        

    }

    private void startRead() {

        Thread thread = new Thread("Clink IoSelectorProvider ReadSelector Thread"){
            @Override
            public void run() {
                super.run();

                try {
                    while(!isClosed.get()){

                        if(readSelector.select() == 0){
                            waitSelection(inRegisInput);
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {

                            if(selectionKey.isValid()){
                               // 丢给线程池异步处理每一个读的selectionKey
                               handleSelection(selectionKey, SelectionKey.OP_READ, inputCallbackMap, inputHandlePool);
                            }
                        }
                        selectionKeys.clear();

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        };

        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();


    }

    private void startWrite() {

        Thread thread = new Thread("Clink IoSelectorProvider WriteSelector Thread"){
            @Override
            public void run() {
                super.run();

                try {
                    while(!isClosed.get()){

                        if(writeSelector.select() == 0){
                            waitSelection(inRegisOutput);
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = writeSelector.selectedKeys();
                        for (SelectionKey selectionKey : selectionKeys) {

                            if(selectionKey.isValid()){
                                //丢给线程池异步处理每一个写的selectionKey
                                handleSelection(selectionKey, SelectionKey.OP_WRITE, outputCallbackMap, outputHandlePool);
                            }
                        }
                        selectionKeys.clear();

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        };

        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();

    }



    @Override
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
        return registerSelectionKey(channel,readSelector,SelectionKey.OP_READ, inRegisInput,
                inputCallbackMap, callback) != null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback   callback) {
        return registerSelectionKey(channel,writeSelector,SelectionKey.OP_WRITE, inRegisOutput,
                outputCallbackMap, callback) != null;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {
        unRegisterSelection(channel,readSelector,inputCallbackMap);
    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {
        unRegisterSelection(channel,readSelector,inputCallbackMap);
    }

    @Override
    public void close() throws IOException {

        if(isClosed.compareAndSet(false,true)){
            inputHandlePool.shutdown();
            outputHandlePool.shutdown();

            inputCallbackMap.clear();
            outputCallbackMap.clear();

            readSelector.wakeup();
            writeSelector.wakeup();

            CloseUtils.close(readSelector);
            CloseUtils.close(writeSelector);
        }
    }

    private static void waitSelection(final AtomicBoolean locker){

        synchronized (locker){
            if(locker.get()){
                try {
                    locker.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

    }


    private static SelectionKey registerSelectionKey(SocketChannel socketChannel, Selector selector,
                          int registerOps, AtomicBoolean locker,
                          Map<SelectionKey,Runnable> map,
                          Runnable runnable){

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (locker){
            // 设置状态
            locker.set(true);

            try{

                // 唤醒当前的selector， 让selector不处于select()状态
                // 這裡喚醒selector的原因是: selector 进入阻塞状态会占有锁，
                // 会导致无法注册channel。
                selector.wakeup();

                SelectionKey key = null;
                if(socketChannel.isRegistered()){
                    //查询是否已经注册过
                    key = socketChannel.keyFor(selector);
                    if( key != null){
                        // 把当前的key注册到原来的key中
                        key.interestOps(key.readyOps() | registerOps);
                    }

                }

                // 如果没有找到
                if(key == null){
                    // 注册 selector 得到key 并且订阅感兴趣事件。
                    key = socketChannel.register(selector, registerOps);
                    // 注册回调
                    map.put(key, runnable);
                    System.out.println(map.size());
                }

                return key;
            } catch (ClosedChannelException e) {
                return null;
            } finally {
                //解除锁定状态
                locker.set(false);
                try{
                    //通知
                    locker.notify();
                }catch (Exception ignored){

                }
            }
        }



    }

    private static void unRegisterSelection(SocketChannel socketChannel, Selector selector,
                                            Map<SelectionKey,Runnable> map){

        if(socketChannel.isRegistered()){
            SelectionKey selectionKey = socketChannel.keyFor(selector);
            if(selectionKey != null){
                // 取消监听的方法
                // 这里可以理解成 channel与selector的联系关系取消
                selectionKey.cancel();
                map.remove(selectionKey);
                selector.wakeup();
            }
        }

    }



    private static void handleSelection(SelectionKey key, int keyOps,
                                 Map<SelectionKey, Runnable> callbackMap,
                                 ExecutorService pool) {

        // 重点 取消感兴趣事件 这里可以理解成只是selectionKey中某个事件不再感兴趣，但是channel与Selector还存在绑定关系。
        // 1.为什么要取消感兴趣事件？看笔记
        // 2.当注册读事件时,实际上都是在同一个key上进行与运算叠加了;同理 当取消事件时，也可以通过与运算减掉这一部分
        key.interestOps(key.readyOps() & ~keyOps);

        Runnable runnable = null;
        try {
            runnable  = callbackMap.get(key);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

        if(runnable != null && !pool.isShutdown()){
            // 异步调度 回调函数
            pool.execute(runnable);
        }


    }

    static class IoProviderThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IoProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon()){
                t.setDaemon(false);
            }

            if (t.getPriority() != Thread.NORM_PRIORITY){
                t.setPriority(Thread.NORM_PRIORITY);
            }

            return t;
        }
    }
}
