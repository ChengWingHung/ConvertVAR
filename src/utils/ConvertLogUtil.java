package utils;

import common.ConvertParam;

public class ConvertLogUtil {
	
	public ConvertLogUtil() {
		
	}
	
	public static void printConvertLog(String type, String logContent) {
		
		if (ConvertParam.IS_DEV_FLAG || "local".equals(type)) {
			
			System.out.println(type + ":" + logContent);
		} else {
			
			FileOperationUtil.printToolLogs(type + ":" + logContent);
		}
	}
}
