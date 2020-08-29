package com.hins.libary.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author: hins
 * @created: 2020-08-23 13:25
 * @desc: 公共的数据封装
 *        提供了类型以及基本的长度定义
 **/
public abstract class Packet<T extends Closeable> implements Closeable {

    /**
     * 发送的数据类型 string、文件、图片等
     */
    protected byte type;

    /**
     * 数据包长度
     */
    protected long length;

    private T stream;

    public byte type(){
        return type;
    }

    public long length(){
        return length;
    }


    /**
     * 这里使用final 防止子类复写
     * @return
     */
    public final T open(){
        if(stream == null){
            stream = createStream();
        }
        return stream;
    }


    @Override
    public final void close() throws IOException {
        if(stream != null){
            closeStream(stream);
            stream = null;
        }
    }

    /**
     * 创建流方法供给子类拓展,父类抽取了公共的open close 方法。
     * @return
     */
    protected abstract T createStream();

    protected void closeStream(T stream) throws IOException {
        stream.close();
    }



}
