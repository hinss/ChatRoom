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

    private IoArgs.IoArgsEventProcessor receiveIoArgsEventProcessor;
    private IoArgs.IoArgsEventProcessor sendIoArgsEventProcessor;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventProcessor processor) {
        receiveIoArgsEventProcessor = processor;
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        if(isClosed.get()){
            throw new IOException("Current channel is closed");
        }

        return ioProvider.registerInput(channel,handleInputCallback);
    }

    @Override
    public void setSendListener(IoArgs.IoArgsEventProcessor processor) {
        sendIoArgsEventProcessor = processor;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if(isClosed.get()){
            throw new IOException("Current channel is closed");
        }

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

            IoArgs.IoArgsEventProcessor processor = receiveIoArgsEventProcessor;
            // 当该socketChannel 可读的时候才准备IoArgs
            IoArgs ioArgs = processor.provideIoArgs();

            // 具体的读取操作
            try {
                if(ioArgs.readFrom(channel) > 0){
                    //读取完成的回调
                    processor.onConsumeCompleted(ioArgs);
                }else{
                    processor.onConsumeFailed(ioArgs,new IOException("Cannot readFrom any data!"));
                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }

        }
    };

    private IoProvider.HandleOutputCallback handleOutputCallback = new IoProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput() {
            if(isClosed.get()){
                return ;
            }

            IoArgs.IoArgsEventProcessor processor = sendIoArgsEventProcessor;

            IoArgs args = processor.provideIoArgs();

            // 具体的发送操作
            try {
                if(args.writeTo(channel) > 0){
                    // 发送完成的回调
                    processor.onConsumeCompleted(args);
                }else{
                    processor.onConsumeFailed(args,new IOException("Cannot write any data!"));

                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    public interface OnChannelStatusChangedListener{

        void onChannelClosed(SocketChannel socketChannel);
    }
}
