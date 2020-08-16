package com.hins.libary.clink.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class IoArgs {
    private byte[] byteBuffer = new byte[256];
    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    public int read(SocketChannel channel) throws IOException {
        buffer.clear();

        // 把数据从channel 读到 buffer当中
        return channel.read(buffer);
    }

    public int write(SocketChannel channel) throws IOException {
        return channel.write(buffer);
    }

    public String bufferString() {
        // 丢弃换行符
        return new String(byteBuffer, 0, buffer.position() - 1);
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
