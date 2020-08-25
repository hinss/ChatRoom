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
public class AsyncSendDispather implements SendDispather {

    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedDeque<SendPacket>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);


    private IoArgs ioArgs = new IoArgs();
    private SendPacket packetTemp;

    // 当前packet发送的大小与进度
    private int total;
    private int position;

    public AsyncSendDispather(Sender sender) {
        this.sender = sender;
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
        IoArgs args = ioArgs;

        // 开始 清理
        args.startWriting();

        if(position >= total){
            sendNextPacket();
            return ;
        }else if(position == 0){
            // 首包,需要携带长度信息
            args.writeLength(total);
        }

        byte[] bytes = packetTemp.bytes();
        //把bytes的数据写到IoArgs中
        int count = args.readFrom(bytes, position);
        position += count;

        // 完成封装
        args.finishWriting();

        try {
            sender.sendAsync(args,ioArgsEventListener);
        } catch (IOException e) {
            closeAndNotify();
        }

    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    private IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

//            int receiveSize;
//            if(packetTemp == null){
//                receiveSize = 4;
//            }else{
//                receiveSize = Math.min(total - position,ioArgs.capacity());
//            }
//            //设置本次接收数据大小
//            args.limit(receiveSize);

        }

        @Override
        public void onCompleted(IoArgs args) {
            // 继续发送当前包
            sendCurrentPacket();
        }
    };

    @Override
    public void close() throws IOException {

        if(isClosed.compareAndSet(false,true)){
            isSending.set(false);
            SendPacket sendPacket = this.packetTemp;
            if(sendPacket != null){
                packetTemp = null;
                CloseUtils.close(sendPacket);
            }
        }

    }
}
