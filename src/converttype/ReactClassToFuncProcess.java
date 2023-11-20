package converttype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.ConvertParam;
import utils.ConvertLogUtil;
import utils.ReactProcessUtil;
import utils.TxtContentUtil;

/**
 * 
 * @author 郑荣鸿（ChengWingHung）
 * @date 20231116
 * @description react类组件升级为函数组件处理类
 * @version 1.0.0 beta
 *
 */

public class ReactClassToFuncProcess {
	
	private static String parseFileName;// 当前要解析的文件名
	
	private static Map<String, Map> parseResultMap;// 解析后的信息存储对象
	
	private static Map<String, Map<String, String>> classPropsResultMap;// class props map

	public ReactClassToFuncProcess() {
		
	}
	
	public static String parseReactFileContent(String fileName, String parseResultContent) {
		
		// 判断是不是class 类组件
		if (!ReactProcessUtil.isReactClassFileContent(parseResultContent)) return parseResultContent;
		
		parseFileName = fileName;
		
		parseResultMap = new HashMap<>();
		
		classPropsResultMap = new HashMap<>();
		
		ReactProcessUtil.classDataMap = new HashMap<>();
		
		ConvertLogUtil.printConvertLog("info", "解析前：\n" + parseResultContent);
		
		getImportAndDefineContent(parseResultContent);
		
		parseResultContent = getReactClassName(parseResultContent);
		
		parseResultContent = parseResultContent.substring(parseResultContent.indexOf('{'), parseResultContent.lastIndexOf('}') + 1);
		
		ReactProcessUtil.getMethodResultMap(parseResultContent.substring(1, parseResultContent.length() - 1), "", classPropsResultMap);  
		
		getReactStateInfo(parseResultContent);
		
		getReactPropsInfo(parseResultContent);
		
		getReactDomRenderContent(parseResultContent);
		
		String parseReactResultContent = getAssembleReactFuncContent();
		
		parseReactResultContent = ReactProcessUtil.processFileContentFormat(parseReactResultContent);
		
		ConvertLogUtil.printConvertLog("info", "解析后：\n" + parseReactResultContent);
		
		return parseReactResultContent;
	}
	
	/**
	 * 得到class 前面部分的信息
	 * 
	 * @param parseResultContent
	 */
	public static void getImportAndDefineContent(String parseResultContent) {
		
		String tempText = "";
		
		if ("".equals(parseResultContent.substring(0, parseResultContent.indexOf("class ")).trim())) {
			// 无需处理
			return;
		}
		
		tempText = parseResultContent.substring(0, parseResultContent.indexOf("class "));
		
		tempText = tempText.substring(0, tempText.lastIndexOf("\n"));
		
		Map<String, String> importAndDefineMap = new HashMap<>();
		
		importAndDefineMap.put("contentValue", tempText);
		
		parseResultMap.put("importDefine", importAndDefineMap);
	}
	
	/**
	 * 得到class 类名
	 * 
	 * @param parseResultContent
	 */
	public static String getReactClassName(String parseResultContent) {
		
		String tempText = "";
		
		int startIndex = parseResultContent.indexOf("class ") + "class ".length();
		
		tempText = parseResultContent.substring(startIndex, parseResultContent.length());
		
		tempText = tempText.substring(0, TxtContentUtil.getNotVariableIndex(tempText, 0));
		
		Map<String, String> classNameMap = new HashMap<>();
		
		classNameMap.put("contentValue", tempText);
		
		parseResultMap.put("className", classNameMap);
		
		parseResultContent = parseResultContent.substring(startIndex, parseResultContent.length());
		
		// :React.FC<IProps>
		tempText = parseResultContent.substring(0, parseResultContent.indexOf('{')).trim();
		
		if (tempText.indexOf('<') > -1 && tempText.indexOf('>') == tempText.length() - 1) {
			
			tempText = tempText.substring(tempText.indexOf('<') + 1, tempText.length() - 1);
			
			Map<String, String> interfaceNameMap = new HashMap<>();
			
			interfaceNameMap.put("contentValue", tempText);
			
			parseResultMap.put("interFaceName", interfaceNameMap);
		}
		
		return parseResultContent;
		
	}
	
