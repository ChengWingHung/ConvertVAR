package utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import common.ConvertParam;

public class TxtContentUtil {

	public TxtContentUtil() {
		
	}
	
	private static int keyWordIndex = -1;
	
	/**
	 * 获取html 标签属性值
	 * 
	 * @param sourceText
	 * @param propsName
	 * @param endProps
	 * @return String
	 */
	public static String getHtmlTagProps(String sourceText, String propsName, String endProps) {
		
		String resultText = sourceText.substring(sourceText.indexOf(propsName) + propsName.length(), sourceText.length());
		
		resultText = resultText.substring(0, resultText.indexOf(endProps) + endProps.length() - 1);
		
		return resultText;
	}
	
	/**
	 * 清除html 标签属性值
	 * 
	 * @param sourceText
	 * @param propsName
	 * @param endProps
	 * @return String
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
	 * @return int
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
	 * @param startIndex
	 * @return int
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
	 * @param startIndex
	 * @return String
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
	 * @return String
	 */
	public static String getContentByTag(String sourceText, int startInex, char startTag, char endTag) {
		
		int endIndex = 0;
		
		String tempText = sourceText.substring(startInex, sourceText.length());
		
		endIndex = getTagEndIndex(tempText, startTag, endTag) + 1;
		
		tempText = tempText.substring(0, endIndex);
		
		return tempText;
	}
	
	/**
	 * 清理整行为空的内容
	 * 
	 * @param sourceText
	 * @return String
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
	 * @return String 注释信息内容
	 */
	public static String getCommentInformation(String sourceText) {
		
		String tempText = "";
		String commentDescription = "";
		
		int endIndex = -1;// 获取截取结束位置
		
		if (sourceText.indexOf("<--") == 0) {
			
			tempText = sourceText.substring(sourceText.indexOf("<--"), sourceText.length());
			
			commentDescription = tempText.substring(0, tempText.indexOf("-->") + 3);
			
		} else if (sourceText.indexOf("/**") == 0) {
			
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
			
			commentDescription = sourceText.substring(sourceText.indexOf("//"), endIndex == -1?tempText.length():endIndex);
			
		}
		
		return commentDescription;
	}
	
	/**
	 * 得到符合变量命名位置的索引
	 * 
	 * @param sourceText
	 * @param startIndex
	 * @return int
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
	 * @param startIndex
	 * @return int
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
	 * @return String
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
	 * @return String
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
	 * 处理文件内容格式
	 * 
	 * @param sourceText 要处理js的内容
	 * @param indentCount 添加的空格数
	 * @return String
	 */
	public static String processFileContentFormat(String fileContent, int indentCount) {
		
		// 不需要处理直接返回结果信息
		if ("".equals(fileContent) || fileContent.indexOf('\n') < 0) {
			
			if (!"".equals(fileContent)) fileContent = getIndentCountResutl(indentCount) + fileContent;
			
			return fileContent;
		}
		
		String tempText = "";
		
		// 先每行增加indentCount 数
		if (indentCount != 0) {
			
			tempText = getIndentCountResutl(indentCount);
			
			String resultText = "";
			
			for (String lineContent:fileContent.split("\n")) {
				
				resultText += tempText + lineContent + "\n";
			}
			
			fileContent = resultText;
		}
		
		char startChar = ' ';
		char endChar = ' ';
		
		int endIndex = -1;
		
		for (int n=0;n<fileContent.length();n++) {
			
			if ('(' == fileContent.charAt(n)) {
				
				startChar = '(';
				endChar = ')';
			} else if ('{' == fileContent.charAt(n)) {
				
				startChar = '{';
				endChar = '}';
			} else if ('[' == fileContent.charAt(n)) {
				
				startChar = '[';
				endChar = ']';
			}
			
			if (' ' != startChar) {
				
				tempText = fileContent.substring(n, fileContent.length());
				
				endIndex = getTagEndIndex(tempText, startChar, endChar) + 1;
				
				tempText = tempText.substring(0, endIndex);
				
				if (tempText.indexOf('\n') > -1 && tempText.lastIndexOf('\n') > tempText.indexOf('\n')) {
					
					n += tempText.indexOf('\n');
					
					endIndex = n + tempText.lastIndexOf('\n') - tempText.indexOf('\n');
					
					tempText = tempText.substring(tempText.indexOf('\n') + 1, tempText.lastIndexOf('\n'));
					
					tempText = processFileContentFormat(tempText, indentCount + 2);
					
					if ('\n' != tempText.charAt(tempText.length() - 1)) tempText += "\n";
					
					fileContent = fileContent.substring(0, n + 1) + tempText + fileContent.substring(endIndex + 1, fileContent.length());
					
					n += tempText.length();
				} else {
					
					n += endIndex - 1;
				}
			}
			
			startChar = ' ';
			endChar = ' ';
		}
		
		
		return fileContent;
	}
	
