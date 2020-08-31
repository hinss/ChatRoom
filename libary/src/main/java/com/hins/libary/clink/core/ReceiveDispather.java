package com.hins.libary.clink.core;

import java.io.Closeable;

/**
 * @author: hins
 * @created: 2020-08-23 15:00
 * @desc: 接收的数据调度封装
 * 把一份或者多份IOArgs组成一份Packet
 **/
public interface ReceiveDispather extends Closeable {

    void start();

    void stop();

    interface ReceivePacketCallback{

        ReceivePacket<?,?> onArrivedNewPacket(byte type, long length);

        void onReceivePacketCompalted(ReceivePacket receivePacket);
    }


}
