package com.hins.libary.clink.frames;

import com.hins.libary.clink.core.Frame;
import com.hins.libary.clink.core.IoArgs;

import java.io.IOException;

/**
 * @author: hins
 * @created: 2020-09-09 21:16
 * @desc: 取消发送帧，用于标志某Packet取消进行发送数据
 **/
public class CancelSendFrame extends AbstractSendFrame {

    public CancelSendFrame(short identifier) {
        super(0,
                Frame.TYPE_COMMAND_SEND_CANCEL,
                Frame.FLAG_NONE,
                identifier);
    }

    @Override
    protected int consumeBody(IoArgs ioArgs) throws IOException {
        return 0;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }
}
