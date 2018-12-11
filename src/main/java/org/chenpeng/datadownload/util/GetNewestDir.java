package org.chenpeng.datadownload.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
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
public class GetNewestDir {
	private String path = null;
	private ChannelSftp sftp = null;

	/**
	 * 
	 * @param sftp
	 *            创建对象时需要将sftp对象传递给本对象，本对象依靠sftp连接来获取远端目录
	 * 
	 */
	public GetNewestDir(ChannelSftp sftp, String sftpRootPath) {
		this.sftp = sftp;
		this.path = sftpRootPath;
	}

	/**
	 * 
	 * @param intervalDay
	 * @return
	 * @throws SftpException
	 * @throws ParseException
	 */

	public TreeMap<String, String> getNewestDir(int rangeDay) throws SftpException, ParseException {
		// 获取根路径下的文件列表
		Vector<LsEntry> vector = sftp.ls(path);
		// 建立一个TreeMap集合记录获取到的项目最新文件
		TreeMap<String, String> ptm = new TreeMap<String, String>();
		// 遍历获取到的文件夹列表
		Iterator<LsEntry> it = vector.iterator();

		while (it.hasNext()) {
			LsEntry le = it.next();
			// 获取项目名称命名的文件夹
			String projectName = le.getFilename();
			if (projectName.startsWith(".") || projectName.equals(".") || projectName.equals("..")
					|| !le.getLongname().startsWith("d")) {
				continue;// 如果名称为.或..或不是"d"说明不是目录文件，滤掉。
			} else {
				// 由于不确定项目是否上报了数据，因此初始化时先将最新文件夹设置为“Null”
				ptm.put(projectName, "Null");
				String projectPath = path + "/" + projectName + "/upload";
				// 获取项目文件夹下的文件列表
				Vector<LsEntry> vector2 = sftp.ls(projectPath);
				Iterator<LsEntry> it2 = vector2.iterator();

				// 定义一个记录最新文件夹的变量，与当前正在遍历的文件夹进行对比，哪个最新就选择哪个
				String newestDir = null;
				String newestDate = null;
				String fileName = null;
				String fileDate = null;
				DateFormat df = new SimpleDateFormat("MMM dd yyyy HH:mm", Locale.ENGLISH);
				while (it2.hasNext()) {
					LsEntry le2 = it2.next();
					fileName = le2.getFilename();
					String longFileName = le2.getLongname();

					// 判断如果获取到的文件名称为u“.”“..”或者文件的属性不是d开头（是d的才是文件夹）则跳过本次循环！
					if (fileName.startsWith(".") || fileName.equals(".") || fileName.equals("..")
							|| !longFileName.startsWith("d")) {
						continue;
					} else {

						// 将获取到的文件属性信息分裂为String数组
						String[] fileAttrs = longFileName.split(" +");
						// 其中日期是由数组中的5、6、7元素组成，中间加上了系统获取的年份！
						String day = fileAttrs[6].length() == 1 ? "0" + fileAttrs[6] : fileAttrs[6];
						fileDate = fileAttrs[5] + " " + day + " " + Calendar.getInstance().get(Calendar.YEAR) + " " + fileAttrs[7];

						// 判断逻辑：如果最新文件夹变量是空值，说明是第一次执行，所以直接将当前文件的名称和日期赋给相应变量。
						if (newestDir == null && newestDate == null) {
							newestDir = fileName;
							newestDate = fileDate;
						} else {
							// 判断逻辑：如果不是空值，说明已经不是第一次执行，因此要将最新的日期和当前日期进行比对，哪个新就将哪个赋值给newestDate、newestDir变量
							newestDate = df
									.format(df.parse(fileDate).before(df.parse(newestDate)) ? df.parse(newestDate)
											: df.parse(fileDate));
							newestDir = df.parse(fileDate).before(df.parse(newestDate)) ? newestDir : fileName;
						}
					}
				}
				
				Date currDate = new Date();

				// 获取文件属性中的最新修改日期后，与当前时间进行对比，如果超过了7天的差距则不选这个文件夹。（超过7天已经没有太大意义）
				if ((currDate.getTime() - df.parse(newestDate).getTime()) / (1000 * 3600 * 24) < rangeDay) {
					ptm.put(projectName, newestDir);
					System.out.println("【" + projectName + "】	获取到最新数据文件夹	【" + newestDir + "】");
				}
			}
		}
		return ptm;
	}
}
