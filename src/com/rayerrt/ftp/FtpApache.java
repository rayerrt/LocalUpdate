package com.rayerrt.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

public class FtpApache {
    private static FTPClient ftpClient = new FTPClient();
    private static String encoding = System.getProperty("file.encoding");
    private static final String SERVER_IP = "192.168.2.29";
    private static final int SERVER_PORT = 21;
    private static final String USERNAME = "uploader";
    private static final String PASSWORD = "up@loader";

    public static final Logger logger = Logger.getLogger("com.ray.ftp.FtpApache");

    public boolean downFile(String url, int port, String username,
            String password, String remotePath, String fileName, String localPath, String localName) {
        boolean result = false;
        try {
            int reply;
            ftpClient.connect(url, port);
            ftpClient.login(username, password);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            reply = ftpClient.getReplyCode();
            ftpClient.setBufferSize(1024);
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                logger.warning("FTP server refused connection.");
                return result;
            }
            ftpClient.changeWorkingDirectory(new String(remotePath.getBytes(encoding), "utf-8"));
            FTPFile[] fs = ftpClient.listFiles();
            for (FTPFile ff : fs) {
                if (ff.getName().equals(fileName)) {
                    File localFile = new File(localPath + File.separator + localName);
                    OutputStream is = new FileOutputStream(localFile);
                    ftpClient.retrieveFile(ff.getName(), is);
                    is.close();
                    break;
                }
            }
            ftpClient.logout();
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        return result;
    }

    public boolean downSpecifiedFile(String ftpfilepath, String ftpfile, String localfilepath, String localfile) {
    	boolean flag = false;
        try {
            flag = downFile(SERVER_IP, SERVER_PORT, USERNAME, PASSWORD,
            		ftpfilepath, ftpfile, localfilepath, localfile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(flag) {
            System.out.println("download successfully!");
        }
        return flag;
    }
}
