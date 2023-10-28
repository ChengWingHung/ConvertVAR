package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.ConvertParam;

public class VueProcessUtil {

	public VueProcessUtil() {
		
	}
	
	/**
	 * 处理vue2 props信息
	 * 
	 * @param sourceText
	 * @return
	 */
	public static void processVuePropsInfo(String sourceText, Map<String, Map<String, String>> propsResultMap) {
		
		String tempText = "";
		String propName = "";
		String propDefineValue = "";
		String propContent = "";
		String propDescription = "";
		
		// 先获取注释信息
		propDescription = TxtContentUtil.getCommentInformation(sourceText);
		
		sourceText = sourceText.substring(sourceText.indexOf(propDescription) + propDescription.length(), sourceText.length()).trim();
		
		propName = sourceText.substring(0, sourceText.indexOf(':'));
		
		tempText = sourceText.substring(sourceText.indexOf(':') + 1, sourceText.length()).trim();
		
		propContent = TxtContentUtil.getContentByTag(sourceText, 0, tempText.charAt(0), tempText.charAt(0) == '{'?'}':tempText.charAt(0));
		
		propDefineValue = propContent.substring(propContent.indexOf(':') + 1, propContent.length());
		
		Map<String, String> propMap = new HashMap<>();
		
		propMap.put(propDefineValue, propDescription);
		
		propsResultMap.put(propName, propMap);
		
		sourceText = sourceText.substring(sourceText.indexOf(propContent) + propContent.length(), sourceText.length()).trim();
		
		if (sourceText.indexOf(':') > 0) {
			
			// 去除首个字符为逗号
			if (',' == sourceText.charAt(0)) sourceText = sourceText.substring(1, sourceText.length());
			
			processVuePropsInfo(sourceText.trim(), propsResultMap);
		}
	}
	
	/**
	 * 处理vue2 data信息
	 * 
	 * @param sourceText
	 * @return
	 */
	public static void processVueDataInfo(String sourceText, ArrayList<String> dataResultList) {
		
		String tempText = "";
		String dataName = "";
		String dataNextContent = "";
		String dataDescription = "";
		
		int endIndex = -1;
		
		// 先获取注释信息
		dataDescription = TxtContentUtil.getCommentInformation(sourceText);
		
		sourceText = sourceText.substring(sourceText.indexOf(dataDescription) + dataDescription.length(), sourceText.length()).trim();
		
		// 还有注释信息，继续清除
		if (sourceText.indexOf("/**") == 0 || sourceText.indexOf("//") == 0) {
			processVueDataInfo(sourceText, dataResultList);
			return;
		}
		
		// 无逗号，只有一个字段
		if (sourceText.indexOf(',') < 0) {
			
			// 有冒号
			if (sourceText.indexOf(':') > -1) {
				
				dataName = sourceText.substring(0, sourceText.indexOf(':'));
			} else if (!"".equals(sourceText)){
				
				endIndex = TxtContentUtil.getNotVariableIndex(sourceText);
				dataName = sourceText.substring(0, endIndex);
			}
			
			if (!"".equals(dataName.trim()))
				dataResultList.add(dataName.trim());
			
		} else {
			
			if (sourceText.indexOf(',') > sourceText.indexOf(':')) {
				
				dataName = sourceText.substring(0, sourceText.indexOf(':'));
				//值部分判断 ' " [ ` { 字符包裹的情况
				tempText = sourceText.substring(sourceText.indexOf(':') + 1, sourceText.length()).trim();
				
				char startChar = tempText.charAt(0);
				
				if ("'\"[`{".indexOf(startChar) > -1) {
					
					char endChar = tempText.charAt(0);
					
					if ('[' == startChar) {
						endChar = ']';
					} else if ('{' == startChar) {
						endChar = '}';
					}
					
					endIndex = sourceText.indexOf(':') + 1 + TxtContentUtil.getTagEndIndex(tempText, startChar, endChar) + 2;
					
				} else {
					
					endIndex = sourceText.indexOf(',');
				}
				
				dataNextContent = sourceText.substring(endIndex, sourceText.length());
			} else {
				
				dataName = sourceText.substring(0, sourceText.indexOf(','));
				
				dataNextContent = sourceText.substring(sourceText.indexOf(',') + 1, sourceText.length());
			}
			
			if (!"".equals(dataName.trim()))
				dataResultList.add(dataName.trim());
			
			sourceText = dataNextContent.trim();
			
			if (sourceText.length() > 0) {
				
				// 去除首个字符为逗号
				if (',' == sourceText.charAt(0)) sourceText = sourceText.substring(1, sourceText.length());
				
				processVueDataInfo(sourceText.trim(), dataResultList);
			}
		}
		
	}
	
	/**
	 * vue2 替换setup 中的 this.
	 * 
	 * @param sourceText
	 * @param thisKeyWord this标志
	 * @param KeyWord 属性值
	 * @param wordType 是state还是props
	 * @return 替换后信息
	 */
	public static String replaceThisOfVue2Method(String sourceText, String thisKeyWord, String KeyWord, String wordType) {
		
		String tempTxt = "";
		String currentMethodTxt = sourceText;
		
		int startIndex = -1;
		
		if (currentMethodTxt.indexOf(thisKeyWord + KeyWord) > -1) {
			
			tempTxt = currentMethodTxt.substring(currentMethodTxt.indexOf(thisKeyWord + KeyWord) + (thisKeyWord + KeyWord).length(), currentMethodTxt.length()).trim();
			
			// 说明不可以替换
			if (String.valueOf(tempTxt.charAt(0)).matches(ConvertParam.JS_VARIABLE_REG)) {
				
				startIndex = currentMethodTxt.indexOf(thisKeyWord + KeyWord) + (thisKeyWord + KeyWord).length();
				
			} else {
				
				currentMethodTxt = currentMethodTxt.replace(thisKeyWord + KeyWord, wordType + KeyWord);
				
				startIndex = currentMethodTxt.indexOf(wordType + KeyWord) + (wordType + KeyWord).length();
				
			}
			
			return currentMethodTxt.substring(0, startIndex) + replaceThisOfVue2Method(currentMethodTxt.substring(startIndex, currentMethodTxt.length()), thisKeyWord, KeyWord, wordType);
		}
		
		return sourceText;
	}
	
	/**
	 * .sync 的部分并将其替换为 v-model
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String replaceSyncPropsToVmodel(String sourceText) {
		
		if (sourceText.indexOf(".sync=") > -1) {
			
			String tempTxt = "";
			String nextProcessText = "";
			
			int startIndex = -1;
			
			tempTxt = sourceText.substring(0, sourceText.indexOf(".sync="));
			
			for (int i=tempTxt.length()-1;i>-1;i--) {
				
				if (':' == tempTxt.charAt(i)) {
					startIndex = i;
					break;
				}
			}
			
			// 处理替换操作
			if (startIndex != -1) {
				
				// 添加v-model
				sourceText = sourceText.substring(0, startIndex) + "v-model" + sourceText.substring(startIndex + 1, sourceText.length());
				
				startIndex = sourceText.indexOf(".sync=");
				
				// 清空.sync= 并继续后续判断处理
				
				return sourceText.substring(0, startIndex) + replaceSyncPropsToVmodel(sourceText.substring(startIndex + ".sync=".length(), sourceText.length()));
			}
		}
		
		return sourceText;
	}
}