	/**
	 * 处理JS内容格式
	 * 
	 * @param sourceText 要处理js的内容
	 * @param indentCount 添加的空格数
	 * @return String
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
		
		// 1. 处理[ 和 { 成对包含的情况
		char startChar = ' ';
		char endChar = ' ';
		
		if (lineContent.indexOf('{') > -1 && lineContent.indexOf('[') > -1) {
			
			if (lineContent.indexOf('{') > lineContent.indexOf('[')) {
				
				startChar = '{';
				endChar = '}';
			} else {
				
				startChar = '[';
				endChar = ']';
			}
		} else if (lineContent.indexOf('{') > -1) {
			
			startChar = '{';
			endChar = '}';
			
		} else if (lineContent.indexOf('[') > -1) {
			
			startChar = '[';
			endChar = ']';
		}
		
		if (' ' != startChar) {
			
			tempText = sourceText.substring(lineContent.indexOf(startChar), sourceText.length());
			
			endIndex = getTagEndIndex(tempText, startChar, endChar) + lineContent.indexOf(startChar);
			
			if (lineContent.indexOf(startChar) + 1 < endIndex) {
				
				tempText = sourceText.substring(lineContent.indexOf(startChar) + 1, endIndex);
				
				if (tempText.indexOf('\n') > -1) {
					
					return getIndentCountResutl(indentCount) + lineContent.substring(0, lineContent.indexOf(startChar) + 1) + processJSContentFormat(tempText, indentCount + 2) + processJSContentFormat(sourceText.substring(endIndex, sourceText.length()), indentCount);
				}
			}
			
		}
		
		sourceText = getIndentCountResutl(indentCount) + lineContent + processJSContentFormat(sourceText.substring(sourceText.indexOf('\n') + 1, sourceText.length()), indentCount);// 添加空格
		
		return sourceText;
	}
	
	/**
	 * 添加空格
	 * 
	 * @param indentCount
	 * @return String
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
	 * @return String
	 */
	public static String reNameVariable(String variableName) {
		
		for (int i = 0;i < variableName.length();i++) {
			
			if (!String.valueOf(variableName.charAt(i)).matches(ConvertParam.JS_VARIABLE_REG)) {
				variableName = variableName.replaceAll(String.valueOf(variableName.charAt(i)), "");
				i--;
			}
		}
		
		if ("".equals(variableName)) variableName = "iLoveYouForever";
		
		return variableName;
	}
	
	/**
	 * 获取字符串信息开始的索引
	 * 
	 * @param sourceText
	 * @param tagName
	 * @return int
	 */
	public static int getStringStartIndex(String sourceText, String tagName) {
		
		int startIndex = 0;
		
		if (sourceText.indexOf(tagName) > -1 && "".equals(sourceText.substring(0, sourceText.indexOf(tagName)))) {
			
			startIndex = sourceText.indexOf(tagName);
		}
		
		return startIndex;
	}
	
