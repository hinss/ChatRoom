package com.hins.libary.clink.frames;

import com.hins.libary.clink.core.Frame;
import com.hins.libary.clink.core.IoArgs;
import com.hins.libary.clink.core.SendPacket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * @author: hins
 * @created: 2020-09-09 21:15
 * @desc:
 **/
public class SendHeaderFrame extends AbstractSendPacketFrame {
    private static final int PACKET_HEADER_FRAME_MIN_LENGTH = 6;

    private final byte[] body;

    public SendHeaderFrame(short identifier, SendPacket packet){
        super(PACKET_HEADER_FRAME_MIN_LENGTH,
                Frame.TYPE_PACKET_HEADER,
                Frame.FLAG_NONE,
                identifier,
                packet);

        final long packetLength = packet.length();
        final byte packetType = packet.type();
        final byte[] packetHeaderInfo = packet.headerInfo();

        this.body = new byte[bodyRemaing];

        // 前5个字节存储packet的长度
        body[0] = (byte)(packetLength >> 32);
        body[1] = (byte)(packetLength >> 24);
        body[2] = (byte)(packetLength >> 16);
        body[3] = (byte)(packetLength >> 8);
        body[4] = (byte)(packetLength);

        body[5] = packetType;

        if( packetHeaderInfo != null){
            System.arraycopy(packetHeaderInfo, 0,
                    body, PACKET_HEADER_FRAME_MIN_LENGTH, packetHeaderInfo.length);
        }
    }


    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        int count = bodyRemaing;
        int offset = body.length - count;
        return ioArgs.readFrom(body, offset, count);
    }

    @Override
    public Frame buildNextFrame() {
        InputStream stream = packet.open();
        ReadableByteChannel channel = Channels.newChannel(stream);

        return new SendEntityFrame(getBodyIdentifier(), packet.length(), channel, packet);
    }
}
