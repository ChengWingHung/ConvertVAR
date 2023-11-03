package utils;

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
	 * 得到符合变量命名位置的索引
	 * 
	 * @param sourceText
	 * @return
	 */
	public static int getVariableStartIndex(String sourceText, int startIndex) {
		
		int index = -1;
		
		for (int i = startIndex;i < sourceText.length();i++) {
			
			if (String.valueOf(sourceText.charAt(i)).matches(ConvertParam.JS_VARIABLE_REG)) {
				index = i;
				break;
			}
		}
		
		return index;
	}
	
	/**
	 * 得到不是变量命名位置的索引
	 * 
	 * @param sourceText
	 * @return
	 */
	public static int getNotVariableIndex(String sourceText, int startIndex) {
		
		int index = -1;
		
		for (int i = startIndex;i < sourceText.length();i++) {
			
			if (!String.valueOf(sourceText.charAt(i)).matches(ConvertParam.JS_VARIABLE_REG)) {
				index = i;
				break;
			}
		}
		
		return index;
	}
	
	/**
	 * 处理内容格式
	 * 
	 * @param sourceText 要处理的内容
	 * @param fileType 文件类型
	 * @param framworkName 来自vue 还是react
	 * @return
	 */
	public static String processFileContentFormat(String sourceText, String fileType, String framworkName) {
		
		String tempText = "";
		String resultContent = "";
		
		if ("vue".equals(framworkName)) {
			
			if ("js".equals(fileType)) {
				
				sourceText = clearLineStartBlankContent(sourceText);
				
				sourceText = processJSContentFormat(sourceText, 2);
			} else {
				
				if (sourceText.indexOf("<script") > -1 && sourceText.indexOf("</script") > -1) {
					
					tempText = sourceText.substring(0, sourceText.indexOf("<script"));
					
					// 清除掉<script 前的空格
					if (tempText.indexOf('\n') > -1 && "".equals(tempText.substring(tempText.indexOf('\n') + 1, tempText.length()).trim())) {
						
						for (int j=tempText.length() - 1;j > -1;j--) {
							if (' ' == tempText.charAt(j)) {
								tempText = tempText.substring(0, j) + tempText.substring(j + 1, tempText.length());
								j++;
								continue;
							}
							
							break;
						}
						
						sourceText = tempText + sourceText.substring(sourceText.indexOf("<script"), sourceText.length());
					}
					
					tempText = sourceText.substring(sourceText.indexOf("<script"), sourceText.indexOf("</script"));
						
					tempText = tempText.substring(tempText.indexOf('>') + 1, tempText.length());
					
					resultContent = tempText;
					
					tempText = clearLineStartBlankContent(tempText);
					
					tempText = processJSContentFormat(tempText, 4);
					
					sourceText = sourceText.replace(resultContent, tempText + "\n");
					
				} else {
					
					sourceText = clearLineStartBlankContent(sourceText);
					
					sourceText = processJSContentFormat(sourceText, 2);
				}
			}
		}
		
		return sourceText;
	}
	
	/**
	 * 清空每行的头部空格部分
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String clearLineStartBlankContent(String sourceText) {
		
		String tempText = "";
		
		// 开头就是空格去除
		if (sourceText.indexOf(' ') == 0) sourceText = sourceText.trim();
		
		// 判断换行标志
		if (sourceText.indexOf('\n') > -1) {
			
			tempText = sourceText.substring(sourceText.indexOf('\n') + 1, sourceText.length()).trim();
			
			if (tempText.indexOf('\n') > -1) {
				
				sourceText = sourceText.substring(0, sourceText.indexOf('\n') + 1) + clearLineStartBlankContent(tempText);
			} else {
				
				sourceText = sourceText.substring(0, sourceText.indexOf('\n') + 1) + tempText;
			}
			
		}
		
		return sourceText;
	}
	
	/**
	 * 处理JS内容格式
	 * 
	 * @param sourceText 要处理js的内容
	 * @param indentCount 添加的空格数
	 * @return
	 */
	public static String processJSContentFormat(String sourceText, int indentCount) {
		
		// 不需要处理直接返回结果信息
		if ("".equals(sourceText) || sourceText.indexOf('\n') < 0) {
			
			if (!"".equals(sourceText)) sourceText = getIndentCountResutl(indentCount) + sourceText;
			
			return sourceText;
		}
		
		String tempText = "";
		String lineContent = "";
		
		int endIndex = -1;
		
		lineContent = sourceText.substring(0, sourceText.indexOf('\n') + 1);
		
		if (lineContent.indexOf('{') > -1) {
			
			tempText = sourceText.substring(lineContent.indexOf('{'), sourceText.length());
			
			endIndex = getTagEndIndex(tempText, '{', '}') + lineContent.indexOf('{');
			
			if (lineContent.indexOf('{') + 1 < endIndex) {
				
				tempText = sourceText.substring(lineContent.indexOf('{') + 1, endIndex);
				
				if (tempText.indexOf('\n') > -1) {
					
					return getIndentCountResutl(indentCount) + lineContent.substring(0, lineContent.indexOf('{') + 1) + processJSContentFormat(tempText, indentCount + 2) + processJSContentFormat(sourceText.substring(endIndex, sourceText.length()), indentCount);
				}
			}
			
		}
		
		sourceText = getIndentCountResutl(indentCount) + lineContent + processJSContentFormat(sourceText.substring(sourceText.indexOf('\n') + 1, sourceText.length()), indentCount);// 添加空格
		
		return sourceText;
	}
	
	/**
	 * 添加空格
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String getIndentCountResutl(int indentCount) {
		
		String blankResult = "";
		
		for (int i=0;i<indentCount;i++) {
			blankResult += " ";
		}
		
		return blankResult;
	}
	
	/**
	 * 按命名规则重命名变量，不符合部分直接剔除
	 * 
	 * @param variableName 变量名
	 * @return
	 */
	public static String reNameVariable(String variableName) {
		
		for (int i = 0;i < variableName.length();i++) {
			
			if (!String.valueOf(variableName.charAt(i)).matches(ConvertParam.JS_VARIABLE_REG)) {
				variableName = variableName.replaceAll(String.valueOf(variableName.charAt(i)), "");
				i--;
			}
		}
		
		return variableName;
	}
	
	/**
	 * 获取字符串信息开始的索引
	 * 
	 * @param sourceText
	 * @return
	 */
	public static int getStringStartIndex(String sourceText, String tagName) {
		
		int startIndex = 0;
		
		if (sourceText.indexOf(tagName) > -1 && "".equals(sourceText.substring(0, sourceText.indexOf(tagName)))) {
			
			startIndex = sourceText.indexOf(tagName);
		}
		
		return startIndex;
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
