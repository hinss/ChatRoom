package com.hins.libary.clink.box;

import com.hins.libary.clink.core.SendPacket;

import java.io.ByteArrayInputStream;

/**
 * @author: hins
 * @created: 2020-08-23 14:42
 * @desc: 字符串类型的发送包
 **/
public class StringSendPacket extends SendPacket<ByteArrayInputStream> {

    private final byte[] bytes;

    public StringSendPacket(String msg) {
        this.bytes = msg.getBytes();
        this.length = bytes.length;
    }

    @Override
    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(bytes);
    }

}
