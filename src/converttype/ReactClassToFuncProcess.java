package converttype;

import java.util.HashMap;
import java.util.Map;

import common.ConvertParam;
import utils.ConvertLogUtil;
import utils.ReactProcessUtil;
import utils.TxtContentUtil;

/**
 * react类组件升级为函数组件处理类
 * 
 * @author 郑荣鸿（ChengWingHung）
 * @date 20231116 19:00:00 - 20231123 19:45:00
 * @description 只处理一个文件一个类组件的情况
 * @version 1.0.0
 *
 */

public class ReactClassToFuncProcess {
	
	private static String parseFileName;// 当前要解析的文件名
	
	private static Map<String, Map> parseResultMap;// 解析后的信息存储对象
	
	private static Map<String, Map<String, String>> classPropsResultMap;// class props map
	
	private static String classCreateType;// 创建class 组件的方式

	public ReactClassToFuncProcess() {
		
	}
	
	public static String parseReactFileContent(String fileName, String parseResultContent) throws Exception {
		
		// 判断是不是class 类组件
		if (!ReactProcessUtil.isReactClassFileContent(parseResultContent)) return parseResultContent;
		
		parseFileName = fileName;
		
		parseResultMap = new HashMap<>();
		
		classPropsResultMap = new HashMap<>();
		
		ReactProcessUtil.classDataMap = new HashMap<>();
		
		ReactProcessUtil.callBackMethodMap = new HashMap<>();
		
		ReactProcessUtil.callBackCount = 0;
		
		if (parseResultContent.indexOf("React.createClass(") > -1) {
			
			classCreateType = "creacteClass";
			
			return parseResultContent;// 暂不处理
		} else {
			
			classCreateType = "extendClass";
		}
		
		ConvertLogUtil.printConvertLog("local", "解析前：\n" + parseResultContent);
		
		String originParseContent = parseResultContent;
		
		parseResultContent = getReactFormCreateContent(parseResultContent);
		
		getImportAndDefineContent(parseResultContent);
		
		parseResultContent = getReactClassName(parseResultContent);
		
		getPackageClassName(originParseContent);
		
		parseResultContent = parseResultContent.substring(parseResultContent.indexOf('{'), parseResultContent.lastIndexOf('}') + 1);
		
		ReactProcessUtil.getMethodResultMap(parseResultContent.substring(1, parseResultContent.length() - 1), "", classPropsResultMap);  
		
		getReactStateInfo(parseResultContent);
		
		getReactPropsInfo(parseResultContent);
		
		getReactDomRenderContent(parseResultContent);
		
		String parseReactResultContent = getAssembleReactFuncContent();
		
		parseReactResultContent = ReactProcessUtil.processFileContentFormat(parseReactResultContent);
		
		ConvertLogUtil.printConvertLog("local", "解析后：\n" + parseReactResultContent);
		
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
	 * @return String
	 */
	public static String getReactClassName(String parseResultContent) {
		
		String tempText = "";
		
		int startIndex = parseResultContent.indexOf("class ") + "class ".length();
		
		tempText = parseResultContent.substring(startIndex, parseResultContent.length());
		
		tempText = tempText.substring(0, TxtContentUtil.getNotVariableIndex(tempText, 0));
		
		tempText = String.valueOf(tempText.charAt(0)).toUpperCase() + tempText.substring(1, tempText.length());// 首字母要大写
		
		Map<String, String> classNameMap = new HashMap<>();
		
		classNameMap.put("contentValue", tempText);
		
		parseResultMap.put("className", classNameMap);
		
		ReactProcessUtil.classDataMap.put("fcName", tempText);
		
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
	
	public static void getPackageClassName(String parseResultContent) {
		
		String tempText = "";
		String packageClassName = "";
		
		tempText = parseResultContent.substring(0, parseResultContent.indexOf("class ")).trim();
		
		// 判断class 前面是否有括号
		if ('(' == tempText.charAt(tempText.length() - 1)) {
			
			packageClassName = tempText.substring(tempText.lastIndexOf(' '), tempText.length() - 1);
		}
		
		// 判断类名前面是否有括号
		if ("".equals(packageClassName)) packageClassName = ReactProcessUtil.getClassPackageInfo(parseResultContent, (String)parseResultMap.get("className").get("contentValue"));
		
		if (!"".equals(packageClassName)) {
			
			Map<String, String> packageNameMap = new HashMap<>();
			
			packageNameMap.put("contentValue", packageClassName);
			
			parseResultMap.put("packageClassName", packageNameMap);
		}
	}
	
	/**
	 * 得到state 状态信息
	 * 
	 * @param parseResultContent
	 */
	public static void getReactStateInfo(String parseResultContent) {
		
		String tempText = "";
		String stateResultText = "";
		
		Map<String, String> methodMap = new HashMap<>();
		
		// 先判断是否有state 不在constructor 中定义的内容，否则在constructor 中取值
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
			
			stateResultText = tempText;
			
			// 是否需要插入回调函数标志
			if (ReactProcessUtil.callBackMethodMap.size() > 0) {
				
				stateResultText = stateResultText.substring(0, stateResultText.indexOf('{') + 1) + "\n" + parseResultMap.get("className").get("contentValue") + "SetStateCallBackFlg:'0',\n" + stateResultText.substring(stateResultText.indexOf('{') + 1, stateResultText.length());
			}
			
			stateDetailMap.put("stateValue", stateResultText);
			
			parseResultMap.put("state", stateDetailMap);
		}
		
	}
	
	/**
	 * 得到props 信息
	 * 
	 * @param parseResultContent
	 */
	public static void getReactPropsInfo(String parseResultContent) {
		
		if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[0])) {
			
			String tempText = "";
			
			Map<String, String> methodMap = classPropsResultMap.get(ConvertParam.ReactClassLifeMethodList[0]);
			
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
	 * 得到Form.create 内容
	 * 
	 * @param parseResultContent
	 * @return String
	 */
	public static String getReactFormCreateContent(String parseResultContent) {
		
		String formCreateContent = ReactProcessUtil.getReactFormCreateContent(parseResultContent);
		
		if (!"".equals(formCreateContent)) {
			
			parseResultContent = parseResultContent.replace(formCreateContent, "");
			
			Map<String, String> formCreateMap = new HashMap<>();
			
			formCreateMap.put("formContent", formCreateContent);
			
			String tempText = formCreateContent.substring(formCreateContent.indexOf("Form.create(") + "Form.create(".length(), formCreateContent.length());
			
			if ('{' == tempText.trim().charAt(0)) {
				
				tempText = tempText.substring(tempText.indexOf('{'), tempText.length());
				
				tempText = tempText.substring(0, TxtContentUtil.getTagEndIndex(tempText, '{', '}') + 1);
				
				if ("".equals(tempText.trim())) {
					
					formCreateMap.put("formOption", "");
				} else {
					
					formCreateMap.put("formOption", tempText);
				}
			} else {
				
				formCreateMap.put("formOption", "");
			}
			
			parseResultMap.put("formCreate", formCreateMap);
		}
		
		return parseResultContent;
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
	 * 组装react function 的内容
	 * 
	 * @return String
	 */
	public static String getAssembleReactFuncContent() {
		
		String tempText = "";
		String fcName = "";
		String fcState = "";
		String fcSetState = "";
		String jsxContent = "";
		String renderMethodContent = "";
		String parseReactResultContent = "";
		
		Boolean importUseEffect = false;// 是否需要引入useEffect
		
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
		
		ReactProcessUtil.classDataMap.put("fcState", fcState);
		ReactProcessUtil.classDataMap.put("fcSetState", fcSetState);
		
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
			
			tempText = ReactProcessUtil.preReplaceThisOfReactClass(tempText, "state", fcState);
			tempText = ReactProcessUtil.preReplaceThisOfReactClass(tempText, "props", "props");
			tempText = ReactProcessUtil.preReplaceThisOfReactClass(tempText, "", "");
			
			renderMethodContent = tempText;
		}
		
		// 生命周期函数
		tempText = ReactProcessUtil.getLifecleMethod(classPropsResultMap);
		
		if (!"".equals(tempText) || ReactProcessUtil.callBackMethodMap.size() > 0) importUseEffect = true;
		
		parseReactResultContent += tempText;
		
		// 普通函数
		parseReactResultContent += ReactProcessUtil.getClassNormalMethod(classPropsResultMap);
		
		// setState 回调函数
		parseReactResultContent += ReactProcessUtil.getSetStateCallBackMethod();
		
		// render mehod
		if (!"".equals(renderMethodContent)) parseReactResultContent += "\n" + renderMethodContent + "\n";
		
		jsxContent = ReactProcessUtil.preReplaceThisOfReactClass(jsxContent, "state", fcState);
		jsxContent = ReactProcessUtil.preReplaceThisOfReactClass(jsxContent, "props", "props");
		jsxContent = ReactProcessUtil.preReplaceThisOfReactClass(jsxContent, "", "");
		
		// 根节点<div></div> => <></> 待处理
		jsxContent = ReactProcessUtil.processJsxRootTag(jsxContent);
		
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
		
		String formOption = "";
		String formCreateContent = "";
		
		// form 表单部分是否用到了Form.create并判断options
		if (parseResultMap.containsKey("formCreate")) {
			
			formOption = (String)parseResultMap.get("formCreate").get("formOption");
			formCreateContent = (String)parseResultMap.get("formCreate").get("formContent");
		}
		
		String packageClassName = "";
		
		if (parseResultMap.containsKey("packageClassName")) {
			
			packageClassName = (String)parseResultMap.get("packageClassName").get("contentValue");
		}
		
		// React.memo
		if (classPropsResultMap.containsKey(ConvertParam.ReactClassLifeMethodList[6])) {
			
			tempText = "const Memo" + fcName + " = React.memo(" + fcName + ");";
			
			parseReactResultContent += tempText + "\n";
			
			if (parseResultMap.containsKey("domRender")) {
				
				if ("".equals(formCreateContent)) {
					
					if (!"".equals(packageClassName)) {
						
						tempText = packageClassName + "(Memo" + fcName + ")";
					} else {
						
						tempText = "Memo" + fcName;
					}
					
					parseReactResultContent += String.valueOf(parseResultMap.get("domRender").get("renderContent")).replace(fcName, tempText);
				} else {
					
					if ("".equals(formOption)) {
						
						parseReactResultContent += "const FormMemo" + fcName + " = Form.create()(Memo" + fcName + ");";
					} else {
						
						parseReactResultContent += "const FormMemo" + fcName + " = Form.create(" + formOption + ")(Memo" + fcName + ");";
					}
					
					if (!"".equals(packageClassName)) {
						
						tempText = packageClassName + "(FormMemo" + fcName + ")";
					} else {
						
						tempText = "FormMemo" + fcName;
					}
					
					parseReactResultContent += String.valueOf(parseResultMap.get("domRender").get("renderContent")).replace(fcName, tempText);
				}
				
			} else {
				
				if ("".equals(formCreateContent)) {
					
					if (!"".equals(packageClassName)) {
						
						parseReactResultContent += "\n" + "export default " + packageClassName + "(Memo" + fcName + ");\n";
					} else {
						
						parseReactResultContent += "\n" + "export default Memo" + fcName + ";\n";
					}
					
				} else {
					
					if ("".equals(formOption)) {
						
						parseReactResultContent += "const FormMemo" + fcName + " = Form.create()(Memo" + fcName + ");";
					} else {
						
						parseReactResultContent += "const FormMemo" + fcName + " = Form.create(" + formOption + ")(Memo" + fcName + ");";
					}
					
					if (!"".equals(packageClassName)) {
						
						parseReactResultContent += "\n" + "export default " + packageClassName + "(FormMemo" + fcName + ");\n";
					} else {
						
						parseReactResultContent += "\n" + "export default FormMemo" + fcName + ";\n";
					}
				}
			}
		} else if (parseResultMap.containsKey("domRender")) {
			
			if ("".equals(formCreateContent)) {
				
				parseReactResultContent += parseResultMap.get("domRender").get("renderContent");
			} else {
				
				if ("".equals(formOption)) {
					
					parseReactResultContent += "const Form" + fcName + " = Form.create()(" + fcName + ");";
				} else {
					
					parseReactResultContent += "const Form" + fcName + " = Form.create(" + formOption + ")(" + fcName + ");";
				}
				
				if (!"".equals(packageClassName)) {
					
					tempText = packageClassName + "(Form" + fcName + ")";
				} else {
					
					tempText = "Form" + fcName;
				}
				
				parseReactResultContent += String.valueOf(parseResultMap.get("domRender").get("renderContent")).replace(fcName, tempText);
			}
			
		} else {
			
			if ("".equals(formCreateContent)) {
				
				if (!"".equals(packageClassName)) {
					
					parseReactResultContent += "\n" + "export default " + packageClassName + "(" + fcName + ");\n";
				} else {
					
					parseReactResultContent += "\n" + "export default " + fcName + ";\n";
				}
			} else {
				
				if ("".equals(formOption)) {
					
					parseReactResultContent += "const Form" + fcName + " = Form.create()(" + fcName + ");";
				} else {
					
					parseReactResultContent += "const Form" + fcName + " = Form.create(" + formOption + ")(" + fcName + ");";
				}
				
				if (!"".equals(packageClassName)) {
					
					parseReactResultContent += "\n" + "export default " + packageClassName + "(Form" + fcName + ");\n";
				} else {
					
					parseReactResultContent += "\n" + "export default Form" + fcName + ";\n";
				}
				
			}
			
		}
		
		return parseReactResultContent;
	}
	
	
}
