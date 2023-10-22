package utils;

public class FileOperationUtil {

	public FileOperationUtil() {
		
	}
	
	/**
	 * 获取文件类型
	 * 
	 */
	public static String getFileType(String fileName) {
		
		return fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length());
	}
	
	/**
	 * 生成文件
	 * 
	 */
	public static void generateNewFile() {
		
	}
	
	/**
	 * 拷贝文件
	 * 
	 */
	public static void copySourceFile() {
		
	}
	
	/**
	 * 生成tar包
	 * 
	 */
	public static void generateTarFile() {
		
	}
}
