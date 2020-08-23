package com.hins.libary.clink.core;

/**
 * @author: hins
 * @created: 2020-08-23 13:27
 * @desc: 发送包的定义
 **/
public abstract class SendPacket extends Packet{

    /**
     * 发送标识
     */
    private boolean isCanceled;

    /**
     * 发送包内容
     * @return
     */
    public abstract byte[] bytes();

    public boolean isCanceled(){
        return isCanceled;
    }


}
