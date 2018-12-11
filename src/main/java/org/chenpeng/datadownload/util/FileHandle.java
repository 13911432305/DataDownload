package org.chenpeng.datadownload.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;

/**
 * 
 * @author ChenPeng
 *
 */

public class FileHandle {
	// 定义私有变量sftp，因为所有方法均会用到，因此定义在类变量中。
	private ChannelSftp sftp = null;
	// 定义最终变量：sftp上的根目录
	private String ftpPath = null;
	// 定义最终变量：每个项目文件夹下都会有upload这个目录
	private String commFile = null;

	/**
	 * 对象创建时需要传入sftp对象，所有方法君用到
	 * 
	 * @param sftp
	 */
	public FileHandle(ChannelSftp sftp, String sftpRootPath, String sftpCommPath) {
		this.sftp = sftp;
		this.ftpPath = sftpRootPath;
		this.commFile = sftpCommPath;

	}

	/**
	 * 用于删除本地目录中的文件。删除功能在主类中也具备，但考虑到类的封装性，最好将功能集成到一个地方来使用。
	 * 
	 * @param file
	 */
	public void deleteFile(File file) {
		file.delete();
	}

	/**
	 * 用于下载单个文件！
	 * 
	 * @param path
	 * @param dir
	 * @throws FileNotFoundException
	 * @throws SftpException
	 */
	public void downloadFile(String path, File dir) throws FileNotFoundException, SftpException {
		String[] items = path.split("/");
		String projectName = items[3];
		File projectDir = new File(dir.getAbsolutePath() + System.getProperty("file.separator") + projectName);
		if (!projectDir.exists()) {
			projectDir.mkdirs();
			System.out.println("【" + projectDir.getName() + "】不存在，已新建！");
		}
		System.out.println("下载：" + path);
		sftp.get(path, new FileOutputStream(new File(dir.getAbsolutePath() + System.getProperty("file.separator")
				+ projectName + System.getProperty("file.separator") + items[items.length - 1])));
	}

	/**
	 * 用于将某个项目文件夹下的upload文件夹中的所有文件下载到本地，一般只用于报告周期内第一次使用该工具时，第一次以后均不会调用该方法
	 * 
	 * @param path
	 * @param projectDir
	 * @throws SftpException
	 * @throws FileNotFoundException
	 */
	public void downloadProjectAllFiles(String path, File projectDir) throws SftpException, FileNotFoundException {
		// 使用sftp进入需要下载的文件目录：格式应为 [/home/sftp/项目名称/upload]
		sftp.cd(path);
		// 获取目录下所有文件
		Vector<LsEntry> ve = sftp.ls(path);
		Iterator<LsEntry> it = ve.iterator();
		// 遍历获取到的文件列表
		while (it.hasNext()) {
			LsEntry le = it.next();
			String fileName = le.getFilename();
			File filePath = new File(projectDir.getAbsolutePath() + System.getProperty("file.separator") + fileName);
			if (!fileName.equals(".") && !fileName.equals("..") && !fileName.startsWith("d")) {
				// 如果是下载所有文件，请将下面的if筛选去掉！同步修改主程序中的对应项！
				sftp.get(fileName, new FileOutputStream(filePath));
				System.out.println(path + "/" + fileName);
			}
		}
	}

	/**
	 * 用于将本地路径下的文件与sftp中的文件进行对比，如果服务器上没有该文件，本地也无需保留，生成需要删除的文件列表！
	 * 
	 * @param dir
	 *            本地Data文件对象，即程序当前路径下以“KPIFolder”打头的文件夹的对象
	 * @param tm
	 * @return
	 * @throws SftpException
	 */
	public TreeMap<File,Integer> fileCompareU(File dir, TreeMap<String, String> tm) throws SftpException {
		TreeMap<File,Integer> al = new TreeMap<File,Integer>();
		// 获取本地Data文件路径下的项目级别文件夹对象
		File[] projectDirs = dir.listFiles();
		// 先做一个健壮性判断，假如根目录文件夹是新建的，里面没有任何文件夹，则一定会给projectDirs返回null值。假如为空，则提示不需对比！
		if (projectDirs == null || projectDirs.length == 0) {
			System.out.println("数据根目录下没有文件，不需要上行对比！");
		} else {
			// 项目文件夹获取没问题后，开始逐个遍历获取到的项目文件夹
			for (File projectDir : projectDirs) {
				// 文件中可能会有多余的文档（非文件夹）出现，为了避免这种情况，需要健壮性判断，是文件夹则继续执行，不是文件夹则跳过！
				if (!projectDir.isDirectory()) {
					continue;
				} else {
					// 成功进入到项目文件夹后，我们要获取文件夹下所有文件！
					File[] files = projectDir.listFiles();
					// 健壮性判断，如果files变量为null说明在该项目文件夹下没有任何文件，直接跳到下次循环！
					if (files == null || files.length == 0) {
						// System.out.println("【" + projectDir.getName() + "】文件夹没有文件！");
						continue;
					} else {
						String newestDirName = tm.get(projectDir.getName());
						for (File file : files) {
							// 判断如果是文件夹，跳到下次循环
							if (file.isDirectory()) {
								continue;
							} else {
								Vector<LsEntry> ve = null;
								// 获取到具体的文件对象后，与sftp上相应目录下的文件进行对比，如果服务器上没有该文件则将该文件对象添加到ArryList中。

								if (newestDirName == null || newestDirName.equals("Null")) {
									al.put(file, 0);
								} else {

									String ftpProPath = ftpPath + "/" + projectDir.getName() + commFile + "/"
											+ newestDirName;
									sftp.cd(ftpProPath);
									// 如果服务器上没有这个文件将会报错，所以我们通过抓取错误信息判断这个文件是否存在
									try {
										ve = sftp.ls(ftpProPath + "/" + file.getName());
									} catch (SftpException e) {
										if (e.toString().contains("No such file")) {
											al.put(file, 1);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return al;
	}

	public ArrayList<String> fileCompareD(File dir, TreeMap<String, String> tm) throws SftpException {
		ArrayList<String> al = new ArrayList<String>();

		Set<Entry<String, String>> set = tm.entrySet();
		Iterator<Entry<String, String>> it = set.iterator();

		while (it.hasNext()) {
			Entry<String, String> en = it.next();
			if (en.getValue().equals("Null")) {
				continue;
			} else {
				String sftpPath = ftpPath + "/" + en.getKey() + commFile + "/" + en.getValue();
				// 获取sftp路径下的文件列表！
				Vector<LsEntry> ve = sftp.ls(sftpPath);
				Iterator<LsEntry> its = ve.iterator();
				while (its.hasNext()) {
					LsEntry le = its.next();
					String sftpFileName = le.getFilename();
					String sftpFileLongName = le.getLongname();
					if (sftpFileName.startsWith(".") || sftpFileLongName.startsWith("d")) {
						continue;
					} else {
						File localFile = new File(dir.getAbsolutePath() + System.getProperty("file.separator")
								+ en.getKey() + System.getProperty("file.separator") + sftpFileName);
						if (localFile.exists()) {
							long sftpFileSize = Long.parseLong(sftpFileLongName.split(" +")[4]);
							long localFileSize = localFile.length();
							if (sftpFileSize == localFileSize) {
								continue;
							} else {
								al.add(sftpPath + "/" + sftpFileName);
							}
						} else {
							al.add(sftpPath + "/" + sftpFileName);
						}
					}
				}
			}
		}
		return al;
	}
}
