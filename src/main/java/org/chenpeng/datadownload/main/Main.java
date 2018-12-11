package org.chenpeng.datadownload.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.chenpeng.datadownload.util.FileHandle;
import org.chenpeng.datadownload.util.GetNewestDir;
import org.chenpeng.datadownload.util.SFTPUtil;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

/**
 * 
 * @author ChenPeng
 *
 */

public class Main {

	private static FileHandle fh;
	private static String user;
	private static String passwd;
	private static String host;
	private static String dataPath;
	private static String sftpRootPath;
	private static String sftpCommPath;
	private static int rangeDay;
	/**
	 * 类加载时即将配置信息载入程序中！
	 */
	static {

		String configFilePath = System.getProperty("user.dir") + System.getProperty("file.separator") + "Config"
				+ System.getProperty("file.separator") + "config.properties";

		System.out.println("加载配置信息：" + configFilePath);
		System.out.println("------------------------------------------------------------------------");
		File configFile = new File(configFilePath);
		FileInputStream fis = null;
		Properties prop = null;
		try {
			fis = new FileInputStream(configFile);
			prop = new Properties();
			prop.load(fis);

			if (prop.getProperty("user") == null) {
				throw new RuntimeException("请设置/检查【user】参数！");
			} else {
				user = prop.getProperty("user");
				System.out.println("user = " + user);
			}

			if (prop.getProperty("passwd") == null) {
				throw new RuntimeException("请设置/检查【passwd】参数！");
			} else {
				passwd = prop.getProperty("passwd");
				System.out.println("passwd = ******");
			}

			if (prop.getProperty("host") == null) {
				throw new RuntimeException("请设置/检查【host】参数！");
			} else {
				host = prop.getProperty("host");
				System.out.println("host = " + host);
			}

			if (prop.getProperty("dataPath") == null) {
				throw new RuntimeException("请设置/检查【dataPath】参数！");
			} else {
				dataPath = prop.getProperty("dataPath");
				System.out.println("dataPath = " + dataPath);
			}

			if (prop.getProperty("sftpRootPath") == null) {
				throw new RuntimeException("请设置/检查【sftpRootPath】参数！");
			} else {
				sftpRootPath = prop.getProperty("sftpRootPath");
				System.out.println("sftpRootPath = " + sftpRootPath);
			}

			if (prop.getProperty("sftpCommPath") == null) {
				throw new RuntimeException("请设置/检查【sftpCommPath】参数！");
			} else {
				sftpCommPath = prop.getProperty("sftpCommPath");
				System.out.println("sftpCommPath = " + sftpCommPath);
			}

			if (prop.getProperty("rangeDay") == null) {
				throw new RuntimeException("请设置/检查【rangeDay】参数！");
			} else {
				try {
					rangeDay = Integer.parseInt(prop.getProperty("rangeDay"));
					System.out.println("rangeDay = " + rangeDay);
				} catch (NumberFormatException e) {
					throw new RuntimeException("请检查输入的【rangeDay】是否为数字！");
				}
			}
			System.out.println();
			System.out.println("配置信息加载完毕！");
		} catch (NullPointerException e) {
			throw new RuntimeException("未找到配置文件！【" + configFilePath + "】");
		} catch (IOException e) {
			e.printStackTrace();
			;
		} finally {
			try {
				fis.close();
			} catch (IOException e) {

			} finally {
				fis = null;
			}
		}

	}

