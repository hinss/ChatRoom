package com.hins.sample.client;



import com.hins.sample.client.bean.ServerInfo;

import java.io.*;

public class Client {
    public static void main(String[] args) throws IOException, InterruptedException {
        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            TCPClient tcpClient = null;
            try {
                    tcpClient  = TCPClient.getTcpClient(info);
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

    }

    private static void write(TCPClient tcpClient) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String str = input.readLine();
            // 发送到服务器
            tcpClient.send(str);

            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }
        } while (true);
     }
}
