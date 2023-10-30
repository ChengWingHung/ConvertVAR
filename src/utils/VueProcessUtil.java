package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.ConvertParam;

public class VueProcessUtil {

	public VueProcessUtil() {
		
	}
	
	private static String vueTempText = "";
	
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
				
				endIndex = TxtContentUtil.getNotVariableIndex(sourceText, 0);
				dataName = sourceText.substring(0, endIndex == -1?sourceText.length():endIndex);
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
				sourceText = sourceText.substring(0, startIndex) + "v-model:" + sourceText.substring(startIndex + 1, sourceText.length());
				
				startIndex = sourceText.indexOf(".sync=");
				
				// 清空.sync= 并继续后续判断处理
				
				return sourceText.substring(0, startIndex) + replaceSyncPropsToVmodel(sourceText.substring(startIndex + ".sync=".length() - 1, sourceText.length()));
			}
		}
		
		return sourceText;
	}
	
	/**
	 * vue2的key 值转换处理，<template v-for> 子节点的key 需要绑定到template 上
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String changeTemplateKeyBindObject(String sourceText) {
		
		// 无template 内容直接返回，<template>这种说明无v-for 属性
		if (sourceText.indexOf("<template ") < 0) return sourceText;
		
		String tempTxt = "";
		
		tempTxt = sourceText.substring(sourceText.indexOf("<template "), sourceText.length());
		
		// template无v-for 属性直接返回
		if (sourceText.indexOf(" v-for=") < 0 || sourceText.indexOf(" v-for=") > sourceText.indexOf(">")) return sourceText;
		
		String vTemplateResultContent = ""; // 用于替换的template 的子内容
		String vTemplateContent = ""; // 
		String vForContent = ""; // v-for 的内容
		String vForItemTxt = ""; // v-for 内容中的item字段
		
		int endIndex = -1;
		
		tempTxt = tempTxt.substring(tempTxt.indexOf(" v-for=") + " v-for=".length(), tempTxt.indexOf(">"));
		
		endIndex = TxtContentUtil.getTagEndIndex(tempTxt, tempTxt.charAt(0), tempTxt.charAt(0));
		
		vForContent = tempTxt.substring(1, endIndex + 1);
		
		vForItemTxt = vForContent.substring(0, vForContent.indexOf(' ')).trim();
		
		vTemplateContent = sourceText.substring(sourceText.indexOf("<template "), sourceText.indexOf("</template"));
		
		// 得到template 的子内容
		vTemplateContent = vTemplateContent.substring(vTemplateContent.indexOf('>') + 1, vTemplateContent.length());
		
		vTemplateResultContent = vTemplateContent;
		
		vueTempText = ""; // 获取绑定的item.字段 信息
		// 处理掉 vTemplateContent 中含有 vForItemTxt 的key信息
		vTemplateContent = processTemplateContentKey(vTemplateContent, vForItemTxt);
		
		sourceText = sourceText.replace(vTemplateResultContent, vTemplateContent);
		
		if (!"".equals(vueTempText)) {
			
			endIndex = sourceText.indexOf(vForContent);
			
			sourceText = sourceText.substring(0, endIndex + vForContent.length()) + " :key=\"" + vueTempText + "\" " + sourceText.substring(endIndex + vForContent.length(), sourceText.length());
		}
		
		return sourceText;
	}
	
	/**
	 * 处理vTemplateContent 中含有 vForItemTxt 的key信息
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String processTemplateContentKey(String sourceText, String itemText) {
		
		// 未设置key 则不处理
		if (sourceText.indexOf(" :key=") < 0) return sourceText;
		
		String tempTxt = "";
		String keyContent = "";
		
		keyContent = sourceText.substring(sourceText.indexOf(" :key="), sourceText.length());
		
		int endIndex = -1;
		
		tempTxt = keyContent.substring(" :key=".length(), keyContent.length());
		
		endIndex = TxtContentUtil.getTagEndIndex(tempTxt, tempTxt.charAt(0), tempTxt.charAt(0));
		
		tempTxt = tempTxt.substring(0, endIndex);
		
		keyContent = keyContent.substring(0, endIndex + 1 + " :key=".length());
		
		// 判断是否有v-for 中的 item 信息
		if (tempTxt.indexOf(itemText + ".") < 0) return sourceText;
		
		tempTxt = tempTxt.substring(tempTxt.indexOf(itemText + ".") + (itemText + ".").length(), tempTxt.length());
		
		endIndex = -1;
		
		for (int i = 0;i<tempTxt.length();i++) {
			
			if (!String.valueOf(tempTxt.charAt(i)).matches(ConvertParam.JS_VARIABLE_REG)) {
				endIndex = i;
				break;
			}
			
			if (i == tempTxt.length() - 1 && endIndex == -1) {
				endIndex = tempTxt.length();
			}
		}
		
		vueTempText = itemText + "." + tempTxt.substring(0, endIndex);
		
		return sourceText.substring(0, sourceText.indexOf(keyContent) + keyContent.length()).replace(keyContent, "") + processTemplateContentKey(sourceText.substring(sourceText.indexOf(keyContent) + keyContent.length(), sourceText.length()), itemText);
	}
	
	/**
	 * 获取render 整个方法内容
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String getRenderAllContentFunction(String sourceText) {
		
		String tempText = sourceText;// 临时处理字段
		String vueRenderContent = "";// Vue.component render 部分内容
		
		int endIndex = -1;// 获取截取结束位置
		
		// 判断是否有render 函数
		if (tempText.indexOf("render:") > -1) {
			
			vueRenderContent = tempText.substring(tempText.indexOf("render:"), tempText.length());
			
		} else if (tempText.indexOf("render(") > -1) {
			
			vueRenderContent = tempText.substring(tempText.indexOf("render("), tempText.length());
		}
		
		if (!"".equals(vueRenderContent)) {
			
			// 非箭头函数
			if (vueRenderContent.indexOf('{') > -1) {
				
				endIndex = TxtContentUtil.getTagEndIndex(vueRenderContent, '{', '}');
				
				vueRenderContent = vueRenderContent.substring(0, endIndex + 1);
				
			} else {
				
				// 箭头函数
				vueRenderContent = vueRenderContent.substring(0, vueRenderContent.length());
				
				endIndex = TxtContentUtil.getStatementEndIndex(vueRenderContent, 0);
				
				vueRenderContent = vueRenderContent.substring(0, endIndex + 1);
				
			}
			
		}
		
		return vueRenderContent;
	}
	
	/**
	 * 获取render 方法体内容
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String getRenderContentFunction(String sourceText) {
		
		String tempText = sourceText;// 临时处理字段
		String vueRenderContent = "";// Vue.component render 部分内容
		
		int endIndex = -1;// 获取截取结束位置
		
		// 判断是否有render 函数
		if (tempText.indexOf("render:") > -1) {
			
			vueRenderContent = tempText.substring(tempText.indexOf("render:") + "render:".length(), tempText.length());
			
		} else if (tempText.indexOf("render(") > -1) {
			
			vueRenderContent = tempText.substring(tempText.indexOf("render(") + "render(".length(), tempText.length());
		}
		
		if (!"".equals(vueRenderContent)) {
			
			// 非箭头函数
			if (vueRenderContent.indexOf('{') > -1) {
				
				endIndex = TxtContentUtil.getTagEndIndex(vueRenderContent, '{', '}');
				
				vueRenderContent = vueRenderContent.substring(vueRenderContent.indexOf('{'), endIndex + 1);
				
			} else {
				
				// 箭头函数
				vueRenderContent = vueRenderContent.substring(vueRenderContent.indexOf("=>") + "=>".length(), vueRenderContent.length());
				
				endIndex = TxtContentUtil.getStatementEndIndex(vueRenderContent, 0);
				
				vueRenderContent = vueRenderContent.substring(0, endIndex + 1);
				
			}
			
		}
		
		return vueRenderContent;
	}
	
	/**
	 * 获取option api 中的属性及其索引位置
	 * 
	 * @param sourceText
	 * @return
	 */
	public static void getPropertyOfOptionsApi(String sourceText, Map<String, Map<String, String>> optionApiPropMap, int findIndex) {
		
		String tempText = "";
		String apiName = "";
		String apiNameEndChar = "";
		String apiNextContent = "";
		String apiDescription = "";
		
		int startIndex = 0;
		int endIndex = -1;
		
		startIndex = TxtContentUtil.getStringStartIndex(sourceText, "/**");
		
		findIndex += startIndex;
		
		sourceText = sourceText.substring(startIndex, sourceText.length());
		
		startIndex = TxtContentUtil.getStringStartIndex(sourceText, "//");
		
		findIndex += startIndex;
		
		sourceText = sourceText.substring(startIndex, sourceText.length());
		
		// 先获取注释信息
		apiDescription = TxtContentUtil.getCommentInformation(sourceText);
		
		findIndex += apiDescription.length();
		
		// 还有注释信息，继续清除
		if (sourceText.indexOf("/**") == 0 || sourceText.indexOf("//") == 0) {
			getPropertyOfOptionsApi(sourceText, optionApiPropMap, findIndex);
			return;
		}
		
		// 找到第一个符合变量定义规则的索引
		startIndex = TxtContentUtil.getVariableStartIndex(sourceText, 0);
		
		startIndex = startIndex == -1?0:startIndex;
		
		// 无逗号，只有一个字段
		if (sourceText.indexOf(',') < 0) {
			
			if (sourceText.indexOf(':') > -1) {
				
				apiNameEndChar = ":";// 冒号
				
				apiName = sourceText.substring(startIndex, sourceText.indexOf(':'));
			} else if (sourceText.indexOf('(') > -1) {
				
				apiNameEndChar = sourceText.substring(sourceText.indexOf('('), sourceText.indexOf(')'));// 括号
				
				apiName = sourceText.substring(startIndex, sourceText.indexOf('('));
			}
			
			if (!"".equals(apiName.trim())) {
				
				findIndex += sourceText.indexOf(apiName);
				
				Map<String, String> apiDataMap = new HashMap<>();
				
				apiDataMap.put("apiName", apiName);
				apiDataMap.put("apiNameIndex", String.valueOf(findIndex));
				apiDataMap.put("apiNameEndChar", apiNameEndChar);
				
				optionApiPropMap.put(apiName.trim(), apiDataMap);
			}
			
		} else {
			
			endIndex = TxtContentUtil.getNotVariableIndex(sourceText, startIndex);
			
			tempText = sourceText.substring(endIndex == -1?0:endIndex, sourceText.length()).trim();
			
			char charTypeValue = tempText.charAt(0);
			
			if (' ' != charTypeValue) {
				
				apiName = sourceText.substring(startIndex, sourceText.indexOf(charTypeValue));
				
				if (':' == charTypeValue) {
					
					apiNameEndChar = ":";
					
					//值部分判断 ' " [ ` { 字符包裹的情况
					tempText = sourceText.substring(sourceText.indexOf(charTypeValue) + 1, sourceText.length());
					
					char startChar = tempText.trim().charAt(0);
					
					if ("'\"[`{".indexOf(startChar) > -1) {
						
						char endChar = startChar;
						
						if ('[' == startChar) {
							endChar = ']';
						} else if ('{' == startChar) {
							endChar = '}';
						}
						
						endIndex = sourceText.indexOf(charTypeValue) + 1 + TxtContentUtil.getTagEndIndex(tempText, startChar, endChar) + 1;
						
					} else {
						
						endIndex = sourceText.indexOf(',');
					}
					
				} else {
					
					apiNameEndChar = sourceText.substring(sourceText.indexOf('('), sourceText.indexOf(')'));
					
					tempText = sourceText.substring(sourceText.indexOf(")") + 1, sourceText.length());
					
					endIndex = sourceText.indexOf(")") + 1 + TxtContentUtil.getTagEndIndex(tempText, '{', '}') + 1;
				}
				
				startIndex = findIndex + sourceText.indexOf(apiName);
				
				Map<String, String> apiDataMap = new HashMap<>();
				
				apiDataMap.put("apiName", apiName);
				apiDataMap.put("apiNameIndex", String.valueOf(startIndex));
				apiDataMap.put("apiNameEndChar", apiNameEndChar);
				
				optionApiPropMap.put(apiName.trim(), apiDataMap);
				
				findIndex += endIndex;
						
				apiNextContent = sourceText.substring(endIndex, sourceText.length());
				
			} else {
				
				apiName = sourceText.substring(startIndex, sourceText.indexOf(','));
				
				findIndex += apiName.length();
				
				Map<String, String> apiDataMap = new HashMap<>();
				
				apiDataMap.put("apiName", apiName);
				apiDataMap.put("apiNameIndex", String.valueOf(startIndex));
				apiDataMap.put("apiNameEndChar", "");
				
				optionApiPropMap.put(apiName.trim(), apiDataMap);
				
				apiNextContent = sourceText.substring(sourceText.indexOf(',') + 1, sourceText.length());
			}
			
			tempText = apiNextContent.trim();
			
			if (tempText.length() > 0) {
				
				startIndex = -1;
				
				// 去除首个字符为逗号
				if (',' == tempText.charAt(0)) {
					
					for (int i=0;i<apiNextContent.length();i++) {
						
						if (',' == apiNextContent.charAt(i)) {
							startIndex = i;
							break;
						}
					}
					
					findIndex += startIndex + 1;
				}
				
				tempText = apiNextContent.substring(startIndex + 1, apiNextContent.length());
				
				getPropertyOfOptionsApi(tempText, optionApiPropMap, findIndex);
			}
		}
		
	}
	
	/**
	 * 删除 .native 修饰符的所有实例
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String clearOnEventWithNativeKeyWord(String sourceText) {
		
		
		return sourceText;
	}
	
	
}
