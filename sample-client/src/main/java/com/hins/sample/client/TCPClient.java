package com.hins.sample.client;



import com.hins.libary.clink.core.Connector;
import com.hins.libary.clink.utils.CloseUtils;
import com.hins.sample.client.bean.ServerInfo;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;

public class TCPClient extends Connector {


    public TCPClient(SocketChannel socketChannel) throws IOException {
       setup(socketChannel);
    }

    public void exit(){
        CloseUtils.close(this);
    }

    public static TCPClient getTcpClient(ServerInfo info) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();

        // 连接本地，端口2000；超时时间3000ms
        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socketChannel.getLocalAddress().toString());
        System.out.println("服务器信息：" + socketChannel.getRemoteAddress().toString());

        try {

            //构造TCP Client 对象
            TCPClient tcpClient = new TCPClient(socketChannel);

            return tcpClient;
        } catch (Exception e) {
            System.out.println("连接异常");
            CloseUtils.close(socketChannel);
        }

        return null;
    }

    @Override
    public void onChannelClosed(SocketChannel socketChannel) {
        super.onChannelClosed(socketChannel);
        System.out.println("连接已关闭，无法读取数据!");

    }
}
