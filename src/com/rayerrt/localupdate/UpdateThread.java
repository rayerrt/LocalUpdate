package com.rayerrt.localupdate;

import static java.util.logging.Logger.getLogger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.rayerrt.ftp.FtpApache;

/**
 *
 */
class UpdateThread implements Runnable {

	static String className = Thread.currentThread().getStackTrace()[2].getClassName();  
	protected static final Logger logger = getLogger(className);

	private static final char FLAG = '/';
	protected static final String SRCZIPFORMAT = "-update-full.zip";
	protected static final String UPDATETXT = "update.txt";
	
	private static final String DESTZIPFORMAT = "-ota-eng.compiler.zip";
	private static final String ADBCMDPREFIX = "adb -s ";
	private static final String GETPROPSUBCMD = " shell getprop ";
	private static final String AMBROADCAST = " shell am broadcast -a ";
	private static final String ANDROIDRUNTIMELOGCAT = " shell logcat -d -s AndroidRuntime:E";
	private static final String UPDATERECEIVER = "bbk.receiver.action.SYSTEM_UPDATE";
	private static final String ESEXTRA = " --es bbk.system.update.PACKAGE_NAME";
	private static final String EXEXTRA = " --ez bbk.system.update.IS_LOCAL ";
	private static final String DEVICEUDISK = " /mnt/sdcard/";
	private static final String UDISKMOUNTPOINT = " /storage/sdcard0/";
	
	private static final String UPDATERSCRIPT = "META-INF/com/google/android/updater-script";
	private static final String PROJECTPROPERTY = "ro.vivo.product.model";
	private static final String BUILDPRODUCTPROPERTY = "ro.build.product";
	private static final String HARDWAREVERPROPERTY = "ro.vivo.hardware.version";
	private static final String HARDWAREFLAG = "ro.hardware.bbk";
	
	private static final long WAITING_TIME = 300000;
	private static final long DURATION = 3000;
		
	private String device;
	private String path;

	public UpdateThread(String d, String path) {
		this.device = d;
		this.path = path;
	}
	
