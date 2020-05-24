package com.whocares.turtle.ftp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FtpServer {

    public FtpServer(FtpServerConfig config) {
        this.config = config;
    }

    public FtpServer() {
        this.config = new FtpServerConfig("/", 8888, 12345);
    }


    public void start() {
        try {
            ServerSocket ss = new ServerSocket(config.getPort());
            this.dataServer = new ServerSocket(config.getDataPort());
            System.out.println("启动服务器....");
            while(true){
                Socket s = ss.accept();
                System.out.println("客户端:"+s.getInetAddress().getLocalHost()+"已连接到服务器");

                Thread t = new Thread(new FtpConnection(s, this));
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        FtpServer server = new FtpServer();
        server.start();
    }

    public ServerSocket getDataServer() {
        return dataServer;
    }

    public FtpServerConfig getConfig() {
        return config;
    }

    private ServerSocket dataServer;
    private FtpServerConfig config;
}
