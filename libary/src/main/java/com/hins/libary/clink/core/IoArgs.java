package com.hins.libary.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class IoArgs {

    private int limit = 256;
    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    /**
     * 从bytes中读数据
     * @param bytes
     * @param offset
     * @return
     */
    public int readFrom(byte[] bytes, int offset){

        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.put(bytes, offset, size);
        return size;

    }

    /**
     * 写入数据到bytes中
     * @param bytes
     * @param offset
     * @return
     */
    public int writeTo(byte[] bytes, int offset){

        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
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

        buffer.putInt(total);
    }

    public int readLength(){

        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }


    /**
     * 对于IoArgs的事件监听器
     */
    public interface IoArgsEventListener {

        // 对IoArgs 读取开始的回调
        void onStarted(IoArgs args);
        // 对IoArgs 读取完成的回调
        void onCompleted(IoArgs args);
    }
}
