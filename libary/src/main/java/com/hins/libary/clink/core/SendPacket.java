package com.hins.libary.clink.core;


import java.io.InputStream;

/**
 * @author: hins
 * @created: 2020-08-23 13:27
 * @desc: 发送包的定义
 **/
public abstract class SendPacket<T extends InputStream> extends Packet<T>{

    /**
     * 发送标识
     */
    private boolean isCanceled;

    public boolean isCanceled(){
        return isCanceled;
    }

}
