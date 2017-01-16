package com.pnunes.sabproxy;

import java.io.File;

public class Utils {
	private static String appDirName = ".sabproxy";
	
	public static void initializeUserSettings(){
		// create app folder
		File directory = new File(String.valueOf(getAppSettingFolder()));
	    if (!directory.exists()){
	        directory.mkdir();
	    }
	}
	
	private static String getUserHomeDir(){
		return System.getProperty("user.home");
	}
	
	public static String getAppSettingFolder(){
		return getUserHomeDir()+"/"+appDirName;
	}
}
