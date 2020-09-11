package com.hins.libary.clink.core;

import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @author: hins
 * @created: 2020-09-11 14:28
 * @desc:
 **/
public class IoArgsTest {

    @Test
    public void testBuffer(){

        byte[] bytes = new byte[]{1,2,3,4,5};
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);

        byteBuffer.put(bytes, 0, 2);

        System.out.println("DEBUG");
    }

    @Test
    public void testIntToByte(){

        int count = 2;
        byte b = (byte)count;
        System.out.println(b);

    }

    @Test
    public void testSystemArrayCopy(){
        byte[] body = new byte[3];
        byte[] packetHeaderInfo = new byte[]{6,6,6,6,6};

        System.arraycopy(packetHeaderInfo, 0,
                body, 3, packetHeaderInfo.length);

        // this will throw ArrayIndexOutOfBoundsException.
        System.out.println(body);
    }


}