	/**
	 * 获取当前年月日
	 * 
	 * @return String
	 */
	public static String getCurrentYYYYMMDD() {
		
		// 获取当前日期
	    LocalDate currentDate = LocalDate.now();

	    // 定义日期格式
	    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

	    // 格式化日期为指定格式
	    return currentDate.format(dateFormatter);
	}
	
	/**
	 * 判断是否有某个字符标志并返回对应索引值
	 * 
	 * @param sourceText
	 * @param keyWord
	 * @return int
	 */
	public static int getKeyWordIndex(String sourceText,String keyWord) {
		
		keyWordIndex = -1;
		
		isKeyWordExist(sourceText, keyWord, 0);
		
		return keyWordIndex;
	}
	
	/**
	 * 判断是否有某个字符标志并返回对应索引值
	 * 
	 * @param processContentTxt
	 * @param keyWord
	 * @param startIndex
	 */
	public static void isKeyWordExist(String processContentTxt,String keyWord, int startIndex) {
		
		String tempTxt = "";
		String dataDescription = "";
		String sourceText = processContentTxt.substring(startIndex, processContentTxt.length());
		
		// 先获取注释信息
		dataDescription = getCommentInformation(sourceText);
		
		sourceText = sourceText.substring(sourceText.indexOf(dataDescription) + dataDescription.length(), sourceText.length());
		
		tempTxt = sourceText.trim();
		
		startIndex += dataDescription.length();
		
		// 还有注释信息，继续清除
		if (tempTxt.indexOf("<--") == 0 || tempTxt.indexOf("/**") == 0 || tempTxt.indexOf("//") == 0) {
			
			startIndex += sourceText.indexOf(tempTxt.substring(0, 2));
			
			isKeyWordExist(processContentTxt, keyWord, startIndex);
			return;
		}
		
		// 无对应关键信息
		if (sourceText.indexOf(keyWord) < 0) return;
			
		// 首尾都带非变量标志
		if (!String.valueOf(keyWord.charAt(0)).matches(ConvertParam.JS_VARIABLE_REG) && !String.valueOf(keyWord.charAt(keyWord.length() - 1)).matches(ConvertParam.JS_VARIABLE_REG)) {
			
			keyWordIndex = startIndex + sourceText.indexOf(keyWord);
		}
		// 末位带非变量标志
		else if (!String.valueOf(keyWord.charAt(keyWord.length() - 1)).matches(ConvertParam.JS_VARIABLE_REG)) {
			
			if (sourceText.indexOf(keyWord) == 0) {
				
				keyWordIndex = startIndex;
			} else if (!String.valueOf(sourceText.charAt(sourceText.indexOf(keyWord) - 1)).matches(ConvertParam.JS_VARIABLE_REG)) {
				
				keyWordIndex = startIndex + sourceText.indexOf(keyWord);
			}
			
		} 
		// 全是变量
		else {
			
			if (sourceText.indexOf(keyWord) == 0) {
				
				if (sourceText.length() == keyWord.length() || (sourceText.length() > keyWord.length() && !String.valueOf(sourceText.charAt(sourceText.indexOf(keyWord) + keyWord.length())).matches(ConvertParam.JS_VARIABLE_REG))) {
					
					keyWordIndex = startIndex + sourceText.indexOf(keyWord);
				}
			} else if (sourceText.indexOf(keyWord) == sourceText.length() - keyWord.length()) {
				
				if (!String.valueOf(sourceText.charAt(sourceText.indexOf(keyWord) - 1)).matches(ConvertParam.JS_VARIABLE_REG)) {
					
					keyWordIndex = startIndex + sourceText.indexOf(keyWord);
				}
				
			} else if (!String.valueOf(sourceText.charAt(sourceText.indexOf(keyWord) - 1)).matches(ConvertParam.JS_VARIABLE_REG) && !String.valueOf(sourceText.charAt(sourceText.indexOf(keyWord) + keyWord.length())).matches(ConvertParam.JS_VARIABLE_REG)) {
				
				keyWordIndex = startIndex + sourceText.indexOf(keyWord);
			}
		}
		
		if (keyWordIndex == -1) {
			
			startIndex += sourceText.indexOf(keyWord) + keyWord.length();
			
			dataDescription = processContentTxt.substring(startIndex, processContentTxt.length());
					
			if (dataDescription.indexOf(keyWord) > -1) isKeyWordExist(processContentTxt, keyWord, startIndex);
		}
		
	}
	
