package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.ConvertParam;

public class TxtContentUtil {

	public TxtContentUtil() {
		
	}
	
	/**
	 * 获取html标签属性值
	 * 
	 * @param sourceText
	 * @param propsName
	 * @param endProps
	 * @return
	 */
	public static String getHtmlTagProps(String sourceText, String propsName, String endProps) {
		
		
		String resultText = sourceText.substring(sourceText.indexOf(propsName) + propsName.length(), sourceText.length());
		
		resultText = resultText.substring(0, resultText.indexOf(endProps) + endProps.length() - 1);
		
		return resultText;
	}
	
	/**
	 * 清除html标签属性值
	 * 
	 * @param sourceText
	 * @param propsName
	 * @param endProps
	 * @return
	 */
	public static String clearHtmlTagProps(String sourceText, String propsName, String endProps) {
		
		String resultText = sourceText.substring(sourceText.indexOf(propsName) + propsName.length(), sourceText.length());
		
		resultText = resultText.substring(0, resultText.indexOf(endProps) + endProps.length());
		resultText = sourceText.substring(resultText.indexOf(propsName) , resultText.indexOf(propsName) + propsName.length()) + resultText;
		
		return sourceText.replace(resultText, "");
	}
	
	/**
	 * 获取成对标签结束位置
	 * 
	 * @param sourceText
	 * @param startTag
	 * @param endTag
	 * @return
	 */
	public static int getTagEndIndex(String sourceText, char startTag, char endTag) {
		
		int startIndex = 0;
		int endIndex = 0;
		
		for (int i=0;i<sourceText.length();i++) {
			
			if (sourceText.charAt(i) == startTag) {
				startIndex++;
			}
			
			if (startTag == endTag) {
				if (startIndex == 2) {
					endIndex = i;
					break;
				}
			} else {
				
				if (sourceText.charAt(i) == endTag) {
					endIndex++;
				}
				
				if (startIndex != 0 && startIndex == endIndex) {
					endIndex = i;
					break;
				}
			}
		}
		
		return endIndex;
	}
	
	/**
	 * 获取语句结束索引
	 * 
	 * @param sourceText
	 * @return
	 */
	public static int getStatementEndIndex(String sourceText, int startIndex) {
		
		int endIndex = -1;
		
		for (int i = startIndex;i<sourceText.length();i++) {
			
			if (sourceText.charAt(i) == '\r' || sourceText.charAt(i) == '\n' || sourceText.charAt(i) == ';') {
				if (i == 0 && sourceText.charAt(i) != ';') continue;
				endIndex = i;
				break;
			}
			
			// 正好是末尾
			if (sourceText.length() - 1 == i) {
				endIndex = i;
			}
		}
		
		return endIndex;
	}
	
	/**
	 * 删除第一个为逗号的字符
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String deleteFirstComma(String sourceText, int startIndex) {
		
		if (',' == sourceText.substring(startIndex, sourceText.length()).trim().charAt(0)) {
			
			String tempText = sourceText.substring(startIndex, sourceText.length()).trim();
			sourceText = sourceText.substring(0, startIndex) + tempText.substring(1, tempText.length());
		}
		
		return sourceText;
	}
	
	/**
	 * 获取成对标签的整体内容信息
	 * 
	 * @param sourceText 要处理的内容
	 * @param startInex 截取开始位置
	 * @param startTag 起始标签
	 * @param endTag 结束标签
	 * @return
	 */
	public static String getContentByTag(String sourceText, int startInex, char startTag, char endTag) {
		
		int endIndex = 0;
		
		String tempText = sourceText.substring(startInex, sourceText.length());
		
		endIndex = getTagEndIndex(tempText, '{', '}') + 1;
		
		tempText = tempText.substring(0, endIndex);
		
		return tempText;
	}
	
	/**
	 * 清理整行为空的内容
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String clearBlankContentInLine(String sourceText) {
		
		int startIndex = -1;
		int endIndex = -1;
		
		for (int i=0;i<sourceText.length();i++) {
			
			if (startIndex == -1 && '\n' == sourceText.charAt(i)) {
				startIndex = i;
			} else if (startIndex != -1 && endIndex == -1 && '\n' == sourceText.charAt(i)) {
				endIndex = i;
			}
			
			if (startIndex != -1 && endIndex != -1) {
				
				if ("".contentEquals(sourceText.substring(startIndex, endIndex).trim())) {
					sourceText = sourceText.substring(0, startIndex + 1) + sourceText.substring(endIndex + 1, sourceText.length());
					i = startIndex;
				} else {
					
					startIndex = -1;
				}
				
				endIndex = -1;
				
			}
			
		}
		
		return sourceText;
	}
	
	/**
	 * 获取注释信息部分
	 * 
	 * @param sourceText
	 * @return 注释信息内容
	 */
	public static String getCommentInformation(String sourceText) {
		
		String tempText = "";
		String commentDescription = "";
		
		int endIndex = -1;// 获取截取结束位置
		
		if (sourceText.indexOf("/**") == 0) {
			
			tempText = sourceText.substring(sourceText.indexOf("/**"), sourceText.length());
			
			endIndex = TxtContentUtil.getTagEndIndex(sourceText, '/', '/') + 1;
			
			commentDescription = sourceText.substring(sourceText.indexOf("/**"), endIndex);
			
		} else if (sourceText.indexOf("//") == 0) {
			
			tempText = sourceText.substring(sourceText.indexOf("//"), sourceText.length());
			
			for (int i = 0;i<tempText.length();i++) {
				if (tempText.charAt(i) == '\n') {
					endIndex = i;
					break;
				}
			}
			
			commentDescription = sourceText.substring(sourceText.indexOf("//"), endIndex);
			
		}
		
		return commentDescription;
	}
	
	/**
	 * 得到不是变量命名位置的索引
	 * 
	 * @param sourceText
	 * @return
	 */
	public static int getNotVariableIndex(String sourceText) {
		
		int index = -1;
		
		for (int i = 0;i < sourceText.length();i++) {
			
			if (!String.valueOf(sourceText.charAt(0)).matches(ConvertParam.JS_VARIABLE_REG)) {
				index = i;
				break;
			}
		}
		
		return index;
	}
	
	/**
	 * 处理内容格式
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String processFileContentFormat(String sourceText) {
		
		sourceText = TxtContentUtil.clearBlankContentInLine(sourceText);// 处理整行都是空白的内容
		
		return sourceText;
	}
	
	/**
	 * 获取语句有实际字符开始的索引
	 * 
	 * @param sourceText
	 * @return
	 */
	public static int getStatementStartIndex(String sourceText) {
		
		int startIndex = 0;
		
		
		
		return startIndex;
	}
}
