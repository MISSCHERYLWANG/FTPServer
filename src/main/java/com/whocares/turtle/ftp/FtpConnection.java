package com.whocares.turtle.ftp;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FtpConnection implements Runnable {

    private Socket socket, dataConnection;
    private FtpServer ftpServer;
    private ServerSocket dataSocket;
    private String user, pwd;
    private String directory = "/";

    private enum UserStatus {
        NOTLOGGEDIN, PRINTEDUSERNAME, LOGGEDIN
    }
    private UserStatus userStatus = UserStatus.NOTLOGGEDIN;

    private enum TransferType {
        ASCII, BINARY
    }
    private TransferType transferType = TransferType.ASCII;

    FtpConnection(Socket s, FtpServer server) {
        this.socket = s;
        this.ftpServer = server;
    }
    @Override
    public void run() {
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bw.write("220 Welcome to turtleFTP.\n");
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //读取客户端发送来的消息
        while (true) {
            try {

                String mess = br.readLine();
                System.out.println("客户端：" + mess);

                String [] messArr = mess.split(" ");
                if (messArr.length == 0) {
                    bw.write("500 Unknow command.\n");
                    continue;
                }
                switch (messArr[0].toUpperCase()) {
                    case "USER" :
                        this.user = messArr[1];
                        if (user.equals("anonymous")) {
                            userStatus = UserStatus.PRINTEDUSERNAME;
                            bw.write("331 Please specify the password.\n");
                        } else {
                            bw.write("530 This FTP server is anonymous only.\n");
                        }
                        break;
                    case "PASS" :
                        this.pwd = messArr[1];

                        if (userStatus == UserStatus.PRINTEDUSERNAME && pwd.equals("anonymous")) {
                            userStatus = UserStatus.LOGGEDIN;
                            bw.write("230 Login Successfully.\n");
                        } else {
                            bw.write("530 Password is incorrect.\n");
                        }
                        break;
                    case "TYPE":
                        String args = messArr[1];
                        if (args.toUpperCase().equals("A")) {
                            transferType = TransferType.ASCII;
                            bw.write("200 OK\n");
                        } else if(args.toUpperCase().equals("I")){
                            transferType = TransferType.BINARY;
                            bw.write("200 OK\n");
                        } else {
                            bw.write("504 Not OK\n");
                        }
                        break;
                    case "QUIT":
                        bw.write("221 Good bye.\n");
                        socket.close();
                        return;
                    case "PORT":
                        handlePort(messArr[1]);
                        bw.write("200 Command OK.\n");
                        break;
                    case "PASV":
//                        String myIp = ftpServer.getDataServer().getInetAddress().getHostAddress();
                        String localIp = InetAddress.getLocalHost().getHostAddress();
                        String[] ip = localIp.split("\\.");
                        int dataPort = ftpServer.getDataServer().getLocalPort();
                        int port1 = dataPort / 256, port2 = dataPort % 256;
                        System.out.println("Will send " + String.join(",", ip));
                        bw.write("227 Entering Passive Mode (" + ip[0] +"," + ip[1] + "," + ip[2] + ","
                                + ip[3] + "," + port1 + "," + port2 + ")\r\n");
                        bw.flush();
                        System.out.println("227 Entering Passive Mode (" + ip[0] +"," + ip[1] + "," + ip[2] + ","
                                + ip[3] + "," + port1 + "," + port2 + ")\r\n");
                        dataConnection = ftpServer.getDataServer().accept();
                        System.out.println("Receive data connection");
                        break;
                    case "LIST":
                        if (dataConnection == null || dataConnection.isClosed()) {
                            bw.write("425 No data connection was established\n");
                        } else {

                            bw.write("150 Here comes the directory listing.\n");
                            bw.flush();
                            BufferedWriter dataBw = new BufferedWriter(
                                    new OutputStreamWriter(dataConnection.getOutputStream()));

                            File file = new File(this.ftpServer.getConfig().getDataDir(), directory);
                            File[] files = file.listFiles();

                            System.out.println("Start Send file info");
                            for (File f : files) {
                                long lastModified = f.lastModified();
                                Date date = new Date(lastModified);
                                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d yyyy", new Locale("en"));
                                if(!f.isDirectory()) {
                                    String resp = "-rw-r--r-- 1 0 0" + f.length()+" "+dateFormat.format(date)+" "+f.getName()+"\r\n";
                                    System.out.println("Send "+ resp);
                                    dataBw.write(resp);
                                }else{
                                    String resp = "drwxr-xr-x " + f.list().length +  " 0 0 " + f.length()+" "+dateFormat.format(date)+" "+f.getName()+"\r\n";
                                    System.out.println("Send "+ resp);
                                    dataBw.write(resp);
                                }
                            }
                            dataBw.flush();
                            System.out.println("Send file info complete");
                            dataConnection.close();
                            bw.write("226 Directory send OK.\n");
                        }
                        break;
                    case "RETR":
                        File file = new File(this.ftpServer.getConfig().getDataDir() + File.separator + this.directory + File.separator + messArr[1]);

                        if (!file.exists()) {
                            bw.write("550 File does not exist.");
                        }
                        // transerfer mode
                        if (transferType == TransferType.ASCII) {
                            bw.write("150 Opening ASCII mode data connection for requested file " + file.getName() + ".\n");
                            bw.flush();

                            BufferedReader dataBr = null;
                            PrintWriter dataPw = null;

                            try {
                                dataBr = new BufferedReader(new FileReader(file));
                                dataPw = new PrintWriter(dataConnection.getOutputStream());

                                String s;
                                while ((s = dataBr.readLine()) != null ) {
                                    dataPw.println(s);
                                }

                                dataBr.close();
                                dataPw.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else if (transferType == TransferType.BINARY) {
                            bw.write("150 Opening Binary mode data connection for requested file " + file.getName()+".\n");
                            bw.flush();
                            BufferedInputStream dataIn = null;
                            BufferedOutputStream dataOut = null;

                            try {
                                dataIn = new BufferedInputStream(new FileInputStream(file));
                                dataOut = new BufferedOutputStream(dataConnection.getOutputStream());

                                byte[] buffer = new byte[1024];
                                int len = 0;
                                while ((len = dataIn.read(buffer, 0, 1024)) != -1) {
                                    dataOut.write(buffer, 0, len);
                                }
                                dataIn.close();
                                dataOut.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        bw.write("226 File transfer successful. Closing data connection.\n");
                        bw.flush();
                        dataConnection.close();
                        break;
                    // 上传文件
                    case "STOR":
                        if (messArr[1] == null) {
                            bw.write("501 No filename given.\n");
                        } else {
                            File file1 = new File(this.ftpServer.getConfig().getDataDir() + File.separator + directory + File.separator + messArr[1]);
                            if (file1.exists()) {
                                bw.write("550 File already exists.\n");
                            } else {
                                if (transferType == TransferType.ASCII) {
                                    bw.write("150 Opening ASCII mode data connection for requested file " + file1.getName() + ".\n");
                                    bw.flush();
                                    BufferedReader dataBr = null;
                                    PrintWriter dataPw = null;
                                    try {
                                        dataBr = new BufferedReader(new InputStreamReader(dataConnection.getInputStream()));
                                        dataPw = new PrintWriter(new FileOutputStream(file1));

                                        String s;
                                        while ((s = dataBr.readLine()) != null) {
                                            dataPw.println(s);
                                        }

                                        dataBr.close();
                                        dataPw.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    bw.write("150 Opening Binary mode data connection for requested file " + file1.getName() + ".\n");
                                    bw.flush();

                                    BufferedInputStream dataIn = null;
                                    BufferedOutputStream dataOut = null;
                                    try {
                                       dataIn = new BufferedInputStream(dataConnection.getInputStream());
                                       dataOut = new BufferedOutputStream(new FileOutputStream(file1));

                                       byte[] buffer = new byte[1024];
                                       int len = 0;
                                       while ((len = dataIn.read(buffer, 0, 1024)) != -1) {
                                           dataOut.write(buffer, 0, len);
                                       }
                                       dataOut.flush();
                                       dataOut.close();
                                       System.out.println("write file complete");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                bw.write("226 File transfer successful. Closing data connection.\n");
                                bw.flush();
                                dataConnection.close();
                            }
                        }
                        break;
                    case "PWD":
                        bw.write("257 \"" + directory + "\" is the current directory.\n");
                        break;
                    case "CWD":
                        String datadir = this.ftpServer.getConfig().getDataDir() + File.separator;
                        String mappedDir = directory;
                        String dir = messArr[1];
                        if (dir.equals("..")) {
                            int end = mappedDir.lastIndexOf(File.separator);
                            if (end > 0) {
                                mappedDir = mappedDir.substring(0, end);
                            }
                        } else if (!dir.equals(".")) {
                            mappedDir = mappedDir + File.separator + dir;
                        }
                        File f = new File(datadir + mappedDir);
                        if (f.exists() && f.isDirectory() && (mappedDir.length() >= directory.length())) {
                            directory = mappedDir;
                            bw.write("250 Directory successfully changed.\n");
                        } else {
                            bw.write("550 Requested action not taken. File unavailable.\n");
                        }
                        bw.flush();
                        break;
                    default:
                        bw.write("500 Unknown Command.\n");
                }
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void activeDataConnection(String ip, int port) {
        try {
            dataConnection = new Socket(ip, port);
//            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
            System.out.println("Data connection主动模式");

        } catch (IOException e)
        {
            System.out.println("Could not create data connection.");
            e.printStackTrace();
        }
    }
    private void passiveDataConnection(int port) {
        try {
            dataSocket = new ServerSocket(port);
            dataConnection = dataSocket.accept();
            System.out.println("Data connection被动模式");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handlePort(String args) {

        String[] mess = args.split(",");
        String ip = mess[0] + "." + mess[1] + "." + mess[2] + "." + mess[3];
        int port = Integer.parseInt(mess[4]) * 256 + Integer.parseInt(mess[5]);
        activeDataConnection(ip, port);
    }

}