	/**
	 * 替换内容中的this 信息
	 * 
	 * @param jsxContent
	 * @param KeyWord
	 * @param replaceKeyWord
	 * @return String
	 */
	public static String replaceJsxContentStateVariable(String jsxContent, String KeyWord, String replaceKeyWord) {
		
		String tempText = "";
		String processContent = "";
		
		int endIndex = -1;
		
		// 找到{} 中的变量再判断
		if (jsxContent.indexOf('{') > -1) {
			
			tempText = jsxContent.substring(jsxContent.indexOf('{'), jsxContent.length());
			
			endIndex = getTagEndIndex(tempText, '{', '}') + 1;
			
			tempText = tempText.substring(0, endIndex);
			
			processContent = tempText;
			
			tempText = replaceAll(tempText, KeyWord, replaceKeyWord);
			
			return jsxContent.substring(0, jsxContent.indexOf(processContent)) + tempText + replaceJsxContentStateVariable(jsxContent.substring(jsxContent.indexOf(processContent) + processContent.length(), jsxContent.length()), KeyWord, replaceKeyWord);
		}
		
		return jsxContent;
	}
	
	/**
	 * 替换内容中的所有关键字
	 * 
	 * @param sourceText
	 * @param KeyWord
	 * @param replaceKeyWord
	 * @return String
	 */
	public static String replaceAll(String sourceText, String KeyWord, String replaceKeyWord) {
		
		int endIndex = -1;
		
		endIndex = getKeyWordIndex(sourceText, KeyWord);
		
		if (endIndex != -1) {
			
			return sourceText.substring(0, endIndex) + replaceKeyWord + replaceAll(sourceText.substring(endIndex + KeyWord.length(), sourceText.length()), KeyWord, replaceKeyWord);
		}
		
		return sourceText;
	}
	
	/**
	 * 替换内容中的this 信息
	 * 
	 * @param sourceText
	 * @param thisKeyWord
	 * @param KeyWord
	 * @param replaceKeyWord
	 * @param wordType
	 * @return String
	 */
	public static String replaceThisOfFrameWorkContent(String sourceText, String thisKeyWord, String KeyWord, String replaceKeyWord, String wordType) {
		
		String tempTxt = "";
		String currentMethodTxt = sourceText;
		
		int startIndex = -1;
		
		if (currentMethodTxt.indexOf(thisKeyWord + KeyWord) > -1) {
			
			tempTxt = currentMethodTxt.substring(0, currentMethodTxt.indexOf(thisKeyWord + KeyWord));
			
			// 如果前一个字符也是符合变量定义，则说明不是
			if (!"".equals(tempTxt) && String.valueOf(tempTxt.charAt(tempTxt.length() - 1)).matches(ConvertParam.JS_VARIABLE_REG)) {
				
				startIndex = currentMethodTxt.indexOf(thisKeyWord + KeyWord) + (thisKeyWord + KeyWord).length();
				
			} else {
				
				tempTxt = currentMethodTxt.substring(currentMethodTxt.indexOf(thisKeyWord + KeyWord) + (thisKeyWord + KeyWord).length(), currentMethodTxt.length());
				
				if ("".equals(KeyWord) || '.' == KeyWord.charAt(KeyWord.length() - 1)) {
					
					currentMethodTxt = currentMethodTxt.replace(thisKeyWord + KeyWord, wordType + replaceKeyWord);
					
					startIndex = currentMethodTxt.indexOf(wordType + KeyWord) + (wordType + KeyWord).length();
				}
				// 说明不可以替换
				else if ('(' != KeyWord.charAt(KeyWord.length() - 1) && String.valueOf(tempTxt.charAt(0)).matches(ConvertParam.JS_VARIABLE_REG)) {
					
					startIndex = currentMethodTxt.indexOf(thisKeyWord + KeyWord) + (thisKeyWord + KeyWord).length();
					
				} else {
					
					currentMethodTxt = currentMethodTxt.replace(thisKeyWord + KeyWord, wordType + replaceKeyWord);
					
					startIndex = currentMethodTxt.indexOf(wordType + KeyWord) + (wordType + KeyWord).length();
					
				}
			}
			
			return currentMethodTxt.substring(0, startIndex) + replaceThisOfFrameWorkContent(currentMethodTxt.substring(startIndex, currentMethodTxt.length()), thisKeyWord, KeyWord, replaceKeyWord, wordType);
		}
		
		return sourceText;
	}
	
