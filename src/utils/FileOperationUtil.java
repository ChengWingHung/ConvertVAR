package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class FileOperationUtil {

	public FileOperationUtil() {
		
	}
	
	private static String currentYYYYMMDD = "";
	
	private static String outPutFileDir = "";
	
	private static String outPutFilePath = "";
	
	private static ArrayList<String> fileList;// 选择的所有文件
	
	/**
	 * 获取指定文件夹下的所有文件
	 * 
	 * @param fileDir
	 * @return ArrayList<String>
	 */
	public static ArrayList<String> getProcessFileList(File fileDir) {
		
		fileList = new ArrayList<String>();
		
		getDirectoryFileList(fileDir);
		
		return fileList;
	}
	
	/**
	 * 递归获取文件信息
	 * 
	 * @param fileDir
	 */
	private static void getDirectoryFileList(File fileDir) {
		
		File[] files = fileDir.listFiles();
		
		if (files != null) {
			
			for (File f : files) {
				if (f.isFile()) {
					fileList.add(f.toString());
				} else {
					getDirectoryFileList(f);
				}
			}
		}
	}
	
	/**
	 * 获取文件类型
	 * 
	 * @param fileDir
	 * @return String
	 */
	public static String getFileType(String fileName) {
		
		return fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length());
	}
	
	/**
	 * 先创建所有解析后的文件
	 * 
	 * @param savePathDir
	 * @param fileList
	 * @throws IOException
	 */
	public static void createResultFileList(String savePathDir, ArrayList<String> fileList) throws IOException {
		
		try {
			
			createResultFile(savePathDir, savePathDir);
			
			for(String filePath:fileList)
	        {
				createResultFile(savePathDir, filePath);
	        }
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 创建文件
	 * 
	 * @param savePathDir
	 * @param filePath
	 * @throws IOException
	 */
	public static void createResultFile(String savePathDir, String filePath) throws IOException{
		
		try {
			
			File file = new File(filePath);
			
			if (!file.exists()) {
				 
				if (!file.getParentFile().exists()) {
					
					String parentPath = file.getParentFile().getParent() + "/";
					
					createResultFile(savePathDir, parentPath);
					
					file.getParentFile().mkdir();
				}
				
				if (filePath.lastIndexOf('/') == filePath.length() -1) {
					file.mkdir();
				} else {
					file.createNewFile();
				}
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 读取文件内容
	 * 
	 * @param file
	 * @return String
	 * @throws IOException
	 */
	public static String readFileUsingInputStream(File file) throws IOException
	{
		
		StringBuffer buffer = new StringBuffer();
		
		BufferedReader br = new BufferedReader(new FileReader(file));;
		
		try {
			
            while (br.ready()) {
            	buffer.append(br.readLine() + "\n");
            }
            
        } catch(Exception e) {
			e.printStackTrace();
		} finally{
			
			if(br != null)
			{
				br.close();
			}
		}
		
		return buffer.toString();
		
	}
	
	/**
	 * 将内容写入文件
	 * 
	 * @param filePath
	 * @param fileContent
	 * @param append
	 * 
	 */
	public static void writeContentIntoFile(String filePath, String fileContent, Boolean append) {
		
		FileWriter writer = null;
		
        try {
        	
        	File file = new File(filePath);
        	
            writer = new FileWriter(file, append);
            
            writer.write(fileContent);
            
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
        	
            try {
                if(writer != null) {
                    writer.close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
	}
	
	/**
	 * 拷贝文件
	 * 
	 * @param copyFileList
	 * @param selectedFileDir
	 * @param outPutFileDir
	 * 
	 */
	public static void copySourceFileList(ArrayList<String> copyFileList, String selectedFileDir, String outPutFileDir) {
		
		try {
			
			String relativeFilePath = "";
			
			for(String filePath:copyFileList)
	        {
				relativeFilePath = filePath.substring(filePath.indexOf(selectedFileDir) + selectedFileDir.length(), filePath.length());
				
				ConvertLogUtil.printConvertLog("info", "拷贝文件:" + filePath);
				
				createResultFile(outPutFileDir, outPutFileDir + relativeFilePath);// 先创建目标文件
				
				copySourceFile(filePath, outPutFileDir + relativeFilePath);
	        }
		} catch(Exception e) {
	         e.printStackTrace();
	    }
	}
	
	/**
	 * 拷贝文件
	 * 
	 * @param sourceFilePath
	 * @param distinctFilePath
	 * @throws IOException
	 */
	public static void copySourceFile(String sourceFilePath, String distinctFilePath) throws IOException {
	    
		FileChannel sourceChannel = null;
	    FileChannel destChannel = null;
	    
	    try {
	    	
	        sourceChannel = new FileInputStream(sourceFilePath).getChannel();
	        
	        destChannel = new FileOutputStream(distinctFilePath).getChannel();
	        
	        destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
	        
	   } catch(Exception e) {
           e.printStackTrace();
       } finally {
	       sourceChannel.close();
	       destChannel.close();
	   }
	}
	
	/**
	 * 打印日志信息到本地
	 * 
	 * @param logContent
	 * 
	 */
	public static void printToolLogs(String logContent) {
		
		if ("".equals(currentYYYYMMDD)) {
			
			currentYYYYMMDD = TxtContentUtil.getCurrentYYYYMMDD();
			
			outPutFileDir = System.getProperty("user.dir") + "//";
			
			outPutFilePath = outPutFileDir + "ConvertVAR_" + currentYYYYMMDD + ".txt";
		}
		
		try {
			
			logContent = logContent + "\n";
			
			createResultFile(outPutFileDir, outPutFilePath);// 创建生成的文件
	    	
			writeContentIntoFile(outPutFilePath, logContent, true);//写入文件
			
		} catch(IOException err) {
			
		}
	}
}
