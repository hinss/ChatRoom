package com.hins.libary.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author: hins
 * @created: 2020-08-23 13:25
 * @desc: 公共的数据封装
 *        提供了类型以及基本的长度定义
 **/
public abstract class Packet implements Closeable {

    /**
     * 发送的数据类型 string、文件、图片等
     */
    protected byte type;

    /**
     * 数据包长度
     */
    protected int length;

    public byte type(){
        return type;
    }

    public int length(){
        return length;
    }

}