	public static String findImportContentByKey(String fileContent, String fromKey, int startIndex) {
		
		String tempText = "";
		
		if (fileContent.indexOf(fromKey) > -1) {
			
			int endIndex = fileContent.indexOf(fromKey);
			
			tempText = fileContent.substring(0, endIndex);
			
			// find it
			if (tempText.trim().lastIndexOf("from ") == tempText.trim().length() - 6) {
				
				String replaceContent = "";
				
				startIndex += tempText.lastIndexOf("import ");
				
				replaceContent = tempText.substring(tempText.lastIndexOf("import "), tempText.length());
				
				tempText = fileContent.substring(endIndex, fileContent.length());
				
				tempText = tempText.substring(0, TxtContentUtil.getStatementEndIndex(tempText, 0));
				
				replaceContent += tempText;
				
				return startIndex + "#" + replaceContent;
				
			} else {
				
				tempText = fileContent.substring(endIndex + fromKey.length(), fileContent.length());
				
				startIndex += endIndex + fromKey.length();
				
				return findImportContentByKey(tempText, fromKey, startIndex);
			}
		}
		
		return "";
	}
	
	/**
	 * 获取所有定义的变量信息
	 * 
	 * @param sourceText
	 * @param variableNameList
	 */
	public static void getDefineVariable(String sourceText, ArrayList<String> variableNameList) {
		
		String dataDescription = "";
		
		// 先获取注释信息
		dataDescription = getCommentInformation(sourceText);
		
		sourceText = sourceText.substring(sourceText.indexOf(dataDescription) + dataDescription.length(), sourceText.length()).trim();
		
		// 还有注释信息，继续清除
		if (sourceText.indexOf("<--") == 0 || sourceText.indexOf("/**") == 0 || sourceText.indexOf("//") == 0) {
			getDefineVariable(sourceText, variableNameList);
			return;
		}
		
		sourceText = getDefineVariableAndClearDefine(sourceText, "const", variableNameList);
		sourceText = getDefineVariableAndClearDefine(sourceText, "var", variableNameList);
		sourceText = getDefineVariableAndClearDefine(sourceText, "let", variableNameList);
	}
	
	public static String getDefineVariableAndClearDefine(String sourceText, String variableType, ArrayList<String> variableNameList) {
		
		String tempTxt = "";
		String defineContent = "";

		int startIndex = -1;
		
		startIndex = getKeyWordIndex(sourceText, variableType);
		
		if (startIndex != -1) {
			
			tempTxt = sourceText.substring(startIndex, sourceText.length());
			
			defineContent = tempTxt.substring(0, getStatementEndIndex(tempTxt, 0) + 1);
			
			startIndex = getNotVariableIndex(tempTxt, variableType.length() + 1);
			
			tempTxt = tempTxt.substring(variableType.length(), startIndex);
			
			variableNameList.add(tempTxt.trim());
			
			sourceText = sourceText.replace(defineContent, "");
		}
		
		startIndex = getKeyWordIndex(sourceText, variableType);
		
		if (startIndex != -1) {
			
			getDefineVariableAndClearDefine(sourceText.trim(), variableType, variableNameList);
		}
		
		return sourceText;
	}
	
}
