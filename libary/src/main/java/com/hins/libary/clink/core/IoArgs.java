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

    private int limit = 256;
    private ByteBuffer buffer = ByteBuffer.allocate(256);

    /**
     * 从bytes数组进行消费
     */
    public int readFrom(byte[] bytes, int offset, int count) {

        int size = Math.min(count, buffer.remaining());
        if(size <= 0){
            return 0;
        }
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 写入数据到bytes中
     * @param bytes
     * @param offset
     * @return
     */
    public int writeTo(byte[] bytes, int offset) {

        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }



    /**
     * 从bytes中读数据
     */
    public int readFrom(ReadableByteChannel readableByteChannel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()){

            // 把数据从channel 读到 buffer当中
            int len = readableByteChannel.read(buffer);
            if(len < 0){
                throw new EOFException();
            }

            bytesProduced += len;
        }

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
        this.limit = Math.min(limit, buffer.capacity());
    }

    /**
     * 写完数据后调用
     */
    public void finishWriting(){
        buffer.flip();
    }

    public int readLength(){

        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public boolean remained() {

        return buffer.remaining() > 0;
    }

    public int fillEmpty(int size) {

        int fillSize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + fillSize);
        return fillSize;
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
