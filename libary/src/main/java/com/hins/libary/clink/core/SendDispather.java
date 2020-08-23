package com.hins.libary.clink.core;

import java.io.Closeable;

/**
 * @author: hins
 * @created: 2020-08-23 14:58
 * @desc: 发送数据的调度者
 *  缓存所有需要发送的数据，通过队列对数据进行一个真实的发送
 *  并且在发送时对数据时，实现对数据的包装
 **/
public interface SendDispather extends Closeable {

    /**
     * 发送一份数据
     *
     * @param sendPacket
     */
    void send(SendPacket sendPacket);

    /**
     * 取消发送数据
     *
     * @param sendPacket
     */
    void cancel(SendPacket sendPacket);










}
