package converttype;

import java.util.HashMap;
import java.util.Map;

import utils.ConvertLogUtil;
import utils.ReactProcessUtil;

public class ReactClassToFuncProcess {
	
	private static String parseFileName;// 当前要解析的文件名
	
	private static Map<String, Map> parseResultMap;// 解析后的信息存储对象

	public ReactClassToFuncProcess() {
		
	}
	
	public static String parseReactFileContent(String fileName, String parseResultContent) {
		
		// 判断是不是class 类组件
		if (!ReactProcessUtil.isReactClassFileContent(parseResultContent)) return parseResultContent;
		
		parseFileName = fileName;
		
		parseResultMap = new HashMap<>();
		
		ConvertLogUtil.printConvertLog("info", "解析前：\n" + parseResultContent);
		
		getReactClassName(parseResultContent);
		
		getReactStateInfo(parseResultContent);
		
		getReactPropsInfo(parseResultContent);
		
		getReactLifeCircleMethod(parseResultContent);
		
		getReactMethodInfo(parseResultContent);
		
		getReactReturnJSXContent(parseResultContent);
		
		String parseReactResultContent = getAssembleReactFuncContent();
		
		ConvertLogUtil.printConvertLog("info", "解析后：\n" + parseReactResultContent);
		
		return parseReactResultContent;
	}
	
	/**
	 * 得到class 类名
	 * 
	 * @param parseResultContent
	 */
	public static void getReactClassName(String parseResultContent) {
		
		
	}
	
	/**
	 * 得到state 状态信息
	 * 
	 * @param parseResultContent
	 */
	public static void getReactStateInfo(String parseResultContent) {
		
		
	}
	
	/**
	 * 得到props 信息
	 * 
	 * @param parseResultContent
	 */
	public static void getReactPropsInfo(String parseResultContent) {
		
		
	}
	
	/**
	 * 得到生命周期信息
	 * 
	 * @param parseResultContent
	 */
	public static void getReactLifeCircleMethod(String parseResultContent) {
		
		
	}
	
	/**
	 * 得到生命周期外的函数信息
	 * 
	 * @param parseResultContent
	 */
	public static void getReactMethodInfo(String parseResultContent) {
		
		
	}
	
	/**
	 * 得到return 的jsx 内容
	 * 
	 * @param parseResultContent
	 */
	public static void getReactReturnJSXContent(String parseResultContent) {
		
		
	}
	
	/**
	 * 组装react function 的内容
	 * 
	 * @param parseResultContent
	 */
	public static String getAssembleReactFuncContent() {
		
		String parseReactResultContent = "";
		
		
		
		return parseReactResultContent;
	}
	
	
}
