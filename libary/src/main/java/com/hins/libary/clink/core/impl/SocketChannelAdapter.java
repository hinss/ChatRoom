package com.hins.libary.clink.core.impl;

import com.hins.libary.clink.core.IoArgs;
import com.hins.libary.clink.core.IoProvider;
import com.hins.libary.clink.core.Receiver;
import com.hins.libary.clink.core.Sender;
import com.hins.libary.clink.utils.CloseUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: hins
 * @created: 2020-08-15 15:50
 * @desc: Socket Channel 具体输入 输出的实现类
 **/
public class SocketChannelAdapter implements Sender, Receiver, Closeable {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final SocketChannel channel;
    private final IoProvider ioProvider;
    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgsEventListener receiveIoArgsEventListener;
    private IoArgs.IoArgsEventListener sendIoArgsEventListener;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    @Override
    public boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException {

        if(isClosed.get()){
            throw new IOException("Current channel is closed");
        }

        receiveIoArgsEventListener = listener;

        return ioProvider.registerInput(channel,handleInputCallback);
    }

    @Override
    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException {

        if(isClosed.get()){
            throw new IOException("Current channel is closed");
        }

        sendIoArgsEventListener = listener;
        //当前发送的数据附加到回调中
         handleOutputCallback.setAttach(args);

        return ioProvider.registerOutput(channel,handleOutputCallback);

    }

    @Override
    public void close() throws IOException {

        if(isClosed.compareAndSet(false,true)){
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);

            CloseUtils.close(channel);
            // 回调当前Channel已关闭
            listener.onChannelClosed(channel);
        }

    }

    private IoProvider.HandleInputCallback handleInputCallback = new IoProvider.HandleInputCallback() {
        @Override
        protected void canProviderInput() {

            if(isClosed.get()){
                return ;
            }

            IoArgs ioArgs = new IoArgs();
            IoArgs.IoArgsEventListener receiveIoEventListener = SocketChannelAdapter.this.receiveIoArgsEventListener;
            if(receiveIoEventListener != null){
                // 读取开始的回调
                receiveIoEventListener.onStarted(ioArgs);
            }

            // 具体的读取操作
            try {
                if(ioArgs.read(channel) > 0 && receiveIoEventListener!= null){
                    //读取完成的回调
                    receiveIoEventListener.onCompleted(ioArgs);
                }else{
                    throw new IOException("Cannot read any data!");
                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }

        }
    };

    private IoProvider.HandleOutputCallback handleOutputCallback = new IoProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput(Object attach) {
            if(isClosed.get()){
                return ;
            }

            //TODO
            sendIoArgsEventListener.onCompleted(null);
        }
    };



    public interface OnChannelStatusChangedListener{

        void onChannelClosed(SocketChannel socketChannel);
    }
}
