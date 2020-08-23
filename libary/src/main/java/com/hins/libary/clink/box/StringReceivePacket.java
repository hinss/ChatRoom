package com.hins.libary.clink.box;

import com.hins.libary.clink.core.ReceivePacket;

import java.io.IOException;

/**
 * @author: hins
 * @created: 2020-08-23 14:50
 * @desc:
 **/
public class StringReceivePacket extends ReceivePacket {
    private byte[] buffer;
    /**
     * 记录buffer的保存到的position
     */
    private int position;

    public StringReceivePacket(int len){
        this.buffer = new byte[len];
        this.length = len;
    }

    @Override
    public void save(byte[] bytes, int count) {

        System.arraycopy(bytes,0,buffer,position,count);
        position += count;
    }

    public String string(){
        return new String(buffer);
    }

    @Override
    public void close() throws IOException {

    }
}
