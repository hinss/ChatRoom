package com.hins.sample.client;

import com.hins.sample.client.bean.ServerInfo;
import com.hins.sample.foo.Foo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: hins
 * @created: 2020-08-09 17:03
 * @desc: 模拟多个客户端访问服务器的测试类
 **/
public class ClientTest {

    private static boolean done;

    public static void main(String[] args) throws InterruptedException, IOException {

        File cachePath = Foo.getCacheDir("client/test");

        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);
        if(info == null){
            return ;
        }

        // 当前的连接数量
        int size = 0;

        List<TCPClient> tcpClientList = new ArrayList<>();
        for(int i = 0; i < 1000; i++){
            try {
                TCPClient tcpClient = TCPClient.getTcpClient(info,cachePath);
                if(tcpClient == null){
                    System.out.println("连接异常");
                    continue;
                }

                System.out.println("连接成功,当前size"+ (++size));
                tcpClientList.add(tcpClient);

            } catch (IOException e) {
                System.out.println("连接异常");
            }

            // 稍微延迟20ms
            Thread.sleep(20);
        }

        // 阻塞 直到等待键盘输入才开始发送数据
        System.in.read();

        // 开一个新的线程 去调用全部的clinet 往server 发送数据
        Runnable runnable = () -> {
            while(!done){
                for(TCPClient client : tcpClientList){
                    client.send("HELLO~~");
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();

        // 阻塞 直到等待到键盘输入才开始断开全部的连接
        System.in.read();

        done = true;

        // 等待直到线程的任务处理完毕
        // Waits for this thread to die.
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 结束所有客户端并且释放资源
        for(TCPClient client : tcpClientList){
            client.exit();
        }
    }


}
