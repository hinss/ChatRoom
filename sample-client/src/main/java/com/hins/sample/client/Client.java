package com.hins.sample.client;



import com.hins.libary.clink.box.FileSendPacket;
import com.hins.libary.clink.core.IoContext;
import com.hins.libary.clink.core.impl.IoSelectorProvider;
import com.hins.sample.client.bean.ServerInfo;
import com.hins.sample.foo.Foo;

import java.io.*;

public class Client {
    public static void main(String[] args) throws IOException {
        //拿到缓存文件
        File cachePath = Foo.getCacheDir("client");
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .start();


        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            TCPClient tcpClient = null;
            try {
                    tcpClient  = TCPClient.getTcpClient(info,cachePath);
                    if(tcpClient == null){
                        return ;
                    }
                    write(tcpClient);

            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(tcpClient != null){
                    tcpClient.exit();
                }
            }
        }

        IoContext.close();
    }

    private static void write(TCPClient tcpClient) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String str = input.readLine();
            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }

            // --f url
            if(str.startsWith("--f")){
                String[] array = str.split(" ");
                if(array.length >= 2){
                    String filePath = array[1];
                    File file = new File(filePath);
                    if(file.exists() && file.isFile()){
                        FileSendPacket fileSendPacket = new FileSendPacket(file);
                        tcpClient.send(fileSendPacket);
                        continue;
                    }
                }
            }
            // 发送到服务器 一次发送3条测试粘包解决方案
            tcpClient.send(str);
//            tcpClient.send(str);
//            tcpClient.send(str);


        } while (true);
     }
}
