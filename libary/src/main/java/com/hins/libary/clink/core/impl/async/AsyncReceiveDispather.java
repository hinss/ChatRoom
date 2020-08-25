package com.hins.libary.clink.core.impl.async;

import com.hins.libary.clink.box.StringReceivePacket;
import com.hins.libary.clink.core.IoArgs;
import com.hins.libary.clink.core.ReceiveDispather;
import com.hins.libary.clink.core.ReceivePacket;
import com.hins.libary.clink.core.Receiver;
import com.hins.libary.clink.utils.CloseUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.Buffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: hins
 * @created: 2020-08-23 16:28
 * @desc:
 **/
public class AsyncReceiveDispather implements ReceiveDispather {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private IoArgs ioArgs = new IoArgs();
    private ReceivePacket packetTemp;
    private byte[] buffer;
    private int total;
    private int position;

    public AsyncReceiveDispather(Receiver receiver, ReceivePacketCallback callback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(listener);
        this.callback = callback;
    }


    @Override
    public void start() {

        registerReceive();

    }

    @Override
    public void stop() {

    }

    @Override
    public void close() throws IOException {

        if(isClosed.compareAndSet(false,true)){
            ReceivePacket packet = packetTemp;
            if (packet != null) {
                packetTemp = null;
                CloseUtils.close(packet);
            }

        }

    }

    private void registerReceive() {

        try {
            receiver.receiveAsync(ioArgs);
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {

        CloseUtils.close(this);
    }

    private IoArgs.IoArgsEventListener listener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

            int receiveSize;
            if(packetTemp == null){
                receiveSize = 4;
            }else{
                receiveSize = Math.min(total - position,ioArgs.capacity());
            }
            //设置本次接收数据大小
            args.limit(receiveSize);

        }

        @Override
        public void onCompleted(IoArgs args) {

            assemblePacket(args);
            // 继续接收下一条数据
            registerReceive();
        }
    };

    /**
     * 解析数据到packet中
     * @param args
     */
    private void assemblePacket(IoArgs args) {

        if(packetTemp == null){

            //这里读到的length就是设置在IoArgs数据头部中 需要后续读到的整个数据包的大小
            int length = args.readLength();
            packetTemp = new StringReceivePacket(length);
            buffer = new byte[length];
            total = length;
            position = 0;
        }

        int count  = args.writeTo(buffer, 0);
        if(count > 0){
            packetTemp.save(buffer,count);
            position += count;

            // 检查是否已完成一份Packet接收
            if(position == total){
                completePacket();
                packetTemp = null;
            }
        }
    }

    /**
     * 完成数据接收操作
     */
    private void completePacket() {

        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);

        callback.onReceivePacketCompalted(packet);
    }
}
