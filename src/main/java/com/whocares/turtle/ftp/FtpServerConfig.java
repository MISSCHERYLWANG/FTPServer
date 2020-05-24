package com.whocares.turtle.ftp;

public class FtpServerConfig {
    private String HomePath;
    private int port, dataPort;


    FtpServerConfig(String path, int port, int dataPort){
        this.HomePath = path;
        this.port = port;
        this.dataPort = dataPort;
    }

    public String getHomePath() {
        return HomePath;
    }

    public void setHomePath(String homePath) {
        HomePath = homePath;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getDataPort() {
        return dataPort;
    }

    public void setDataPort(int dataPort) {
        this.dataPort = dataPort;
    }
}
