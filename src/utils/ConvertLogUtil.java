package utils;

import common.ConvertParam;

public class ConvertLogUtil {
	
	public ConvertLogUtil() {
		
	}
	
	public static void printConvertLog(String type, String logContent) {
		
		if (ConvertParam.IS_DEV_FLAG) {
			
			System.out.println(type + ":" + logContent);
		} else {
			
			// async 待处理
			FileOperationUtil.printToolLogs(type + ":" + logContent);
		}
	}
}
