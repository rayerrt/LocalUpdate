package com.rayerrt.localupdate;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.cli.*;


/**
 *
 */
public class LocalUpdate {

	static final Logger logger = Logger.getLogger("com.rayerrt.localupdate.LocalUpdate");

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		String dailybuild = System.getProperty("user.dir");
		String testdir = "";
		String version = "";

		CommandLineParser parser = new BasicParser();
		Options options = new Options();
		options.addOption("t", "type", true, "指定测试文件夹");
		options.addOption("v", "version", true, "指定版本号");
    	options.addOption("h", "help", false, "显示帮助信息");
		try {
			final CommandLine commandline = parser.parse(options, args);
			if (commandline.hasOption("-t")) {
				testdir = commandline.getOptionValue("t");
			}
			if (commandline.hasOption("-v")) {
				version = commandline.getOptionValue("v");
			}
			if (commandline.hasOption("h")) {
				logger.info("java -jar LocalUpdate.jar [-t SystemTest] [-v 1.11.6]");
				System.exit(0);
			}
		} catch (MissingArgumentException e) {
			e.printStackTrace();
		}
		deleteOldFiles(dailybuild);
		
		List<String> devicesList;
		devicesList = getAdbDevices();
		if (devicesList.size() != 0) {
			for (String d : devicesList) {
				UpdateThread r = new UpdateThread(d, dailybuild, testdir, version);
				new Thread(r, d).start();
			}
		} else {
			logger.warning("没有获取到正在连接的adb设备");
		}
	}

	/**
	 * if necessary, restart adb-server
	 */
	private static boolean ensureAdbRunning() {
		boolean isRunning = false;
		try {
			Process pro = Runtime.getRuntime().exec("tasklist /v");
			BufferedInputStream in = new BufferedInputStream(pro.getInputStream());
			BufferedReader inBr = new BufferedReader(new InputStreamReader(in, "GBK"));
			String lineStr;
			while (null != (lineStr = inBr.readLine())) {
				if (lineStr.contains("adb.exe")) {
					isRunning =  true;
					break;
				}
			}
			if (! isRunning) {
				pro = Runtime.getRuntime().exec("adb start-server");
				if (pro.waitFor() == 0) {
					isRunning = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	return isRunning;
	}

	/**
	for get devices connected now
	@return
	 * @throws InterruptedException 
	 */
	private static List<String> getAdbDevices() {
		
	    List<String> als = new ArrayList<String>();
		String cmd = "adb devices";
		if(!ensureAdbRunning()) {
			System.out.println("Adb server is not running now.");
			System.exit(1);
		}
		Runtime mRuntime = Runtime.getRuntime();
		try {
			Process p = mRuntime.exec(cmd);
			BufferedInputStream mBis = new BufferedInputStream(p.getInputStream());
			BufferedReader mBr = new BufferedReader(new InputStreamReader(mBis));
            String lineStr;
            while (null != (lineStr = mBr.readLine())) {
            	if (0 != lineStr.length() && ! (lineStr.startsWith("List") && ! (lineStr.startsWith("* daemon")))) {
					lineStr = lineStr.replace("device", "").replace(" ", "").trim();
            		als.add(lineStr);
            	}
            }

            if (0 == p.waitFor()) {
    			logger.info("获取到正在连接的adb设备成功");
            } else {
    			logger.warning("没有获取到正在连接的adb设备失败");
            }
			mBis.close();
            mBr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return als;
	}

	/**
	 *
	 * @param localdir
	 */
	private static void deleteOldFiles(String localdir ) {
		File dir = new File(localdir);
		File[] files = dir.listFiles();
		for (File f : files) {
			try {
				if (f.getName().endsWith(UpdateThread.SRCZIPFORMAT)) {
					f.delete();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
	}
}
