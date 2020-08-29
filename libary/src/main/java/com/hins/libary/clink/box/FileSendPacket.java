package com.hins.libary.clink.box;

import com.hins.libary.clink.core.SendPacket;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * @author: hins
 * @created: 2020-08-23 14:42
 * @desc: 字符串类型的发送包
 **/
public class FileSendPacket extends SendPacket<FileInputStream> {

    public FileSendPacket(File  file) {
        this.length = file.length();
    }

    @Override
    protected FileInputStream createStream() {
        return null;
    }

}