	/**
	 * 得到state 状态信息
	 * 
	 * @param parseResultContent
	 */
	public static void getReactStateInfo(String parseResultContent) {
		
		String tempText = "";
		
		Map<String, String> methodMap = new HashMap<>();
		
		// 先判断是否有state 不在constructor 中定义的内容，否则在constructor 中拿
		if (classPropsResultMap.containsKey("state")) {
			
			methodMap = classPropsResultMap.get("state");
			
			tempText = methodMap.get("methodBody");
			
		} else if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[0])) {
			
			methodMap = classPropsResultMap.get(ConvertParam.ReactClassLifeMethodList[0]);
			
			tempText = methodMap.get("methodBody");
			
			if (tempText.indexOf("this.state") > -1) {
				
				tempText = tempText.substring(tempText.indexOf("this.state") + "this.state".length(), tempText.length());
				
				tempText = TxtContentUtil.getContentByTag(tempText, 0, '{', '}');
				
			} else {
				// 无状态信息不需要处理
				methodMap = new HashMap<>();
			}
		}
		
		if (methodMap.size() > 0) {
			
			tempText = tempText.substring(tempText.indexOf('{'), tempText.lastIndexOf('}') + 1);
			
			tempText = ReactProcessUtil.preReplaceThisOfReactClass(tempText, "", "");
			
			Map<String, String> stateDetailMap = new HashMap<>();
			
			stateDetailMap.put("stateValue", tempText);
			
			parseResultMap.put("state", stateDetailMap);
		}
		
	}
	
	/**
	 * 得到props 信息
	 * 
	 * @param parseResultContent
	 */
	public static void getReactPropsInfo(String parseResultContent) {
		
		String tempText = "";
		
		Map<String, String> methodMap = new HashMap<>();
		
		if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[0])) {
			
			methodMap = classPropsResultMap.get(ConvertParam.ReactClassLifeMethodList[0]);
			
			tempText = methodMap.get("methodBody");
			
			if (tempText.indexOf("this.props") > -1) {
				
				tempText = tempText.substring(tempText.indexOf("this.props") + "this.props".length(), tempText.length());
				
				tempText = tempText.substring(tempText.indexOf('{'), tempText.lastIndexOf('}') + 1);
				
				Map<String, Map<String, String>> propsDetailMap = new HashMap<>();
				
				ReactProcessUtil.getPropertyDetailOfObject(tempText.substring(1, tempText.length() - 1), propsDetailMap, 1);
				
				parseResultMap.put("props", propsDetailMap);
			}
		}
	}
	
	/**
	 * 得到ReactDOM.render信息
	 * 
	 * @param parseResultContent
	 */
	public static void getReactDomRenderContent(String parseResultContent) {
		
		String reactDomRender = ReactProcessUtil.getReactRenderIndex(parseResultContent);
		
		if (!"".equals(reactDomRender)) {
			
			Map<String, String> domRenderMap = new HashMap<>();
			
			domRenderMap.put("renderContent", reactDomRender);
			
			parseResultMap.put("domRender", domRenderMap);
		}
		
	}
	
	/**
	 * 得到return 的jsx 内容
	 * 
	 * @param parseResultContent
	 */
	public static void getReactReturnJSXContent(String parseResultContent) {
		
		String tempText = "";
		
		Map<String, String> methodMap = new HashMap<>();
		
		if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[8])) {
			
			methodMap = classPropsResultMap.get(ConvertParam.ReactClassLifeMethodList[8]);
			
			tempText = methodMap.get("methodBody");
			
			
		}
	}
	
	/**
	 * 组装react function 的内容
	 * 
	 * @param parseResultContent
	 */
	public static String getAssembleReactFuncContent() {
		
		Boolean importUseEffect = false;// 是否需要引入useEffect
		
		String tempText = "";
		String fcName = "";
		String fcState = "";
		String fcSetState = "";
		String jsxContent = "";
		String renderMethodName = "";
		String renderMethodContent = "";
		String renderMethodReturnContent = "";
		String parseReactResultContent = "";
		
		int endIndex = -1;
		
		parseReactResultContent = "function ";
		
		// class 
		fcName = (String)parseResultMap.get("className").get("contentValue");
		
		// 存在接口的情况 :React.FC<IProps> 
		if (parseResultMap.containsKey("interFaceName")) {
			
			parseReactResultContent += fcName + ":React.FC<" + parseResultMap.get("interFaceName").get("contentValue") + ">=(props)=>{\n";
		} else {
			
			parseReactResultContent += fcName + "(props) {\n";
		}
		
		fcState = String.valueOf(fcName.charAt(0)).toLowerCase() +  fcName.substring(1, fcName.length()) + "State";
		
		fcSetState = "set" + fcName + "State";
		
		parseReactResultContent += "const [" + fcState + ", " + fcSetState + "] = useState(";
		
		// state 把class 的state包装成一个object ，这样对于function 就一个state
		if (parseResultMap.containsKey("state")) {
			
			parseReactResultContent += parseResultMap.get("state").get("stateValue");
			
		}
		
		parseReactResultContent += ");\n";
		
		// render
		Map<String, String> renderMethodMap = classPropsResultMap.get(ConvertParam.ReactClassLifeMethodList[8]);
			
		tempText = renderMethodMap.get("methodBody");
		
		jsxContent = tempText.substring(tempText.lastIndexOf("return"), tempText.lastIndexOf(')') + 1);
		
		tempText = tempText.substring(tempText.indexOf('{') + 1, tempText.indexOf(jsxContent));
		
		// render 和 return 之间是否有内容
		if (!"".equals(tempText.trim())) {
			
			tempText = tempText.trim();
			
			tempText = ReactProcessUtil.preReplaceThisOfReactClass(tempText, "state.", fcState + ".");
			tempText = ReactProcessUtil.preReplaceThisOfReactClass(tempText, "props.", "props.");
			tempText = ReactProcessUtil.preReplaceThisOfReactClass(tempText, "", "");
			
			String renderContent = tempText;
			
			// 去除this.state 解构赋值信息
			if (tempText.indexOf("this.state") > -1) {
				
				tempText = tempText.substring(tempText.indexOf("this.state"), tempText.length());
				
				endIndex = tempText.indexOf("this.state") + TxtContentUtil.getStatementEndIndex(tempText, 0);
				
				tempText = renderContent.substring(0, endIndex);
				
				endIndex = -1;
				
				for (int j=tempText.length() - 1;j>-1;j--) {
					
					// let const var
					if (j > 4 && "const".equals(tempText.substring(j-5, j))) {
						
						endIndex = j-5;
						break;
					} else if (j > 2 && ("let".equals(tempText.substring(j-3, j)) || "var".equals(tempText.substring(j-3, j)))) {
						
						endIndex = j-3;
						break;
					}
				}
				
				if (endIndex == -1) endIndex = 0;
				
				tempText = tempText.substring(endIndex, tempText.length());
				
				renderContent = renderContent.replace(tempText, "");
				
				// 拿到变量
				tempText = TxtContentUtil.getContentByTag(tempText, 0, '{', '}');
				
				// renderContent 和 jsxContent 涉及到的state变量替换
				for(String stateVariable:tempText.split(",")) {
					
					if (!"".equals(stateVariable)) {
						
						renderContent = TxtContentUtil.replaceThisOfFrameWorkContent(renderContent, "", stateVariable,  "", fcState + "." + stateVariable);
						jsxContent = TxtContentUtil.replaceThisOfFrameWorkContent(jsxContent, "", stateVariable,  "", fcState + "." + stateVariable);
					}
				}
			}
			
			// 去除this.props 结构赋值信息
			if (tempText.indexOf("this.props") > -1) {
				
				tempText = tempText.substring(tempText.indexOf("this.props"), tempText.length());
				
				endIndex = tempText.indexOf("this.props") + TxtContentUtil.getStatementEndIndex(tempText, 0);
				
				tempText = renderContent.substring(0, endIndex);
				
				endIndex = -1;
				
				for (int j=tempText.length() - 1;j>-1;j--) {
					
					// let const var
					if (j > 4 && "const".equals(tempText.substring(j-5, j))) {
						
						endIndex = j-5;
						break;
					} else if (j > 2 && ("let".equals(tempText.substring(j-3, j)) || "var".equals(tempText.substring(j-3, j)))) {
						
						endIndex = j-3;
						break;
					}
				}
				
				if (endIndex == -1) endIndex = 0;
				
				tempText = tempText.substring(endIndex, tempText.length());
				
				renderContent = renderContent.replace(tempText, "");
				
				// 拿到变量
				tempText = TxtContentUtil.getContentByTag(tempText, 0, '{', '}');
				
				// renderContent 和 jsxContent 涉及到的props变量替换
				for(String propsVariable:tempText.split(",")) {
					
					if (!"".equals(propsVariable)) {
						
						renderContent = TxtContentUtil.replaceThisOfFrameWorkContent(renderContent, "", propsVariable,  "", "props." + propsVariable);
						jsxContent = TxtContentUtil.replaceThisOfFrameWorkContent(jsxContent, "", propsVariable,  "", "props." + propsVariable);
					}
				}
			}
			
			if (!"".equals(renderContent)) {
				
				tempText = "";
				
				ArrayList<String> variableNameList = new ArrayList<String>();
				
				// 除了state 和 props 外的变量部分处理为state 的变量
				TxtContentUtil.getDefineVariable(renderContent, variableNameList);
				
				for(String defineVariable:variableNameList) {
					
					if (!"".equals(defineVariable)) {
						
						tempText += defineVariable + ", ";
						
						jsxContent = TxtContentUtil.replaceJsxContentStateVariable(jsxContent, defineVariable, fcState + "." + defineVariable);
					}
				}
				
				renderMethodName = fcName + "RenderMehod";
				
				if (!"".equals(tempText)) {
					
					renderMethodContent = "const " + renderMethodName + " = (" + fcState + ") => {\n";					
					
					renderMethodContent += renderContent.trim() + "\n";
					
					renderMethodReturnContent = tempText.substring(0, tempText.length() - 2);
					
					renderMethodContent += "return { " + renderMethodReturnContent + " };\n";
				} else {
					
					renderMethodContent = "const " + renderMethodName + " = () => {\n";
					
					renderMethodContent += renderContent.trim() + "\n";
				}
				
				renderMethodContent += "}\n";
			}
			
		}
		
		ReactProcessUtil.classDataMap.put("fcName", fcName);
		ReactProcessUtil.classDataMap.put("fcState", fcState);
		ReactProcessUtil.classDataMap.put("fcSetState", fcSetState);
		ReactProcessUtil.classDataMap.put("renderMethodName", renderMethodName);
		ReactProcessUtil.classDataMap.put("renderMethodReturnContent", renderMethodReturnContent);
		
		jsxContent = ReactProcessUtil.preReplaceThisOfReactClass(jsxContent, "state.", fcState + ".");
		jsxContent = ReactProcessUtil.preReplaceThisOfReactClass(jsxContent, "props.", "props.");
		jsxContent = ReactProcessUtil.preReplaceThisOfReactClass(jsxContent, "", "");
		
		// 根节点<div></div> => <></> 待处理
		jsxContent = ReactProcessUtil.processJsxRootTag(jsxContent);
		
		// setState 的回调是否有对应的函数
		
		
		// 生命周期函数
		tempText = ReactProcessUtil.getLifecleMethod(classPropsResultMap);
		
		if (!"".equals(tempText)) importUseEffect = true;
		
		parseReactResultContent += tempText;
		
		// 普通函数
		parseReactResultContent += ReactProcessUtil.getClassNormalMethod(classPropsResultMap);
		
		// render mehod
		if (!"".equals(renderMethodContent)) parseReactResultContent += renderMethodContent;
		
		// return jsx 
		parseReactResultContent += jsxContent + "\n";
		
		parseReactResultContent += "}\n";
		
		// import define 部分
		if (parseResultMap.containsKey("importDefine")) {
			
			tempText = (String)parseResultMap.get("importDefine").get("contentValue");
			
			String importHooks = "useState";
			
			if (importUseEffect) importHooks += ", useEffect";
			
			// 增加import 内容，引入hooks
			tempText = ReactProcessUtil.processHooksImport(tempText, importHooks);
			
			// import ReactDOM from 'react-dom'
			if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[6])) {
				
				tempText = ReactProcessUtil.processReactDOMImport(tempText);
			}
			
			parseReactResultContent = tempText + "\n" + parseReactResultContent;
		}
		
		// ReactDOM.memo
		if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[6])) {
			
			tempText = "const Memo" + fcName + " = ReactDOM.memo(" + fcName + ");";
			
			parseReactResultContent += tempText + "\n";
			
			parseReactResultContent += "\n" + "export default Memo" + fcName + ";\n";
			
			if (parseResultMap.containsKey("domRender")) {
				
				parseReactResultContent += String.valueOf(parseResultMap.get("domRender").get("renderContent")).replace(fcName, "Memo" + fcName);
			}
		} else if (parseResultMap.containsKey("domRender")) {
			
			parseReactResultContent += "\n" + "export default " + fcName + ";\n";
			
			parseReactResultContent += parseResultMap.get("domRender").get("renderContent");
		} else {
			
			parseReactResultContent += "\n" + "export default " + fcName + ";\n";
		}
		
		return parseReactResultContent;
	}
	
	
}
