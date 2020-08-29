package com.hins.libary.clink.box;

import com.hins.libary.clink.core.ReceivePacket;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;

/**
 * @author: hins
 * @created: 2020-08-23 14:50
 * @desc:
 **/
public class StringReceivePacket extends ReceivePacket<ByteArrayOutputStream> {
    private String string;

    public StringReceivePacket(int len){
        this.length = len;
    }


    public String string(){
        return string;
    }

    @Override
    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int)length);
    }

    @Override
    protected void closeStream(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        super.closeStream(byteArrayOutputStream);
        string = new String(byteArrayOutputStream.toByteArray());
    }
}
