package com.hins.libary.clink.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

/**
 * 1.IoProvider 并非针对单个连接，是作用于全部连接。
 *
 * 2.观察者模式:
 * 通过将SocketChannel注册 然后观察它的可读/可写状态，
 * 一旦触发了可读/可写状态然后调用CallBack回调函数
 */
public interface IoProvider extends Closeable {

    boolean registerInput(SocketChannel channel, HandleInputCallback callback);

    boolean registerOutput(SocketChannel channel, HandleOutputCallback callback);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);

    abstract class HandleInputCallback implements Runnable {
        @Override
        public final void run() {
            canProviderInput();
        }

        protected abstract void canProviderInput();
    }

    abstract class HandleOutputCallback implements Runnable {
        private Object attach;

        @Override
        public final void run() {
            canProviderOutput(attach);
        }

        public final void setAttach(Object attach) {
            this.attach = attach;
        }

        public final <T> T getAttach() {
            return (T) attach;
        }

        protected abstract void canProviderOutput(Object attach);
    }

}