	@Override
	public void run() {
		/* TODO Auto-generated method stub */
		String projectName = getSystemPropValue(device, PROJECTPROPERTY);
		String hardwareVer = getSystemPropValue(device, HARDWAREVERPROPERTY);
		String product = getSystemPropValue(device, BUILDPRODUCTPROPERTY);
		String destZip = product + DESTZIPFORMAT;
		final char SPLASH = '"';
		ftpDownUpdateZip(projectName, this.path);
		//System.out.println("nihaoma");
		//System.exit(1);
		//String datePrefix = formatDateInfo("", "YYYY/MM/dd");
		File dir = new File(this.path);		
		File[] files = dir.listFiles();
		for (File f : files) {
			String filename = f.getName();
			if (filename.endsWith(SRCZIPFORMAT) && filename.contains(projectName)) {
				String srcZip = f.getAbsolutePath();
				if(!checkFileIntegrity(srcZip)) {
					logger.warning("check " + srcZip + " Integrity failed");
					break;
				}
				String projectVer = readInZip(srcZip, UPDATERSCRIPT);
				if (projectVer.contains(HARDWAREFLAG) && projectVer.contains("==")) {
					projectVer = projectVer.substring(projectVer.indexOf("==") + 2);
					projectVer = projectVer.substring(projectVer.indexOf(SPLASH) + 1, projectVer.lastIndexOf(SPLASH));
				}
				if (hardwareVer.equalsIgnoreCase(projectVer)) {
					pushZipToDevice(device, srcZip, destZip);
					sendSystemUpdateBroadcast(device, destZip, true);
					try {
						Thread.sleep(WAITING_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (projectName.equals(getSystemPropValue(device, PROJECTPROPERTY))) {
						if (traceDeviceLogCat(device)) {
							logger.info("Update DailyTest Succeeded!");
						} else {
							logger.warning(projectName + " Update DailyTest failed!");
						}
					} else {
						logger.warning(projectName + " Update DailyTest failed!");
					}
				}
				break;
			}
		}
	}
	private void ftpDownUpdateZip(String project, String localdir) {
		String ftpfilepath = "/images/" + project + "/DailyTest/";
		String localupdatetxt = project + UPDATETXT;
		boolean result = false;
		FtpApache ftp = new FtpApache();
		result = ftp.downSpecifiedFile(ftpfilepath, UPDATETXT, localdir, localupdatetxt);
		if(result) {
			File updatetxt = new File(localdir + File.separator + localupdatetxt);
			String updatepath = readFile(updatetxt);
			if (!updatepath.trim().isEmpty()) {
				int index = updatepath.indexOf("/images");
				updatepath = updatepath.substring(index);
				index = updatepath.lastIndexOf(FLAG);
				ftpfilepath = updatepath.substring(0, index);
				String ftpfilename = updatepath.substring(index + 1).replace("\n", "");
				String todayInfo = formatDateInfo("A", "MM.dd");
				if (ftpfilepath.contains(todayInfo)) {
					ftp.downSpecifiedFile(ftpfilepath, ftpfilename, localdir, ftpfilename);
				} else {
					logger.warning("\n****Find No Update Zip for " + project + " today!****\n");
				}
			}
		}
	}

	private String formatDateInfo(String prefix, String format) {
		String todayInfo = prefix;
		Date today = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		todayInfo += dateFormat.format(today);
		return todayInfo;
	}

	private String getSystemPropValue(String device, String key) {
		String value = "";
		String cmd = ADBCMDPREFIX + device + GETPROPSUBCMD + key;
		value = runCommandResult(cmd);
		return value;
	}
	
	private void pushZipToDevice(String device, String src, String dest) {
		String cmd = ADBCMDPREFIX + device + " push " + src + DEVICEUDISK + dest;
		runCommand(cmd);
	}
	
	private void sendSystemUpdateBroadcast(String device, String zipFile, boolean isLocal) {
		String local = "false";
		if (isLocal) {
			local = "true";
		}
		String cmd = ADBCMDPREFIX + device + AMBROADCAST + UPDATERECEIVER + ESEXTRA  + UDISKMOUNTPOINT + zipFile + EXEXTRA + local;
		runCommand(cmd);
	}
			
	private boolean traceDeviceLogCat(String device) {
		boolean result = true;
		String cmd = ADBCMDPREFIX + device + ANDROIDRUNTIMELOGCAT;
		String logcat =  runCommandResult(cmd);
		if (logcat.contains("E/AndroidRuntime")) {
			result = false;
		} 
		return result;
	}

	private void runCommand(String cmd) {
		logger.info(cmd);
		Runtime mRuntime = Runtime.getRuntime();				
		try {
			Process p = mRuntime.exec(cmd);
            if (0 != p.waitFor() && p.exitValue() == 0) {
				logger.warning("！！执行失败！！");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String runCommandResult(String cmd) {
		logger.info(cmd);
		String output = "";
		Runtime mRuntime = Runtime.getRuntime();				
		try {
			Process p = mRuntime.exec(cmd);
			BufferedInputStream mBis = new BufferedInputStream(p.getInputStream());
			BufferedReader mBr = new BufferedReader(new InputStreamReader(mBis, "utf-8"));
            String lineStr = null;
            while (null != (lineStr = mBr.readLine())) {
            	output += lineStr;
            }
            if (0 != p.waitFor() && p.exitValue() == 0) {
				logger.warning("！！执行失败！！");
			}
            mBis.close();
            mBr.close();
		} catch (Exception e) {
		}
		return output;		
	}
	
	private String readFile(File file) {
		String content = "";
        try {
    		InputStreamReader reader = new InputStreamReader(new FileInputStream(file),"UTF-8");
    		int fileLen = (int) file.length();
            char[] chars = new char[fileLen];
            reader.read(chars);
            content = String.valueOf(chars); 
            reader.close();
        } catch (Exception e) {
        	e.printStackTrace();
        }
		return content;		
	}
	
	private String readInZip(String filename, String mEntry) {  
        InputStream inputstream = null;
        String value = "";
        try {
            ZipFile zip = new ZipFile(filename);  
            ZipEntry entry =  zip.getEntry(mEntry);
            if(null != entry) {
                inputstream = zip.getInputStream(entry); 
            }
            if(null != inputstream) {
            	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputstream, "utf-8"));
            	String tmp;
            	while ((tmp = bufferedReader.readLine()) != null) {
            		if (tmp.contains(HARDWAREFLAG)) {
                    	value = tmp;
                    	break;
            		}
                }
                bufferedReader.close();
            }
           zip.close();
        } catch (ZipException e) {
			logger.warning("error in opening zip file" + filename);
        } catch (IOException e) {  
            e.printStackTrace();
        }     
        return value;        
    }
	
	private boolean checkFileIntegrity(String filename) {
		boolean state = false;
		File file = new File(filename);
		if (file.exists()) {
			int hashcode1 = file.hashCode();
			int hashcode2 = 0;
			while (hashcode1 != hashcode2) {
				hashcode2 = hashcode1;
				try {
					Thread.sleep(DURATION);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				hashcode1 = file.hashCode();
			}
			if (hashcode1 == hashcode2) {
				state = true;
			}
		}
		return state;
	}
}
