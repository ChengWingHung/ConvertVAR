package utils;

public class ReactProcessUtil {

	public ReactProcessUtil() {
		
	}
	
	/**
	 * 判断文件内容是否是react class 组件
	 * 
	 * @param sourceText
	 * @return
	 */
	public static Boolean isReactClassFileContent(String sourceText) {
		
		return sourceText.indexOf(" class ") > -1;
	}
}
