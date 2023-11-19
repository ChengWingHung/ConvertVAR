package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.ConvertParam;

public class ReactProcessUtil {

	public ReactProcessUtil() {
		
	}
	
	public static Map<String, String> classDataMap;
	
	private static String replaceContent = "";
	
	/**
	 * 获取组件中所有方法信息
	 * 
	 * @param methodContent
	 * @return
	 */ 
	public static void getMethodResultMap(String methodContent, String methodDescription, Map<String, Map<String, String>> methodResultMap){
		
		// 为空的时候无需解析
		if ("".equals(methodContent)) return;
		
		String tempText = "";
		String methodName = "";
		String methodParams = "";
		String methodBody = "";
		
		int endIndex = -1;// 获取截取结束位置
		
		// 判断是否有注释
		tempText = TxtContentUtil.getCommentInformation(methodContent);
		
		if (!"".equals(tempText)) {
					
			methodContent = methodContent.substring(methodContent.indexOf(tempText) + tempText.length(), methodContent.length());
			
			methodDescription += tempText;
		}
		
		methodContent = methodContent.trim();
			
		// 还有注释信息，继续清除
		if (methodContent.indexOf("<--") == 0 || methodContent.indexOf("/**") == 0 || methodContent.indexOf("//") == 0) {
			getMethodResultMap(methodContent, methodDescription, methodResultMap);
			return;
		}
		
		endIndex = TxtContentUtil.getNotVariableIndex(methodContent, 0);
		
		if (endIndex != -1) {
			
			methodName = methodContent.substring(0, endIndex);
			
			methodContent = methodContent.substring(endIndex, methodContent.length()).trim();
				
			if ('=' == methodContent.charAt(0) && '>' != methodContent.charAt(1)) {
				
				methodContent = methodContent.substring(1, methodContent.length()).trim();
			}
			
			// 说明有参数
			if ('(' == methodContent.charAt(0)) {
				
				endIndex = TxtContentUtil.getTagEndIndex(methodContent, '(', ')');
				
				tempText = methodContent.substring(endIndex + 1, methodContent.length()).trim();
				
				// 说明可以认为是函数体
				if ('{' != tempText.charAt(0) && !"=>".equals(tempText.substring(0, 2))) {
					
					methodBody = methodContent.substring(0, endIndex + 1);
				} else {
					
					methodParams = methodContent.substring(1, endIndex);
				}
				
				methodContent = tempText;
				
			}
			
			if (methodContent.length() > 1 && "=>".equals(methodContent.substring(0, 2))) {
				
				methodContent = methodContent.substring(2, methodContent.length()).trim();
				
				if ('{' != methodContent.charAt(0)) {
					
					endIndex = TxtContentUtil.getStatementEndIndex(methodContent, 0) + 1;
					
					methodBody = "{\n return " + methodContent.substring(0, endIndex) + "\n}";
					
					methodContent = methodContent.substring(endIndex, methodContent.length());
				}
				
			}
			
			if ('{' == methodContent.charAt(0)) {
				
				endIndex = TxtContentUtil.getTagEndIndex(methodContent, '{', '}') + 1;
				
				methodBody = methodContent.substring(0, endIndex);
				
				methodContent = methodContent.substring(endIndex, methodContent.length());
			}
			
			// 组装方法信息到map对象
			Map<String, String> methodMap = new HashMap<>();
			
			methodMap.put("methodDescription", methodDescription);
			methodMap.put("methodName", methodName);
			methodMap.put("methodParams", methodParams);
			methodMap.put("methodBody", methodBody);
			
			methodResultMap.put(methodName, methodMap);
			
			// 第一个如果是冒号
			if (methodContent.indexOf(';') == 0) methodContent = methodContent.substring(1, methodContent.length()).trim();
			
			getMethodResultMap(methodContent, "", methodResultMap);
		}
		
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
		if (sourceText.indexOf("<--") == 0 || sourceText.indexOf("/**") == 0 || sourceText.indexOf("//") == 0) {
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
				
				apiNameValue = sourceText.substring(sourceText.indexOf(':') + 1, sourceText.length()).trim();
				
				if ("'\"".indexOf(apiNameValue.charAt(0)) > -1) apiNameValue = apiNameValue.substring(1, apiNameValue.length() - 1);
				
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
						
						apiNameValue = tempText.trim();
						
						apiNameValue = apiNameValue.substring(1, apiNameValue.length());
						
						apiNameValue = apiNameValue.substring(0, apiNameValue.indexOf(endChar));
						
					} else {
						
						endIndex = sourceText.indexOf(',');
					}
					
				} else if (',' == charTypeValue) {
					
					apiNameValue = "";
					
					apiNameEndChar = ",";
					
					endIndex = sourceText.indexOf(',');
					
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
	 * 获取react 生命周期升级为hooks 的处理结果
	 * 
	 * @param classPropsResultMap
	 * @return
	 */
	public static String getLifecleMethod(Map<String, Map<String, String>> classPropsResultMap) {
		
		/*
		 
		React.useEffect(()=>{
	        // 请求数据 ， 事件监听 ， 操纵dom  ， 增加定时器 ， 延时器 
	        console.log('组件挂载完成：componentDidMount')
	        return function componentWillUnmount(){
	            // 解除事件监听器 ，清除
	            console.log('组件销毁：componentWillUnmount')
	        }
	    },[])//
	​
	    React.useEffect(()=>{
	        console.log('props变化：componentWillReceiveProps')
	    },[ props ])
	    
	    React.useEffect(()=>{ 
	        console.log(' 组件更新完成：componentDidUpdate ')
	    })
	    
	    */
		
		replaceContent = "";
		
		String tempText = "";
		String lifeMethod = "";
		String methodContent = "";
		String willmoutMethod = "";
		String didAndUnmoutMethod = "";
		
		int endIndex = -1;
		
		Map<String, String> methodMap = new HashMap<>();
	
		if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[0])) {
			
			methodMap = classPropsResultMap.get(ConvertParam.ReactClassLifeMethodList[0]);
			
			methodContent = methodMap.get("methodBody");
			
			methodContent = methodContent.substring(1, methodContent.length() - 1);
			
			// 去除super  bind 内容后 ，剩余的处理到didmount对应的方法中
			endIndex = TxtContentUtil.getKeyWordIndex(methodContent, "super");
			
			if (endIndex != -1) {
				
				tempText = methodContent.substring(endIndex, methodContent.length());
				
				endIndex = TxtContentUtil.getStatementEndIndex(tempText, 0);
				
				replaceContent = tempText.substring(0, endIndex + 1);
				
				methodContent = methodContent.replace(replaceContent, "");
			}
			
			methodContent = clearContructorStateAndProps(methodContent, "this.state");
			methodContent = clearContructorStateAndProps(methodContent, "this.props");
			
			methodContent = clearBindThisInfo(methodContent);
			
			methodContent = findThisRefAndClearDefine(methodContent, "");
			
		}
		
