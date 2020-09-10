package com.hins.libary.clink.frames;

import com.hins.libary.clink.core.Frame;
import com.hins.libary.clink.core.IoArgs;

import java.io.IOException;

/**
 * @author: hins
 * @created: 2020-09-09 21:16
 * @desc:
 **/
public abstract class AbstractSendFrame extends Frame {

    volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;

    volatile int bodyRemaing;

    public AbstractSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
        this.bodyRemaing = length;
    }

    /**
     * 消费真实数据
     * @param ioArgs
     * @return
     * @throws IOException
     */
    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        try {
            ioArgs.limit(headerRemaining + bodyRemaing);
            ioArgs.startWriting();

            if(headerRemaining > 0 && ioArgs.remained()){
                headerRemaining -= consumeHeader(ioArgs);
            }

            if(headerRemaining == 0 && ioArgs.remained() && bodyRemaing > 0){
                bodyRemaing -= consumeBody(ioArgs);
            }

            return headerRemaining == 0 && bodyRemaing == 0;
        } finally {
            ioArgs.finishWriting();
        }
    }

    private byte consumeHeader(IoArgs ioArgs){
        // 剩余位置
        int count = headerRemaining;
        // 总长 - 剩余位置 = 开始位移
        int offset = header.length - count;
        return (byte)ioArgs.readFrom(header, offset, count);
    }

    protected abstract int consumeBody(IoArgs ioArgs) throws IOException;

    protected boolean isSending(){
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;
    }
}