	/**
	 * 
	 * @param args
	 * @throws SftpException
	 * @throws ParseException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws SftpException, ParseException, FileNotFoundException {

		System.out.println("开始登陆sftp服务器！");
		SFTPUtil su = new SFTPUtil(user, passwd, host);
		su.login();
		System.out.println("------------------------------------------------------------------------");
		System.out.println();
		System.out.println("服务器登陆成功！");
		ChannelSftp sftp = su.getSftp();

		System.out.println("开始获取[服务器]上[项目]最新数据文件夹列表！");
		System.out.println("------------------------------------------------------------------------");

		GetNewestDir getND = new GetNewestDir(sftp, sftpRootPath);
		TreeMap<String, String> tm = getND.getNewestDir(rangeDay);
		System.out.println();
		System.out.println("获取[服务器]上[项目]最新数据文件夹列表完成！");

		fh = new FileHandle(sftp, sftpRootPath, sftpCommPath);

		String dataRootDirName = null;
		File dataRootDir = null;
		// 在此判断程序根目录下有未一个“KPIFolder”打头的文件夹,有则继续执行，未则新建一个
		File dataDir = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + dataPath);

		if (!dataDir.exists()) {
			dataDir.mkdir();
			System.out.println("未检测到" + dataPath + "文件夹，已新建！");
		}

		File[] allFiles = dataDir.listFiles();

		// 进行健壮性判断，如果的值是null，说明Data文件夹下未任何文件，则需要先创建！如果不为空说明有文件存在，但不能保证一定有“KPIFolder”这个文件夹
		try {
			if (allFiles == null || allFiles.length == 0) {
				// 创建一个包含“KPIFolder”的文件夹！
				System.out.println("在" + dataPath + "文件夹下未发现包含有【KPIFolder】字样的文件夹！立即创建：");
				dataRootDirName = dataDir + System.getProperty("file.separator") + "KPIFolder"
						+ Calendar.getInstance().get(Calendar.YEAR) + (Calendar.getInstance().get(Calendar.MONTH) + 1)
						+ Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
				dataRootDir = new File(dataRootDirName);
				// 虽然为空，我们仍不确定这个文件夹到底存在不存在，考虑健壮性我们再次做是否存在的判断！
				if (!dataRootDir.exists()) {
					dataRootDir.mkdirs();
					System.out.println("创建成功！");
				}
				System.out.println("初始化完成，开启下载模式：");
				System.out.println("------------------------------------------------------------------------");
				initDir(tm, dataRootDir);
				System.out.println();
				System.out.println("数据文件下载完成！");
			} else {
				// 遍历文件夹下所有的文件，如果有"KPIFolder"字样的文件夹，则将其赋值给dataRootDirName和dataRootDir
				for (File file : allFiles) {
					if (file.getName().contains("KPIFolder")) {
						dataRootDirName = file.getAbsolutePath();
						System.out.println("发现文件夹：");
						System.out.println("------------------------------------------------------------------------");
						System.out.println(dataRootDirName);
						dataRootDir = file;
						break;
					}
				}

				// 经过上方一轮处理后，如果dataRootDirName和dataRootDir仍是空值，那说明存在一种情况，就是数据文件夹下存在文件，但并不包含“KPIFolder”
				if (dataRootDirName == null || dataRootDir == null) {
					// 创建带有“KPIFolder”字样的文件夹
					dataRootDirName = dataDir + System.getProperty("file.separator") + "KPIFolder"
							+ Calendar.getInstance().get(Calendar.YEAR)
							+ (Calendar.getInstance().get(Calendar.MONTH) + 1)
							+ Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
					dataRootDir = new File(dataRootDirName);
					// 虽然为空，我们仍不确定这个文件夹到底存在不存在，考虑健壮性我们再次做是否存在的判断！
					if (!dataRootDir.exists()) {
						System.out.println("在" + dataPath + "文件夹下未发现包含有【KPIFolder】字样的文件夹！立即创建：");
						dataRootDir.mkdirs();
						System.out.println("创建成功！");
					}

					// 使用初始化方法，第一次将文件下载到本地
					System.out.println(dataRootDir.getAbsolutePath());
					System.out.println("初始化完成，开启下载模式：");
					System.out.println("------------------------------------------------------------------------");
					initDir(tm, dataRootDir);
					System.out.println();
					System.out.println("数据文件下载完成！");

				} else {

					//
					System.out.println("开始进行上行文件比对！");
					System.out.println("------------------------------------------------------------------------");

					TreeMap<File, Integer> al = fh.fileCompareU(dataRootDir, tm);
					System.out.println();
					System.out.println("上行文件比对完成！");

					if (al != null && al.size() != 0) {
						System.out.println("无效文件即将删除：");
						System.out.println("------------------------------------------------------------------------");
						for (int i = 0; i < al.size(); i++) {

						}
						Set<Entry<File, Integer>> set = al.entrySet();
						Iterator<Entry<File, Integer>> it = set.iterator();
						while (it.hasNext()) {
							Entry<File, Integer> en = it.next();
							if (en.getValue() == 0) {
								System.out.println("【非最新】删除：" + en.getKey().getAbsolutePath());
								fh.deleteFile(en.getKey());
							} else {
								System.out.println("【不存在】删除：" + en.getKey().getAbsolutePath());
								fh.deleteFile(en.getKey());
							}

						}
						System.out.println();
						System.out.println("无效文件已删除！");
					} else {
						System.out.println();
						System.out.println("与sftp服务器文件一致，无需删除！");
					}

					System.out.println("开始进行下行文件比对！");
					System.out.println("------------------------------------------------------------------------");

					ArrayList<String> al2 = fh.fileCompareD(dataRootDir, tm);
					System.out.println();
					System.out.println("下行文件比对完成！");

					if (al2 != null && al2.size() != 0) {
						System.out.println("开始下载最近更新的文件！");
						System.out.println("------------------------------------------------------------------------");
						for (int j = 0; j < al2.size(); j++) {
							// 如果是下载所有文件请将if条件删除。下载整个文件夹那个操作记得同步修改！
							fh.downloadFile(al2.get(j), dataRootDir);
						}
						System.out.println();
						System.out.println("文件下载完成！");
					} else {
						System.out.println();
						System.out.println("无文件更新，未下载任何文件！");
					}

				}

			}
			System.out.println("检测根目录下【空文件夹】和【非文件夹】，并删除：");
			System.out.println("------------------------------------------------------------------------");
			deleteEmptyFolder(dataRootDir);
			System.out.println("完成！");
		} finally {
			System.out.println();
			System.out.println("登出sftp服务器！");
			// 初始化完成之后
			su.logout();
			System.out.println();
		}
	}

	private static void initDir(TreeMap<String, String> tm, File dataRootDir)
			throws SftpException, FileNotFoundException {

		Set<Entry<String, String>> es = tm.entrySet();
		Iterator<Entry<String, String>> it = es.iterator();
		while (it.hasNext()) {
			Entry<String, String> en = it.next();
			if (en.getValue().equals("Null")) {
				continue;
			} else {
				File projectDir = new File(
						dataRootDir.getAbsolutePath() + System.getProperty("file.separator") + en.getKey());
				projectDir.mkdir();
				System.out.println("下载【" + en.getKey() + "】项目数据");
				System.out.println("------------------------------------------------------------------------");
				fh.downloadProjectAllFiles(sftpRootPath + "/" + en.getKey() + sftpCommPath + "/" + en.getValue(),
						projectDir);
				System.out.println();
				System.out.println("完成！");
			}
		}
	}

	private static void deleteEmptyFolder(File dir) {

		File[] projectDirs = dir.listFiles();
		for (File projectDir : projectDirs) {
			if (!projectDir.isDirectory()) {
				projectDir.delete();
				System.out.println("【" + projectDir.getName() + "】为非文件夹，已删除！");
			} else {
				if (projectDir.listFiles() == null || projectDir.listFiles().length == 0) {
					projectDir.delete();
					System.out.println("【" + projectDir.getName() + "】文件夹无数据，已删除！");
				}
			}
		}

	}
}
