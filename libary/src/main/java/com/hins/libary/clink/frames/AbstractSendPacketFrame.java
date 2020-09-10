package com.hins.libary.clink.frames;

import com.hins.libary.clink.core.Frame;
import com.hins.libary.clink.core.IoArgs;
import com.hins.libary.clink.core.SendPacket;

import java.io.IOException;

/**
 * @author: hins
 * @created: 2020-09-09 21:16
 * @desc:
 **/
public abstract class AbstractSendPacketFrame extends AbstractSendFrame {
    protected SendPacket<?> packet;

    public AbstractSendPacketFrame(int length, byte type, byte flag, short identifier, SendPacket sendPacket) {
        super(length, type, flag, identifier);
        this.packet = sendPacket;
    }

    @Override
    public synchronized boolean handle(IoArgs ioArgs) throws IOException {
        if(packet == null && !isSending()){
            // 已取消，并且未发送任何数据, 直接返回结束，发送下一帧
            return true;
        }

        return super.handle(ioArgs);
    }

    @Override
    public final synchronized Frame nextFrame() {
        return packet == null ? null : buildNextFrame();
    }

    // True, 当前帧没有发送任何数据
    // 1234, 12,34

    public final synchronized boolean abort(){
        boolean isSending = isSending();
        if(isSending){
            fillDirtDataOnAbort();
        }
        packet = null;

        return !isSending;
    }
    protected void fillDirtDataOnAbort(){



    }

    protected abstract Frame buildNextFrame();


}
