package com.hins.libary.clink.frames;

import com.hins.libary.clink.core.Frame;
import com.hins.libary.clink.core.IoArgs;
import com.hins.libary.clink.core.SendPacket;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * @author: hins
 * @created: 2020-09-09 21:15
 * @desc:
 **/
public class SendEntityFrame extends AbstractSendPacketFrame {
    private ReadableByteChannel readableByteChannel;
    private final long unConsumeEntityLength;

    SendEntityFrame(short identifier,
                           long entityLength,
                           ReadableByteChannel readableByteChannel,
                           SendPacket sendPacket) {
        super((int)Math.min(entityLength, Frame.MAX_CAPACITY),
                Frame.TYPE_PACKET_ENTITY,
                Frame.FLAG_NONE,
                identifier,
                sendPacket);

        this.readableByteChannel = readableByteChannel;
        this.unConsumeEntityLength = entityLength - bodyRemaing;
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        if(packet == null){
            // 已取消发送，填充假数据
           return ioArgs.fillEmpty(bodyRemaing);
        }

        return ioArgs.readFrom(readableByteChannel);
    }

    @Override
    public Frame buildNextFrame() {

        if(unConsumeEntityLength == 0){
            return null;
        }

        return new SendEntityFrame(getBodyIdentifier(), unConsumeEntityLength, readableByteChannel, packet);
    }
}
