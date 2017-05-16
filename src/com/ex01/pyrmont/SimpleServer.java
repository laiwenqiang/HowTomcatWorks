package com.ex01.pyrmont;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by laiwenqiang on 2017/5/16.
 */
public class SimpleServer {

    public static void main(String[] args)  {

        ServerSocket serverSocket = null;
        int port = 8080;
        try {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        }  catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        while(true){
            Socket socket = null;
            OutputStream outputStream = null;

            try {
                socket = serverSocket.accept();
                outputStream = socket.getOutputStream();

                String msg = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "\r\n" +
                        "<html>\r\n" +
                        "<head>\r\n" +
                        "<title>HTTP com.ex01.pyrmont.Response Example</title>\r\n" +
                        "</head>\r\n" +
                        "<body>\r\n" +
                        "Welcome to Brainy Software\r\n" +
                        "</body>\r\n" +
                        "</html>";
                outputStream.write(msg.getBytes());

                Thread.sleep(50);//由于是阻塞写入，暂停 50ms，保证可以写入。

                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }
}
