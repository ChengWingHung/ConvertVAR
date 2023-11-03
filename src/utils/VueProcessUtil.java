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
	 * vue2 替换setup 中的 this.  setup 中无this
	 * 
	 * 1 props 和 state 中的把 this. 替换为 props. 或 state.
	 * 2 method 中的 this. 直接去除
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
	 * 获取对象属性及其所有信息
	 * 
	 * @param sourceText
	 * @return
	 */
	public static void getPropertyDetailOfObject(String sourceText, Map<String, Map<String, String>> recordPropertyMap, int findIndex) {
		
		String tempText = "";
		String apiName = "";
		String apiNameValue = "";
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
			getPropertyDetailOfObject(sourceText, recordPropertyMap, findIndex);
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
				
				if ("el".equals(apiName)) {
					
					apiNameValue = sourceText.substring(sourceText.indexOf(':') + 1, sourceText.length()).trim();
					
					if ("'\"".indexOf(apiNameValue.charAt(0)) > -1) apiNameValue = apiNameValue.substring(1, apiNameValue.length() - 1);
				}
				
			} else if (sourceText.indexOf('(') > -1) {
				
				apiNameEndChar = sourceText.substring(sourceText.indexOf('('), sourceText.indexOf(')'));// 括号
				
				apiName = sourceText.substring(startIndex, sourceText.indexOf('('));
			}
			
			if (!"".equals(apiName.trim())) {
				
				findIndex += sourceText.indexOf(apiName);
				
				Map<String, String> apiDataMap = new HashMap<>();
				
				apiDataMap.put("apiName", apiName);
				apiDataMap.put("apiNameValue", apiNameValue);
				apiDataMap.put("apiNameIndex", String.valueOf(findIndex));
				apiDataMap.put("apiNameEndChar", apiNameEndChar);
				
				recordPropertyMap.put(apiName.trim(), apiDataMap);
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
						
						if ("el".equals(apiName)) {
							
							apiNameValue = tempText.trim();
							
							apiNameValue = apiNameValue.substring(1, apiNameValue.length());
							
							apiNameValue = apiNameValue.substring(0, apiNameValue.indexOf(endChar));
							
						}
						
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
				apiDataMap.put("apiNameValue", apiNameValue);
				apiDataMap.put("apiNameIndex", String.valueOf(startIndex));
				apiDataMap.put("apiNameEndChar", apiNameEndChar);
				
				recordPropertyMap.put(apiName.trim(), apiDataMap);
				
				findIndex += endIndex;
						
				apiNextContent = sourceText.substring(endIndex, sourceText.length());
				
			} else {
				
				apiName = sourceText.substring(startIndex, sourceText.indexOf(','));
				
				findIndex += apiName.length();
				
				Map<String, String> apiDataMap = new HashMap<>();
				
				apiDataMap.put("apiName", apiName);
				apiDataMap.put("apiNameValue", apiNameValue);
				apiDataMap.put("apiNameEndChar", "");
				apiDataMap.put("apiNameIndex", String.valueOf(startIndex));
				
				recordPropertyMap.put(apiName.trim(), apiDataMap);
				
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
				
				getPropertyDetailOfObject(tempText, recordPropertyMap, findIndex);
			}
		}
		
	}
	
	/**
	 * 组件属性转换
	 * 
	 * @param fileContent
	 * @return
	 */
	public static String changeComponentPropertys(String fileContent, Map<String, Map> parseResultMap) {
		
		// 1 v-bind="@xxx" -> v-bind="xxx"
		// 2 v-on: -> @
		
		String temp = "";
		String vue2property = "";// vue2对应的属性
		String vue3property = "";// vue3对应的属性
		
		for (int i=0;i<ConvertParam.Vue2ToVue3PropertyList.length;i++) {
			
			temp = ConvertParam.Vue2ToVue3PropertyList[i];
			
			if (temp.indexOf(ConvertParam.CONVERT_STRING) > -1) {
				
				vue2property = temp.substring(0, temp.indexOf(ConvertParam.CONVERT_STRING));
				
				vue3property = temp.substring(temp.indexOf(ConvertParam.CONVERT_STRING) + 2, temp.length());
				
				if (fileContent.indexOf(vue2property) > -1) {
					
					fileContent = fileContent.replaceAll(vue2property, vue3property);
				}
			}
			
		}
		
		// 3. .sync 的部分并将其替换为 v-model
		fileContent = replaceSyncPropsToVmodel(fileContent);
		
		// 4. template v-for 的子内容的key 绑定到template 上
		fileContent = changeTemplateKeyBindObject(fileContent);
		
		// 5. 删除 .native 修饰符的所有实例，子组件的选项中需设置 inheritAttrs: false
		fileContent = clearOnEventWithNativeKeyWord(fileContent);
		
		// 6. 键盘码值转换为修饰符
		fileContent = replaceKeyCodeWithKeyString(fileContent);
		
		// 7. 需要移除和替换的实例
		fileContent = removeUnUseInstanceInVue3(fileContent, parseResultMap);
		
		
		return fileContent;
	}
	
	/**
	 * 删除 .native 修饰符的所有实例，子组件需添加emits，否则会导致触发两次，本方法只去除 .native 修饰符
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String clearOnEventWithNativeKeyWord(String sourceText) {
		
		if (sourceText.indexOf(".native ") > -1){
			
			sourceText = sourceText.replaceAll(".native ", "");
		}
		
		return sourceText;
	}
	
	/**
	 * 处理vue2 中使用键码改为 vue3 中采用修饰符
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String replaceKeyCodeWithKeyString(String sourceText) {
		
		String temp = "";
		String keyCodeValue = "";// 键码
		String keyStringValue = "";// 修饰符
		
		for(String instanceValue:ConvertParam.keyCodeToKecharList)
        {
			
			keyCodeValue = instanceValue.substring(0, instanceValue.indexOf(ConvertParam.CONVERT_STRING));
			
			keyStringValue = instanceValue.substring(instanceValue.indexOf(ConvertParam.CONVERT_STRING) + 2, instanceValue.length());
			
			temp = "." + keyCodeValue + "=";
			
			if (sourceText.indexOf(temp) > -1) {
				
				sourceText = sourceText.replaceAll(temp, "." + keyStringValue + "=");
			}
        }
		
		return sourceText;
	}
	
	/**
	 * 处理vue3 中移除的实例
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String removeUnUseInstanceInVue3(String sourceText, Map<String, Map> parseResultMap) {
		
		String temp = "";
		String vue2InstanceContent = "";// vue2对应的实例内容
		String vue3InstanceContent = "";// vue3对应的实例内容

		int endIndex = -1;// 获取截取结束位置
		
		for(String instanceContentValue:ConvertParam.clearVue2InstanceList)
        {
			// 无箭头的直接移除
			if (instanceContentValue.indexOf(ConvertParam.CONVERT_STRING) > -1) {
				
				vue2InstanceContent = instanceContentValue.substring(0, instanceContentValue.indexOf(ConvertParam.CONVERT_STRING));
				
				vue3InstanceContent = instanceContentValue.substring(instanceContentValue.indexOf(ConvertParam.CONVERT_STRING) + 2, instanceContentValue.length());
			} else {
				
				vue2InstanceContent = instanceContentValue;
				
				vue3InstanceContent = "";
			}
			
			// 判断文件内容中是否有对应实例内容
			if (sourceText.indexOf(vue2InstanceContent) > -1) {
				
				// 清除无需转换的内容
				if ("".equals(vue3InstanceContent)) {
					
					if (parseResultMap.containsKey("originDefine")) {
						
						Map<String, String> instanceMap = parseResultMap.get("originDefine");
						
						temp = instanceMap.get("newVue");
						
						temp = temp + "." + vue2InstanceContent;
						
						vue2InstanceContent = sourceText.substring(sourceText.indexOf(temp), sourceText.length());
						
						endIndex = TxtContentUtil.getStatementEndIndex(vue2InstanceContent, 0);
						
						vue2InstanceContent = vue2InstanceContent.substring(0, endIndex + 1);
						
						sourceText = sourceText.replace(vue2InstanceContent, "");// 直接置空
					}
					
				} else {
					
					sourceText = sourceText.replaceAll(vue2InstanceContent, vue3InstanceContent);// 直接替换
				}
				
			}
			
        }
		
		return sourceText;
	}
	
	/**
	 * 处理vue2 this.$set() / this.$delete
	 * 
	 * 1. this.$set() => console.log("state data changed")
	 * 2. this.$delete  => console.log("state data changed")
	 * 
	 * @param methodBodyContent
	 * @return
	 */
	public static String removeVue2BindObjectInfoChangeProcess(String methodBodyContent, String processType) {
		
		String temp = "";
		String processTemp = "";
		String replaceTxt = "console.log('" + processType + " state data changed')";
		String vue2ProcessContent = "";// vue2对应的操作信息

		int endIndex = -1;// 获取截取结束位置
		
		processTemp = "this.$" + processType + "(";
		
		if (methodBodyContent.indexOf(processTemp) > -1) {
			
			temp = methodBodyContent.substring(methodBodyContent.indexOf(processTemp), methodBodyContent.length());
			
			endIndex = TxtContentUtil.getTagEndIndex(temp, '(', ')') + 1;
			
			vue2ProcessContent = temp.substring(0, endIndex);
			
			endIndex += methodBodyContent.indexOf(processTemp);
			
			temp = methodBodyContent.substring(0, endIndex);
			
			return temp.replace(vue2ProcessContent, replaceTxt) + removeVue2BindObjectInfoChangeProcess(methodBodyContent.substring(endIndex, methodBodyContent.length()), processType);
		}
		
		return methodBodyContent;
	}
	
}
