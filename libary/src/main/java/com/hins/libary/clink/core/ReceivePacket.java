package com.hins.libary.clink.core;

/**
 * @author: hins
 * @created: 2020-08-23 14:36
 * @desc: 接收包的定义
 **/
public abstract class ReceivePacket extends Packet {

    /**
     * 将接收到的bytes 消息保存下来，可能保存为 string、图片、文件等...
     * @param bytes
     * @param count 不一定全部bytes都能用到，count用于记录保存数
     */
    public abstract void save(byte[] bytes, int count);



}
