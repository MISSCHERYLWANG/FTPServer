package com.whocares.turtle.ftp;

public class FtpServerConfig {
    private String HomePath;
    private int port, dataPort;

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    private String dataDir;

    FtpServerConfig(String homePath, int port, int dataPort, String dataDir){
        this.HomePath = homePath;
        this.port = port;
        this.dataPort = dataPort;
        this.dataDir = dataDir;
    }

    FtpServerConfig(String path, int port, int dataPort){
        this.HomePath = path;
        this.port = port;
        this.dataPort = dataPort;
        this.dataDir = System.getProperty("user.dir");
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
