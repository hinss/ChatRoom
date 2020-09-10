package com.hins.libary.clink.core.impl.async;

import com.hins.libary.clink.core.IoArgs;
import com.hins.libary.clink.core.SendDispather;
import com.hins.libary.clink.core.SendPacket;
import com.hins.libary.clink.core.Sender;
import com.hins.libary.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: hins
 * @created: 2020-08-23 15:07
 * @desc:
 **/
public class AsyncSendDispather implements SendDispather,IoArgs.IoArgsEventProcessor {

    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedDeque<SendPacket>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);


    private IoArgs ioArgs = new IoArgs();
    private SendPacket<?> packetTemp;
    private ReadableByteChannel readableByteChannel;

    // 当前packet发送的大小与进度
    private long total;
    private long position;

    public AsyncSendDispather(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }


    @Override
    public void send(SendPacket sendPacket) {

        queue.offer(sendPacket);
        if(isSending.compareAndSet(false,true)){

            sendNextPacket();

        }

    }

    @Override
    public void cancel(SendPacket sendPacket) {

    }

    /**
     * 递归拿发送包的方法
     * @return
     */
    private SendPacket takePacket(){
        SendPacket sendPacket = queue.poll();
        if(sendPacket != null && sendPacket.isCanceled()){

            // 已取消 不用发送
            return takePacket();
        }
        return sendPacket;
    }


    private void sendNextPacket() {

        // 保证每一次发送的包
        SendPacket temp = packetTemp;
        if(temp != null){
            CloseUtils.close(temp);
        }

        SendPacket sendPacket = packetTemp = takePacket();
        if(sendPacket == null){
            // 队列为空，取消状态发送 让下一个包可以继续发送
            isSending.set(false);
            return;
        }

        //设置发送包的总长度和初始发送位置
        total = sendPacket.length();
        position = 0;

        sendCurrentPacket();

    }

    private void sendCurrentPacket() {

        if(position >= total){
            completePacket(position == total);
            sendNextPacket();
            return ;
        }

        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }

    }

    /**
     * 完成Packet发送
     *
     * @param isSucceed 是否成功
     */
    private void completePacket(boolean isSucceed){

        SendPacket sendPacket = this.packetTemp;
        if(sendPacket == null){
            return ;
        }

        CloseUtils.close(packetTemp);
        CloseUtils.close(readableByteChannel);

        packetTemp = null;
        readableByteChannel = null;
        total = 0;
        position = 0;



    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void close() throws IOException {

        if(isClosed.compareAndSet(false,true)){
            isSending.set(false);
            //异常关闭导致的完成操作
            completePacket(false);
        }

    }

    @Override
    public IoArgs provideIoArgs() {

        IoArgs args = ioArgs;
        if(readableByteChannel == null){
            readableByteChannel = Channels.newChannel(packetTemp.open());
            // 首包
            args.limit(4);
            //args.writeLength((int) packetTemp.length());
        }else{
            args.limit((int) Math.min(args.capacity(),total - position));

            try {
                int count = args.readFrom(readableByteChannel);
                position += count;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return args;
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        // 继续发送当前包
        sendCurrentPacket();
    }
}
