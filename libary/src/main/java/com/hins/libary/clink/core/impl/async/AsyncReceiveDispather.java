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
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: hins
 * @created: 2020-08-23 16:28
 * @desc:
 **/
public class AsyncReceiveDispather implements ReceiveDispather, IoArgs.IoArgsEventProcessor {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final Receiver receiver;
    private final ReceivePacketCallback callback;

    private IoArgs ioArgs = new IoArgs();
    private ReceivePacket<?> packetTemp;

    private WritableByteChannel writableByteChannel;
    private long total;
    private long position;

    public AsyncReceiveDispather(Receiver receiver, ReceivePacketCallback callback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(this);
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
            completePacket(false);

        }

    }

    private void registerReceive() {

        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            closeAndNotify();
        }
    }

    private void closeAndNotify() {

        CloseUtils.close(this);
    }

    /**
     * 解析数据到packet中
     * @param args
     */
    private void assemblePacket(IoArgs args) {

        if(packetTemp == null){

            //这里读到的length就是设置在IoArgs数据头部中 需要后续读到的整个数据包的大小
            int length = args.readLength();
            packetTemp = new StringReceivePacket(length);
            writableByteChannel = Channels.newChannel(packetTemp.open());

            total = length;
            position = 0;
        }

        try {
            int count = args.writeTo(writableByteChannel);
            position += count;

            // 检查是否已完成一份Packet接收
            if(position == total){
                completePacket(true);
                packetTemp = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 完成数据接收操作
     */
    private void completePacket(boolean isSucceed) {
        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);
        packetTemp = null;

        WritableByteChannel writableByteChannel = this.writableByteChannel;
        CloseUtils.close(writableByteChannel);
        this.writableByteChannel = null;

        if(packet != null){
            callback.onReceivePacketCompalted(packet);
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        IoArgs args = ioArgs;

        int receiveSize;
        if(packetTemp == null){
            //这里设置的4个字节正是每一个paket的长度信息，
            //接收端每次处理一个完成的数据包都会先读取头部信息再去读取真实数据!!
            receiveSize = 4;
        }else{
            receiveSize = (int)Math.min(total - position,ioArgs.capacity());
        }
        //设置本次接收数据大小
        args.limit(receiveSize);

        return args;
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {

        assemblePacket(args);
        registerReceive();
    }
}