		if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[1])) {
			
			methodMap = classPropsResultMap.get(ConvertParam.ReactClassLifeMethodList[1]);
			
			tempText = methodMap.get("methodBody");
			
			tempText = TxtContentUtil.getContentByTag(tempText, 0, '{', '}');
			
			tempText = replaceThisKeyWordOfMethod(tempText);
			
			tempText = tempText.substring(1, tempText.length() - 1);
			
			willmoutMethod = tempText.trim() + "\n";
		}
		
		if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[2])) {
			
			methodMap = classPropsResultMap.get(ConvertParam.ReactClassLifeMethodList[2]);
			
			tempText = methodMap.get("methodBody");
			
			tempText = replaceThisKeyWordOfMethod(tempText);
			
			didAndUnmoutMethod = "useEffect(()=>{\n";
			
			tempText = TxtContentUtil.getContentByTag(tempText, 0, '{', '}');
			
			tempText = tempText.substring(1, tempText.length() - 1);
			
			// willmount content
			if (!"".equals(willmoutMethod)) tempText = willmoutMethod + "\n" + tempText.trim();
			
			// constructor content
			if (!"".equals(methodContent)) tempText = methodContent + "\n" + tempText.trim();
			
			didAndUnmoutMethod += tempText.trim() + "\n";
			
		}
		
		if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[3])) {
			
			methodMap = classPropsResultMap.get(ConvertParam.ReactClassLifeMethodList[3]);
			
			tempText = methodMap.get("methodBody");
			
			tempText = replaceThisKeyWordOfMethod(tempText);
			
			if ("".equals(didAndUnmoutMethod)) {
				
				didAndUnmoutMethod = "useEffect(()=>{\n";
				
				if (!"".equals(willmoutMethod)) didAndUnmoutMethod = didAndUnmoutMethod + willmoutMethod;
			}
			
			didAndUnmoutMethod += "return function componentWillUnmount(){\n";// or return ()=>{}
			
			tempText = TxtContentUtil.getContentByTag(tempText, 0, '{', '}');
			
			tempText = tempText.substring(1, tempText.length() - 1);
			
			didAndUnmoutMethod += tempText.trim() + "\n";
			
			didAndUnmoutMethod += "}";
			
			didAndUnmoutMethod += "},[])";
			
		} else if (!"".equals(didAndUnmoutMethod)) {
			
			didAndUnmoutMethod += "},[])";
		}
		
		if (!"".equals(didAndUnmoutMethod)) {
			
			lifeMethod += didAndUnmoutMethod + "\n";
		}
		
		if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[4])) {
			
			methodMap = classPropsResultMap.get(ConvertParam.ReactClassLifeMethodList[4]);
			
			tempText = methodMap.get("methodBody");
			
			tempText = replaceThisKeyWordOfMethod(tempText);
			
			lifeMethod += "useEffect(()=>{\n";
			
			tempText = TxtContentUtil.getContentByTag(tempText, 0, '{', '}');
			
			// 将参数替换为props
			String methodParam = methodMap.get("methodParams");
			
			if (!"".equals(methodParam)) {
				
				for (String propsParam:methodParam.split(",")) {
					
					if (!"".equals(propsParam)) {
						
						if (propsParam.indexOf(':') > -1) propsParam = propsParam.substring(0, propsParam.indexOf(':'));
						
						tempText = replaceThisOfReactMethod(tempText, propsParam.trim() + ".", "", "props.");
					}
				}
			}
			
			tempText = tempText.substring(1, tempText.length() - 1);
			
			lifeMethod += tempText.trim() + "\n";
			
			lifeMethod += "},[ props ])\n";
		}
		
		if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[5])) {
			
			methodMap = classPropsResultMap.get(ConvertParam.ReactClassLifeMethodList[5]);
			
			tempText = methodMap.get("methodBody");
			
			tempText = replaceThisKeyWordOfMethod(tempText);
			
			lifeMethod += "useEffect(()=>{\n";
			
			tempText = TxtContentUtil.getContentByTag(tempText, 0, '{', '}');
			
			tempText = tempText.substring(1, tempText.length() - 1);
			
			lifeMethod += tempText.trim() + "\n";
			
			lifeMethod += "})\n";
		}
		
		return lifeMethod;
	}
	
	/**
	 * 获取react class 组件除了生命周期函数外的其他函数
	 * 
	 * @param classPropsResultMap
	 * @return
	 */
	public static String getClassNormalMethod(Map<String, Map<String, String>> classPropsResultMap) {
		
		/*
		 
		 export default function Hello() {
		 
		    const clickEvent = () => {
		        console.log('2');
		    }
		    
		    const getParams = (params) => {
		        console.log(params);
		    }
		    
		    return (
		        <>
		            <button onClick={() => {
		                console.log('1');
		            }}>按钮</button>
		            <button onClick={clickEvent}>按钮</button>
		            <button onClick={() => getParams('3')}>传参</button>
		        </>
		    )
		}
		 
		 */
		
		String otherMethod = "";
		
		String methodName = "";
		String methodParams = "";
		String methodBody = "";
		String methodDescription = "";
		
		Boolean isNotLifecleMethod = false;
		
		Map<String, String> methodMap = new HashMap<>();
		
		for (Map.Entry<String, Map<String, String>> methodInfoMap : classPropsResultMap.entrySet()) {
			
			isNotLifecleMethod = false;
			
			for (String lifecleMethodKey:ConvertParam.ReactClassLifeMethodList) {
				
				if (lifecleMethodKey.equals(methodInfoMap.getKey())) {
					
					isNotLifecleMethod = true;
					break;
				}
			}
			
			if (!isNotLifecleMethod) {
				
				methodMap = methodInfoMap.getValue();
				
				methodName = methodMap.get("methodName");
				methodParams = methodMap.get("methodParams");
				methodBody = methodMap.get("methodBody");
				methodDescription = methodMap.get("methodDescription");
				
				otherMethod += methodDescription + "\n";
				
				methodBody = replaceThisKeyWordOfMethod(methodBody);
				
				otherMethod += "const " + methodName + " = (" + methodParams + ") => " + methodBody + "\n";
			}
		}
		
		return otherMethod;
	}
	
	public static String preReplaceThisOfReactClass(String methodBodyContent, String originValue, String repalceValue) {
		
		methodBodyContent = replaceThisOfReactMethod(methodBodyContent, "this.", originValue, repalceValue);
		
		return methodBodyContent;
	}
	
	/**
	 * 
	 * 
	 * @param methodContent
	 * @return
	 */
	public static String clearContructorStateAndProps(String methodContent, String type) {
		
		if (methodContent.indexOf(type) > -1) {
			
			replaceContent = "";
			
			String tempText = "";
			
			int endIndex = -1;
			
			tempText = methodContent.substring(methodContent.indexOf(type), methodContent.length());
			
			replaceContent = tempText;
			
			tempText = tempText.substring(type.length(), tempText.length());
			
			if ('=' == tempText.trim().charAt(0)) {
				
				tempText = tempText.substring(tempText.indexOf('=') + 1, tempText.length());
				
				if ('{' == tempText.trim().charAt(0)) {
					
					endIndex = TxtContentUtil.getTagEndIndex(tempText, '{', '}');
					
					endIndex += replaceContent.indexOf('=') + 1 + 1;
				} else {
					
					endIndex = replaceContent.indexOf('=') + 1 + TxtContentUtil.getStatementEndIndex(tempText, 0);
				}
				
				replaceContent = replaceContent.substring(0, endIndex + 1);
				
				methodContent = methodContent.replace(replaceContent, "");
				
			}		
					
		}
		
		return methodContent;
	}
	
	/**
	 * 
	 * 
	 * @param methodContent
	 * @return
	 */
	public static String clearBindThisInfo(String methodContent) {
		
		replaceContent = "";
		
		String tempText = "";
		
		int endIndex = methodContent.indexOf(".bind(this)");
		
		if (endIndex > -1) {
			
			tempText = methodContent.substring(0, endIndex);
			
			endIndex = 0;
			
			for (int k=tempText.length() - 1;k>-1;k--) {
				
				if (k > 5 && "this.".equals(tempText.substring(k - 5, k))) {
					
					endIndex++;
				}
				
				if (endIndex == 2) {
					endIndex = k - 5;
					break;
				}
			}
			
			tempText = methodContent.substring(endIndex, methodContent.length());
			
			endIndex = TxtContentUtil.getStatementEndIndex(tempText, 0);
			
			replaceContent = tempText.substring(0, endIndex + 1);
			
			methodContent = methodContent.replace(replaceContent, "");
			
			endIndex = methodContent.indexOf(".bind(this)");
			
			if (endIndex > -1) clearBindThisInfo(methodContent);
		}
		
		return methodContent;
	}
	
	/**
	 * 清除class 的this 引用
	 * 
	 * 是否有 let /const /var xxx = this
	 * 
	 * @param methodContent
	 * @return
	 */
	public static String findThisRefAndClearDefine(String methodContent, String replaceTxt) {
		
		replaceContent = "";
		
		String tempText = findReactClassThisRef(methodContent);
		
		if (!"".equals(tempText)) {
			
			methodContent = methodContent.replace(replaceContent, "");
			
			methodContent = methodContent.replaceAll(tempText + ".", replaceTxt);
		}
					
		return methodContent;
	}
	
	/**
	 * 
	 * 
	 * @param methodContent
	 * @return
	 */
	public static String findReactClassThisRef(String methodContent) {
		
		String tempText = "";
		String thisRefName = "";
		
		int startIndex = -1;
		int endIndex = -1;
		
		for (int m=0;m<methodContent.length();m++) {
			
			if ('{' == methodContent.charAt(m)) {
				
				tempText = methodContent.substring(m, methodContent.length());
				
				endIndex = TxtContentUtil.getTagEndIndex(tempText, '{', '}');
				
				m += endIndex;
			} else if ('=' == methodContent.charAt(m)) {
				
				tempText = methodContent.substring(m + 1, methodContent.length());
				
				if (tempText.trim().indexOf("this") == 0 && tempText.trim().indexOf("this.") != 0 && tempText.trim().indexOf("this[") != 0) {
					
					endIndex = TxtContentUtil.getStatementEndIndex(tempText, 0);
					
					tempText = methodContent.substring(0, m + 1);
					
					for (int k=tempText.length()-1;k>-1;k--) {
						
						if (k > 6 && "const ".equals(tempText.substring(k-6, k))) {
							startIndex = k-6;
							break;
						}
						
						if (k > 4 && ("var ".equals(tempText.substring(k-4, k)) || "let ".equals(tempText.substring(k-4, k)))) {
							startIndex = k-4;
							break;
						}
					}
					
					replaceContent = methodContent.substring(startIndex, m + 1 + endIndex + 1);
					
					thisRefName = replaceContent.substring(replaceContent.indexOf(" ") + 1, replaceContent.indexOf("=")).trim();
					
					break;
					
				}
			}
		}
			
		return thisRefName;
	}
	
	/**
	 * 
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String getReactRenderIndex(String sourceText) {
		
		String tempText = "";
		
		int startIndex = -1;
		
		startIndex = TxtContentUtil.getKeyWordIndex(sourceText, "ReactDOM.render");
		
		if (startIndex == -1) return "";
		
		tempText = sourceText.substring(startIndex, sourceText.length());
		
		startIndex = TxtContentUtil.getStatementEndIndex(tempText, 0);
		
		tempText = tempText.substring(0, startIndex);
		
		return tempText;
	}
	
	/**
	 * 处理方法体中的this
	 * 
	 * @param methodContent
	 * @return
	 */
	public static String replaceThisKeyWordOfMethod(String methodContent) {
		
		String tempText = "";
		
		// 1. this 别名引用更回
		methodContent = findThisRefAndClearDefine(methodContent, "this.");
		
		// 2. this.state => fcState
		tempText = classDataMap.get("fcState");
		
		methodContent = preReplaceThisOfReactClass(methodContent, "state.", tempText + ".");
		
		// 3. this.setState => renderMethodName() + fcSetState + renderMethodReturnContent
		tempText = classDataMap.get("renderMethodName");
		
		if ("".equals(tempText)) {
			
			tempText = classDataMap.get("fcSetState");
			
			methodContent = preReplaceThisOfReactClass(methodContent, "setState", tempText);
		} else {
			
			if (methodContent.indexOf("this.setState(") > -1) {
				
				methodContent = "let " + classDataMap.get("fcState") + "RenderStateData = "+ tempText + "();\n" + methodContent;
			}
			
			methodContent = replaceClassSetStateContent(methodContent);
		}
		
		// 4. this.props => props
		methodContent = preReplaceThisOfReactClass(methodContent, "props.", "props.");
		
		// 5. this. => ""
		methodContent = preReplaceThisOfReactClass(methodContent, "", "");
		
		return methodContent;
	}
	
	public static String replaceThisOfReactMethod(String sourceText, String thisKeyWord, String KeyWord, String wordType) {
		
		sourceText = TxtContentUtil.replaceThisOfFrameWorkContent(sourceText, thisKeyWord, KeyWord, "", wordType);
		
		return sourceText;
	}
	
	/**
	 * 替换内容中的this.setState 信息并增加renderMethod及返回信息
	 * 
	 * @param currentMethodTxt
	 * @return
	 */
	public static String replaceClassSetStateContent(String currentMethodTxt) {
		
		replaceContent = "";
		
		String tempTxt = "";
		
		int startIndex = -1;
		int endIndex = -1;
		
		if (currentMethodTxt.indexOf("this.setState(") > -1) {
			
			tempTxt = currentMethodTxt.substring(currentMethodTxt.indexOf("this.setState("), currentMethodTxt.length());
			
			startIndex = TxtContentUtil.getTagEndIndex(tempTxt, '(', ')');
			
			tempTxt = tempTxt.substring(0, startIndex);
			
			replaceContent = tempTxt;
			
			if ('{' == tempTxt.substring(tempTxt.indexOf('(') + 1, tempTxt.length()).trim().charAt(0)) {
				
				endIndex = TxtContentUtil.getTagEndIndex(tempTxt, '{', '}');
				
				// 解构方式
				tempTxt = tempTxt.substring(0, endIndex) + ", ..." + classDataMap.get("fcState") + "RenderStateData" + tempTxt.substring(endIndex, tempTxt.length());
			} else {
				
				String stateVariable = tempTxt.substring(tempTxt.indexOf('(') + 1, startIndex);
				
				// 合并处理
				tempTxt = tempTxt.substring(0, tempTxt.indexOf('(') + 1) + "Object.assign(" + stateVariable + "," + classDataMap.get("fcState") + "RenderStateData" + ")" + tempTxt.substring(startIndex, tempTxt.length());
			}
			
			tempTxt = tempTxt.replace("this.setState(", classDataMap.get("fcSetState"));
			
			return currentMethodTxt.substring(0, startIndex).replace(replaceContent, tempTxt) + replaceClassSetStateContent(currentMethodTxt.substring(startIndex, currentMethodTxt.length()));
		}
		
		return currentMethodTxt;
	}
	
	/**
	 * 处理根节点
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String processJsxRootTag(String sourceText) {
		
		String tempText = sourceText.substring(sourceText.indexOf('(') + 1, sourceText.lastIndexOf(')'));
		
		replaceContent = tempText;
		
		tempText = tempText.trim();
		
		if (tempText.indexOf("<div") == 0) {
			
			tempText = tempText.substring("<div".length(), tempText.indexOf('>'));
			
			if ("".equals(tempText.trim())) {
				
				tempText = tempText.replace(tempText.substring(0, tempText.indexOf('>') + 1), "<>");
				
				tempText = tempText.substring(0, tempText.lastIndexOf("</div")) + tempText.substring(tempText.lastIndexOf("</div"), tempText.length()).replace("</div", "</");
				
				sourceText = sourceText.replace(replaceContent, tempText);
			}
		}
		
		return sourceText;
	}
	
	/**
	 * 处理hooks 引入
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String processReactDOMImport(String sourceText) {
		
		replaceContent = "";
		
		String fromKey = "";
		String importContent = "";
		
		// 判断原先内容中是否有from 这个库，有则替换，无则插入
		if (sourceText.indexOf(" from 'react-dom'") > -1) {
			fromKey = " from 'react-dom'";
		} else if (sourceText.indexOf(" from \"react-dom\"") > -1) {
			fromKey = " from \"react-dom\"";
		}
		
		if ("".equals(fromKey)) {
			
			importContent = "import ReactDOM from 'react-dom';\n";
			
			sourceText = importContent + sourceText;
		}
		
		return sourceText;
	}
	
	/**
	 * 处理hooks 引入
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String processHooksImport(String sourceText, String importHooks) {
		
		replaceContent = "";
		
		String tempText = "";
		String fromKey = "";
		String importContent = "";
		
		// 判断原先内容中是否有from 这个库，有则替换，无则插入
		if (sourceText.indexOf(" from 'react'") > -1) {
			fromKey = " from 'react'";
		} else if (sourceText.indexOf(" from \"react\"") > -1) {
			fromKey = " from \"react\"";
		}
		
		if (!"".equals(fromKey)) {
			
			tempText = sourceText.substring(0, sourceText.indexOf(fromKey));
			
			tempText = tempText.substring(tempText.lastIndexOf("import "), tempText.length());
			
			replaceContent = tempText;
			
			if (tempText.indexOf('}') > -1) {
				
				tempText = tempText.substring(0, tempText.indexOf('}')) + ", "  + importHooks + " " + tempText.substring(tempText.indexOf('}'), tempText.length());
			} else {
				
				tempText += ", { " + importHooks + " }";
			}
			
			sourceText = sourceText.replace(replaceContent, tempText);
		} else {
			
			importContent = "import { " + importHooks + " } from 'react';\n";
			
			sourceText = importContent + sourceText;
		}
		
		return sourceText;
	}
	
	/**
	 * 处理文件格式
	 * 
	 * @param sourceText
	 * @return
	 */
	public static String processFileContentFormat(String sourceText) {
		
		sourceText = TxtContentUtil.clearLineStartBlankContent(sourceText);
		
		//sourceText = TxtContentUtil.processJSContentFormat(sourceText, 2);
		
		return sourceText;
	}
	
	/**
	 * 判断文件内容是否是react class 组件
	 * 
	 * @param sourceText
	 * @return
	 */
	public static Boolean isReactClassFileContent(String sourceText) {
		
		return sourceText.indexOf("class ") == 0 || (sourceText.indexOf("class ") > 0 && !String.valueOf(sourceText.charAt(sourceText.indexOf("class ") - 1)).matches(ConvertParam.JS_VARIABLE_REG));
	}
}
