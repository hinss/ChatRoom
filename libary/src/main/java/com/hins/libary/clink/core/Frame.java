package com.hins.libary.clink.core;

import java.io.IOException;

/**
 * @author: hins
 * @created: 2020-09-09 21:12
 * @desc: 帧类
 **/
public abstract class Frame {
    public static final int FRAME_HEADER_LENGTH = 6;
    public static final int MAX_CAPACITY = 64 * 1024 - 1;


    public static final byte TYPE_PACKET_HEADER = 11;
    public static final byte TYPE_PACKET_ENTITY = 12;


    public static final byte TYPE_COMMAND_SEND_CANCEL = 41;
    public static final byte TYPE_COMMAND_RECEIVE_REJECT = 42;

    public static final byte FLAG_NONE = 0;




    protected final byte[] header = new byte[FRAME_HEADER_LENGTH];

    public Frame(int length, byte type, byte flag, short identifier){

        if(length < 0 || length > MAX_CAPACITY){
            throw new RuntimeException("");
        }

        if(identifier < 1 || identifier > 255){
            throw new RuntimeException("");
        }

        // 00000000 00000000 00000000 01000000
        // 右移8位 再将最后8位转成byte 也就是取得倒数两个8位
        header[0] = (byte) (length>>8);
        // 取最后一个8位
        header[1] = (byte) length;

        header[2] = type;
        header[3] = flag;

        header[4] = (byte)identifier;

        header[5] = 0;
    }

    public Frame(byte[] header){
        System.arraycopy(header, 0, this.header, 0, FRAME_HEADER_LENGTH);
    }

    /**
     * 获取帧的body大小
     * @desc:如何获取一个byte存储的int数据
     * @return
     */
    public int getBodyLength(){
        // header[0] 00000000
        // header[1] 01000000

        // 希望得到 00000000 00000000 00000000 01000000

        // 实际会在转int值时会补齐为1
        // 11111111 11111111 00000000 01000000
        // 将前面的16位变为0 那么我们需要将0xFF与上面进行 | 与操作
        // 00000000 00000000 00000000 11111111 0xff

        return (((int)header[0] & 0xFF) << 8) | (((int)header[1]) & 0xFF);
    }

    public byte getBodyType(){
        return header[2];
    }

    public byte getBodyFlag(){
        return header[3];
    }

    public short getBodyIdentifier(){
        return (short)(((short)header[4]) & 0xFF);
    }

    public abstract boolean handle(IoArgs ioArgs) throws IOException;

    /**
     * 构建下一帧 每次发送了当前帧才构建下一帧 避免内存堆积
     */
    public abstract Frame nextFrame();


}
