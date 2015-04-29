package com.rayerrt.ftp;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;

import sun.net.TelnetInputStream;
import sun.net.TelnetOutputStream;
import sun.net.ftp.FtpClient;

import java.util.List;
/**
 * Java1.6自带的API对FTP的操作
 * @Title:Ftp.java
 * @author: 周玲斌
 */
public class FtpApache {
    /**
     * 本地文件名
     */
    private String localfilename;
    /**
     * 远程文件名
     */
    private String remotefilename;
    /**
     * FTP客户端
     */
    private FtpClient ftpClient;

    private String ip;
    private int port;
    private String username;
    private String password;

    public static final String SERVER_IP = "192.168.2.29";
    public static final int SERVER_PORT = 21;
    public static final String USERNAME = "uploader";
    public static final String PASSWORD = "up@loader";

    private static String encoding = System.getProperty("file.encoding");
    private static String className = Thread.currentThread().getStackTrace()[2].getClassName();
    private static final Logger logger = Logger.getLogger(className);

    public FtpApache () {
    }

    public FtpApache(String host, int port, String username, String password) {
        this.ip = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }
    /**
     * 服务器连接
     * @param ip 服务器IP
     * @param port 服务器端口
     * @param user 用户名
     * @param password 密码
     * @param path 服务器路径
     * @author 周玲斌
     * @date   2012-7-11
     */
    public void connectServer(String ip, int port, String user, String password, String path)
    {
        try {
            /* ******连接服务器的两种方法*******/
            //第一种方法
//            ftpClient = new FtpClient();
//            ftpClient.openServer(ip, port);
            //第二种方法
            ftpClient = new FtpClient(ip, port);

            ftpClient.login(user, password);
            // 设置成2进制传输
            ftpClient.binary();
            //System.out.println("login success!");
            if (path.length() != 0){
                //把远程系统上的目录切换到参数path所指定的目录
                ftpClient.cd(path);
            }
            ftpClient.binary();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    /**
     * 关闭连接
     * @author 周玲斌
     * @date   2012-7-11
     */
    public void closeConnect() {
        try {
            ftpClient.closeServer();
            //System.out.println("disconnect success");
        } catch (IOException ex) {
            //System.out.println("not disconnect");
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    /**
     * 上传文件
     * @param localFile 本地文件
     * @param remoteFile 远程文件
     * @author 周玲斌
     * @date   2012-7-11
     */
    public void upload(String localFile, String remoteFile) {
        this.localfilename = localFile;
        this.remotefilename = remoteFile;
        TelnetOutputStream os = null;
        FileInputStream is = null;
        try {
            //将远程文件加入输出流中
            os = ftpClient.put(this.remotefilename);
            //获取本地文件的输入流
            File file_in = new File(this.localfilename);
            is = new FileInputStream(file_in);
            //创建一个缓冲区
            byte[] bytes = new byte[1024];
            int c;
            while ((c = is.read(bytes)) != -1) {
                os.write(bytes, 0, c);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        } finally{
            try {
                if(is != null){
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(os != null){
                        os.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 下载文件
     * @param remoteFile 远程文件路径(服务器端)
     * @param localFile 本地文件路径(客户端)
     * @author 周玲斌
     * @date   2012-7-11
     */
    public boolean download(String remoteFile, String localFile) {
        boolean status = true;
        TelnetInputStream is = null;
        FileOutputStream os = null;
        try {
            //获取远程机器上的文件filename，借助TelnetInputStream把该文件传送到本地。
            is = ftpClient.get(remoteFile);
            File file_in = new File(localFile);
            os = new FileOutputStream(file_in);
            byte[] bytes = new byte[1024];
            int c;
            while ((c = is.read(bytes)) != -1) {
                os.write(bytes, 0, c);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            status = false;
            throw new RuntimeException(ex);
        } finally{
            try {
                if(is != null){
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                status = false;
            } finally {
                try {
                    if(os != null){
                        os.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    status = false;
                }
            }
        }
        return status;
    }

    public String getRemoteFile(String remotedir, String keyword) {
        List<String> dirlist = new ArrayList<String>();
        BufferedReader bufferedReader = null;
        String remotefilepath = "";
        String tempdir = "";
        try {
            ftpClient.cd(remotedir);
            bufferedReader = new BufferedReader(new InputStreamReader(ftpClient.nameList("")));
            while((tempdir = bufferedReader.readLine()) != null) {
                if (tempdir.contains(keyword)) {
                    dirlist.add(tempdir);
                }
            }
            if (dirlist.size() > 0) {
                remotefilepath = dirlist.get(dirlist.size() - 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return remotefilepath;
    }
}