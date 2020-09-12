package com.hins.libary.clink.core.impl.async;

import com.hins.libary.clink.core.IoArgs;
import com.hins.libary.clink.core.SendDispather;
import com.hins.libary.clink.core.SendPacket;
import com.hins.libary.clink.core.Sender;
import com.hins.libary.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: hins
 * @created: 2020-08-23 15:07
 * @desc:
 **/
public class AsyncSendDispather implements SendDispather,
        IoArgs.IoArgsEventProcessor, AsyncPacketReader.PacketProvier {

    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedDeque<SendPacket>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final AsyncPacketReader packetReader = new AsyncPacketReader(this);
    private final Object queueLock = new Object();

    AsyncSendDispather(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }


    @Override
    public void send(SendPacket sendPacket) {

        synchronized (queueLock) {
            queue.offer(sendPacket);
            if(isSending.compareAndSet(false,true)){
                if(packetReader.requestTakePacket()){
                    requestSend();
                }
            }
        }
    }

    @Override
    public void cancel(SendPacket sendPacket) {
        boolean ret;
        // 取消发送packet 从队列中移除发送包
        synchronized (queueLock) {
            ret = queue.remove(sendPacket);
        }

        if(ret){
            sendPacket.cancel();
            return ;
        }

        // 真实取消
        packetReader.cancel(sendPacket);
    }

    /**
     * 递归拿发送包的方法
     * @return
     */
    @Override
    public SendPacket takePacket(){
        SendPacket sendPacket;
        synchronized (queueLock){
           sendPacket = queue.poll();
           if(sendPacket == null){
               // 队列为空, 取消发送状态
               isSending.set(false);
               return null;
           }
        }

        if(sendPacket.isCanceled()){

            // 已取消 不用发送
            return takePacket();
        }
        return sendPacket;
    }

    /**
     * 完成Packet发送
     *
     * @param isSucceed 是否成功
     */
    @Override
    public void completedPacket(SendPacket sendPacket, boolean isSucceed) {
        CloseUtils.close(sendPacket);
    }

    @Override
    public void close() {

        if(isClosed.compareAndSet(false,true)){
            isSending.set(false);
            // reader 关闭
            packetReader.close();
        }

    }

    @Override
    public IoArgs provideIoArgs() {

        return packetReader.fillData();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        if(args != null){
            e.printStackTrace();
        }
        // TODO
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        // 继续发送当前包
        if (packetReader.requestTakePacket()){
            requestSend();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    /**
     * 真实的注册请求网络发送
     */
    private void requestSend() {

        try {
            sender.postSendAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }
}
