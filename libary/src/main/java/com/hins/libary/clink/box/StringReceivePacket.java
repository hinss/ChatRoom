package com.hins.libary.clink.box;


import java.io.ByteArrayOutputStream;

/**
 * @author: hins
 * @created: 2020-08-23 14:50
 * @desc:
 **/
public class StringReceivePacket extends AbsByteArrayReceivePacket<String> {

    public StringReceivePacket(long len){
        super(len);
    }

    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return new String(stream.toByteArray());
    }


    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }
}
