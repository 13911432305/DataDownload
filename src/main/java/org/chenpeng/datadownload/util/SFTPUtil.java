package org.chenpeng.datadownload.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.io.IOUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException; 

/**
 * 
 * @author ChenPeng
 *
 */
public class SFTPUtil {
    //private transient Logger log = LoggerFactory.getLogger(this.getClass());  
    
    private ChannelSftp sftp;  
        
    private Session session;  
    /** SFTP 登录用户名*/    
    private String username; 
    /** SFTP 登录密码*/    
    private String password;  
    /** 私钥 */    
    private String privateKey;  
    /** SFTP 服务器地址IP地址*/    
    private String host;  
    /** SFTP 端口*/  
    private int port;  
    /** SFTP Channel*/  
    private Channel channel;
        
    
    /**
     * 构造基于密码认证的sftp对象
     * @param username
     * @param password
     * @param host
     * @param port
     */
    public SFTPUtil(String username, String password, String host, int port) {  
        this.username = username;  
        this.password = password;  
        this.host = host;  
        this.port = port;  
    } 
    
    public SFTPUtil(String username, String password, String host) {  
    	this(username, password, host, 22);
    } 
    
    /**  
     * 构造基于秘钥认证的sftp对象 
     */  
    public SFTPUtil(String username, String host, int port, String privateKey) {  
        this.username = username;  
        this.host = host;  
        this.port = port;  
        this.privateKey = privateKey;  
    }  
    
    
    @SuppressWarnings("unused")
	private SFTPUtil(){}  
    
    /**
     * 获取session
     */
    public Session getSession() {
    	return this.session;
    }
    
    /**
     * 获取channel对象
     * @return
     */
    
    public Channel getChannel() {
    	return this.channel;
    }
    
    /**
     * 获取sftp
     */
    public ChannelSftp getSftp() {
    	return this.sftp;
    }
    
    /** 
     * 连接sftp服务器 
     */  
    public void login(){  
        try {  
            JSch jsch = new JSch();  
            if (privateKey != null) {  
                jsch.addIdentity(privateKey);// 设置私钥  
            }  
    
            session = jsch.getSession(username, host, port);  
           
            if (password != null) {  
                session.setPassword(password);    
            }  
            Properties config = new Properties();  
            config.put("StrictHostKeyChecking", "no");  
                
            session.setConfig(config);  
            session.connect();  
              
            channel = session.openChannel("sftp");  
            channel.connect();  
    
            sftp = (ChannelSftp) channel;  
        } catch (JSchException e) {  
            e.printStackTrace();
        }  
    }    
    
    /** 
     * 关闭连接 server  
     */  
    public void logout(){  
        if (sftp != null) {  
            if (sftp.isConnected()) {  
                sftp.disconnect();  
            }  
        }  
        if (session != null) {  
            if (session.isConnected()) {  
                session.disconnect();  
            }  
        }  
    }  
 
    
    /**  
     * 将输入流的数据上传到sftp作为文件。文件完整路径=basePath+directory
     * @param basePath  服务器的基础路径 
     * @param directory  上传到该目录  
     * @param sftpFileName  sftp端文件名  
     * @param in   输入流  
     */  
    public void upload(String basePath,String directory, String sftpFileName, InputStream input) throws SftpException{  
        try {   
            sftp.cd(basePath);
            sftp.cd(directory);  
        } catch (SftpException e) { 
            //目录不存在，则创建文件夹
            String [] dirs=directory.split("/");
            String tempPath=basePath;
            for(String dir:dirs){
            	if(null== dir || "".equals(dir)) continue;
            	tempPath+="/"+dir;
            	try{ 
            		sftp.cd(tempPath);
            	}catch(SftpException ex){
            		sftp.mkdir(tempPath);
            		sftp.cd(tempPath);
            	}
            }
        }  
        sftp.put(input, sftpFileName);  //上传文件
    } 
 
    /** 
     * 下载文件。
     * @param directory 下载目录  
     * @param downloadFile 下载的文件 
     * @param saveFile 存在本地的路径 
     */    
    public void download(String directory, String downloadFile, String saveFile) throws SftpException, FileNotFoundException{  
        if (directory != null && !"".equals(directory)) {  
            sftp.cd(directory);  
        }  
        File file = new File(saveFile);  
        sftp.get(downloadFile, new FileOutputStream(file));  
    }  
    
    /**
     * 下载文件
     * @param directory 下载目录 
     * @param downloadFile 下载的文件名 
     * @return
     * @throws SftpException Sftp错误
     * @throws IOException IO错误
     */
    public byte[] download(String directory, String downloadFile) throws SftpException, IOException{  
        if (directory != null && !"".equals(directory)) {  
            sftp.cd(directory);  
        }  
        InputStream is = sftp.get(downloadFile);  
          
        byte[] fileData = IOUtils.toByteArray(is);  
          
        return fileData;  
    }  
    
    /**
     * 删除文件
     * @param directory 要删除文件所在目录 
     * @param deleteFile 要删除的文件 
     * @throws SftpException Sftp错误
     */
    public void delete(String directory, String deleteFile) throws SftpException{  
        sftp.cd(directory);  
        sftp.rm(deleteFile);  
    }  
    
    /**
     * 列出目录下的文件
     * @param directory
     * @return
     * @throws SftpException
     */
    public Vector<?> listFiles(String directory) throws SftpException {  
        return sftp.ls(directory);  
    }  
}

