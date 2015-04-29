package com.rayerrt.ftp;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;

import sun.net.TelnetInputStream;
import sun.net.TelnetOutputStream;
import sun.net.ftp.FtpClient;

import java.util.List;
/**
 * Java1.6�Դ���API��FTP�Ĳ���
 * @Title:Ftp.java
 * @author: �����
 */
public class FtpApache {
    /**
     * �����ļ���
     */
    private String localfilename;
    /**
     * Զ���ļ���
     */
    private String remotefilename;
    /**
     * FTP�ͻ���
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
     * ����������
     * @param ip ������IP
     * @param port �������˿�
     * @param user �û���
     * @param password ����
     * @param path ������·��
     * @author �����
     * @date   2012-7-11
     */
    public void connectServer(String ip, int port, String user, String password, String path)
    {
        try {
            /* ******���ӷ����������ַ���*******/
            //��һ�ַ���
//            ftpClient = new FtpClient();
//            ftpClient.openServer(ip, port);
            //�ڶ��ַ���
            ftpClient = new FtpClient(ip, port);

            ftpClient.login(user, password);
            // ���ó�2���ƴ���
            ftpClient.binary();
            //System.out.println("login success!");
            if (path.length() != 0){
                //��Զ��ϵͳ�ϵ�Ŀ¼�л�������path��ָ����Ŀ¼
                ftpClient.cd(path);
            }
            ftpClient.binary();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    /**
     * �ر�����
     * @author �����
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
     * �ϴ��ļ�
     * @param localFile �����ļ�
     * @param remoteFile Զ���ļ�
     * @author �����
     * @date   2012-7-11
     */
    public void upload(String localFile, String remoteFile) {
        this.localfilename = localFile;
        this.remotefilename = remoteFile;
        TelnetOutputStream os = null;
        FileInputStream is = null;
        try {
            //��Զ���ļ������������
            os = ftpClient.put(this.remotefilename);
            //��ȡ�����ļ���������
            File file_in = new File(this.localfilename);
            is = new FileInputStream(file_in);
            //����һ��������
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
     * �����ļ�
     * @param remoteFile Զ���ļ�·��(��������)
     * @param localFile �����ļ�·��(�ͻ���)
     * @author �����
     * @date   2012-7-11
     */
    public boolean download(String remoteFile, String localFile) {
        boolean status = true;
        TelnetInputStream is = null;
        FileOutputStream os = null;
        try {
            //��ȡԶ�̻����ϵ��ļ�filename������TelnetInputStream�Ѹ��ļ����͵����ء�
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