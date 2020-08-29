package com.hins.libary.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class IoArgs {

    private int limit = 5;
    private ByteBuffer buffer = ByteBuffer.allocate(5);

    /**
     * 从bytes中读数据
     */
    public int readFrom(ReadableByteChannel readableByteChannel) throws IOException {

        startWriting();

        int bytesProduced = 0;
        while (buffer.hasRemaining()){

            // 把数据从channel 读到 buffer当中
            int len = readableByteChannel.read(buffer);
            if(len < 0){
                throw new EOFException();
            }

            bytesProduced += len;
        }

        finishWriting();
        return bytesProduced;
    }

    /**
     * 写入数据到bytes中
     * @return
     */
    public int writeTo(WritableByteChannel writableByteChannel) throws IOException {

        int bytesProduced = 0;
        while (buffer.hasRemaining()){

            // 把数据从channel 写到 buffer当中
            int len = writableByteChannel.write(buffer);
            if(len < 0){
                throw new EOFException();
            }

            bytesProduced += len;
        }
        return bytesProduced;
    }


    /**
     * 从socketChannel中读取数据
     * @param channel
     * @return
     * @throws IOException
     */
    public int readFrom(SocketChannel channel) throws IOException {

        startWriting();

        int bytesProduced = 0;
        while (buffer.hasRemaining()){

            // 把数据从channel 读到 buffer当中
            int len = channel.read(buffer);
            if(len < 0){
                throw new EOFException();
            }

            bytesProduced += len;
        }

        finishWriting();

        return bytesProduced;
    }

    /**
     * 写入数据到SocketChannel中
     * @param channel
     * @return
     * @throws IOException
     */
    public int writeTo(SocketChannel channel) throws IOException {

        int bytesProduced = 0;
        while (buffer.hasRemaining()){

            // 把数据从channel 写到 buffer当中
            int len = channel.write(buffer);
            if(len < 0){
                throw new EOFException();
            }

            bytesProduced += len;
        }
        return bytesProduced;
    }

    /**
     * 开始写入数据到IoArgs中
     */
    public void startWriting(){
        buffer.clear();
        // 定义容纳区间
        buffer.limit(limit);
    }

    /**
     * 设置单次写操作的容纳区间
     */
    public void limit(int limit){
        this.limit = limit;
    }

    /**
     * 写完数据后调用
     */
    public void finishWriting(){
        buffer.flip();
    }

    public void writeLength(int total){
        startWriting();
        buffer.putInt(total);
        finishWriting();
    }

    public int readLength(){

        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    /**
     * IoArgs 提供者、处理者; 数据的生产或消费者
     */
    public interface IoArgsEventProcessor {

        /**
         * 提供一份可消费的IoArgs
         * @return
         */
        IoArgs provideIoArgs();

        /**
         * 消费失败时回调
         *
         * @param args
         * @param e
         */
        void onConsumeFailed(IoArgs args, Exception e);

        /**
         * 消费成功
         * @param args
         */
        void onConsumeCompleted(IoArgs args);
    }
}
