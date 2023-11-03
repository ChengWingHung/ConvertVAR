package converttype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.ConvertParam;
import utils.ConvertLogUtil;
import utils.FileOperationUtil;
import utils.TxtContentUtil;
import utils.VueProcessUtil;

/**
 * 
 * @author 郑荣鸿（ChengWingHung）
 * @date 20231010
 * @description vue2升级vue3处理类
 * @version 1.0.0
 *
 */

public class Vue2ToVue3Process {

	public Vue2ToVue3Process() {
		
	}
	
	private static String parseFileName;// 当前要解析的文件名
	
	private static Map<String, Map> parseResultMap;// 解析后的信息存储对象
	
	private static Map<String, Map> methodResultMap;// method info map
	
	private static Map<String, Map<String, String>> optionApiPropMap;// optionsApi map
	
	private static Map<String, Map<String, String>> routerPropMap;// router map
	
	private static Map<String, Map<String, String>> propsResultMap;// props map
	
	private static Map<String, Map<String, String>> clearInfoMap;// vue3 中移除的 vue2 信息
	
	private static ArrayList<String> dataResultList;// data 信息存储对象
	
	private static String reactiveValue = "";// reactive 信息
	
	private static String selfDefinePropsValue = "";// 自身定义的 props 信息
	
	private static String routerDefineValue = "";// setup 中路由定义信息
	
	private static String emitTypeDefineValue = "";// emit 类型信息
	
	private static int count = 0;// 计数器
	
	public static String parseVue2FileContent(String fileName, String parseResultContent) {
		
		reactiveValue = "";
		
		parseFileName = fileName;
		
		parseResultMap = new HashMap<>();
		
		clearInfoMap = new HashMap<>();
		
		System.out.print("解析前：\n" + parseResultContent);
		
		parseResultContent = changeGlobalApi(parseResultContent);
		
		parseResultContent = changeGlobalApiTreeshaking(parseResultContent);
		
		parseResultContent = changeOptionApiToCompositionApi(parseResultContent);
		
		parseResultContent = VueProcessUtil.changeComponentPropertys(parseResultContent, parseResultMap);
		
		// 路由以及状态管理器
		parseResultContent = changeVueRoute(parseResultContent);
		
		// TypeScript 版本
		parseResultContent = changeTypeScriptVersion(parseResultContent);
		
		// 处理最终合并的结果
		parseResultContent = getVue3FileResultContent(parseResultContent);
		
		// 处理解析后的内容格式
		parseResultContent = TxtContentUtil.processFileContentFormat(parseResultContent, FileOperationUtil.getFileType(parseFileName), "vue");
		
		System.out.println("解析后：\n" + parseResultContent);
		
		return parseResultContent;
	}
	
	/**
	 * 全局api处理
	 * 
	 * @param fileContent 要处理的内容
	 * @return 处理后的内容
	 */
	private static String changeGlobalApi(String fileContent) {
		
		String temp = "";
		String vue2GlobalApi = "";// vue2对应的全局api
		String vue3GlobalApi = "";// vue3对应的全局api
		String vue2ClearName = "";// 要清除的标志
		String vue2ClearContent = "";// 要清除的部分信息
		
		int endIndex = -1;// 获取截取结束位置
		
		Boolean findGlobalApi = false;
		
		for(String globalApiValue:ConvertParam.Vue2ToVue3GlobalApiList)
        {
			// 无箭头的直接移除
			if (globalApiValue.indexOf(ConvertParam.CONVERT_STRING) > -1) {
				
				vue2GlobalApi = globalApiValue.substring(0, globalApiValue.indexOf(ConvertParam.CONVERT_STRING));
				
				vue3GlobalApi = globalApiValue.substring(globalApiValue.indexOf(ConvertParam.CONVERT_STRING) + 2, globalApiValue.length());
			} else {
				
				vue2GlobalApi = globalApiValue;
				
				vue3GlobalApi = "";
			}
			
			// 判断文件内容中是否有对应api
			if (fileContent.indexOf(vue2GlobalApi) > -1) {
				
				// 清除无需转换的内容
				if ("".equals(vue3GlobalApi)) {
					
					temp = fileContent.substring(fileContent.indexOf(vue2GlobalApi), fileContent.length());
					
					// 非函数
					if (temp.substring(0, temp.indexOf('\n')).indexOf('(') < 0) {
						
						endIndex = temp.indexOf('\n') + 1;
						
						vue2ClearContent = temp.substring(0, endIndex);
						
						fileContent = fileContent.replace(vue2ClearContent, "");
						
						vue2ClearName = vue2ClearContent.indexOf('=') > -1?vue2ClearContent.substring(0, vue2ClearContent.indexOf('=')).trim():vue2ClearContent;
						
						Map<String, String> clearDataMap = new HashMap<>();
						
						clearDataMap.put(vue2ClearName, vue2ClearContent);
						
						// 存入删除对象信息中以备用
						clearInfoMap.put(vue2GlobalApi, clearDataMap);
						
						continue;
					} else {
						
						endIndex = TxtContentUtil.getTagEndIndex(temp, '(', ')');
						
						endIndex = TxtContentUtil.getStatementEndIndex(temp, endIndex);
					}
					
					vue2ClearContent = temp.substring(0, endIndex);
					
					temp = fileContent.substring(0, fileContent.indexOf(vue2GlobalApi));
					
					vue2ClearName = "";
					
					// 前面的为= 号  说明需要清除
					if (temp.trim().lastIndexOf('=') == temp.trim().length() - 1) {
						
						for (int i=temp.length()-1;i>-1;i--) {
							
							if (temp.charAt(i) == '\r' || temp.charAt(i) == '\n' || temp.charAt(i) == ';') {
								endIndex = i;
								break;
							}
							
							// 正好是末尾
							if (temp.length() - 1 == i) {
								endIndex = i;
							}
						}
						
						temp = temp.substring(endIndex, temp.length());
						
						vue2ClearContent = temp + vue2ClearContent;
						
						temp = temp.trim();
						
						vue2ClearName = temp.substring(temp.indexOf(' ') + 1, temp.indexOf('='));
					}
					
					fileContent = fileContent.replace(vue2ClearContent, "");
					
					Map<String, String> clearDataMap = new HashMap<>();
					
					clearDataMap.put(vue2ClearName.trim(), vue2ClearContent.substring(vue2ClearContent.indexOf('(') + 1, vue2ClearContent.lastIndexOf(')')));
					
					// 存入删除对象信息中以备用
					clearInfoMap.put(vue2GlobalApi, clearDataMap);
					
				} else {
					
					findGlobalApi = true;
					
					fileContent = fileContent.replaceAll(vue2GlobalApi, vue3GlobalApi);
				}
			}
        }
		
		if (findGlobalApi) {
			
			importCreateApp();
		}
		
		return fileContent;
	}
	
	private static void importCreateApp() {
		
		addVue3ImportContent("vue", "createApp");// 引入createApp
		
		addVue3DefineContent("app", "const app = createApp({});");
	}
	
	/**
	 * 全局api Treeshaking处理
	 * 
	 * @param fileContent 要处理的内容
	 * @return 处理后的内容
	 */
	private static String changeGlobalApiTreeshaking(String fileContent) {
		
		// 1. Vue.nextTick / $nextTick
		// 2. Vue.observable (用 Vue.reactive 替换)
		// 3. Vue.version
		// 4. Vue.compile (仅完整构建版本)
		// 5. Vue.set (仅兼容构建版本)
		// 6. Vue.delete (仅兼容构建版本)
		
		String temp = "";
		
		int endIndex = -1;// 获取截取结束位置
		
		if (fileContent.indexOf("Vue.nextTick") > -1) {
			
			addVue3ImportContent("vue", "nextTick");// 引入nextTick
			
			fileContent = fileContent.replaceAll("Vue.nextTick", "nextTick");
		}
		
		if (fileContent.indexOf("$nextTick") > -1) {
			
			addVue3ImportContent("vue", "nextTick");// 引入nextTick
			
			endIndex = 0;
			
			for (int i = fileContent.indexOf("$nextTick");i > -1;i--) {
				
				if (' ' == fileContent.charAt(i)) {
					endIndex = i;
					break;
				}
			}
			
			temp = fileContent.substring(endIndex + 1, fileContent.indexOf("$nextTick") + "$nextTick".length());
			
			fileContent = fileContent.replace(temp, "nextTick");
		}
		
		return fileContent;
	}
	
	/**
	 * options API -> composition API
	 * 
	 * @param fileContent 要处理的内容
	 * @return 处理后的内容
	 */
	private static String changeOptionApiToCompositionApi(String fileContent) {
		
		String tempText = "";// 临时处理字段
		String vue2ResultTxt = "";// vue2的语句截取结果
		String changeResultTxt = "";// 转换后的结果
		
		String vueComponentName = "";// Vue.component 注册的组件名
		String vueRenderContent = "";// Vue.component render 部分内容
		String vueComponentContent = "";// Vue.component 部分内容
		
		Boolean isCreateApp = false;// 是否new vue 形式的
		
		int startInex = -1;// 获取截取初始位置
		int endIndex = -1;// 获取截取结束位置
		
		// Vue.component( 形式，单独处理，前面已替换为 app.component
		if (fileContent.indexOf("app.component(") > -1) {
			
			startInex = fileContent.indexOf("app.component(");
			
			tempText = fileContent.substring(startInex, fileContent.length());
			
			endIndex = TxtContentUtil.getTagEndIndex(tempText, '(', ')');
			
			// 找到实际结束位置
			endIndex = TxtContentUtil.getStatementEndIndex(tempText, endIndex);
			
			// 得到Vue.component( 的整段代码
			vueComponentContent = fileContent.substring(startInex, startInex + endIndex);
					
			tempText = vueComponentContent;
			
			vueComponentName = tempText.substring(tempText.indexOf('(') + 1, tempText.indexOf(',')).trim();
			
			vueComponentName = vueComponentName.substring(1, vueComponentName.length() - 1);
			
			tempText = vueComponentContent.substring(vueComponentContent.indexOf(',') + 1, vueComponentContent.length()).trim();
			
			// 注册的组件信息来自Vue.extend
			if (!"".equals(vueComponentName) && tempText.indexOf('{') != 0 && clearInfoMap.containsKey("Vue.extend")) {
				
				tempText = tempText.substring(0, tempText.indexOf(')')).trim();
				
				Map<String, String> clearDataMap = clearInfoMap.get("Vue.extend");
				
				for (Map.Entry<String, String> entry : clearDataMap.entrySet()) {
					
					if (tempText.equals(entry.getKey())) {
						
						changeResultTxt = vueComponentContent;
						
						changeResultTxt = changeResultTxt.substring(0, changeResultTxt.lastIndexOf(tempText)) + entry.getValue() + changeResultTxt.substring(changeResultTxt.lastIndexOf(tempText) + tempText.length(), changeResultTxt.length());
						
						fileContent = fileContent.replace(vueComponentContent, changeResultTxt);
						
						break;
					}
				}
			}
			
			if (fileContent.indexOf("export ") < 0) {
				
				fileContent = getVue3FileResultContent(fileContent);
				
				return fileContent;
			}
			
		}
		
		// new Vue() 形式
		if (fileContent.indexOf(" Vue(") > -1) {
			
			isCreateApp = true;
			
			importCreateApp();
			
			tempText = fileContent.substring(0, fileContent.indexOf(" Vue("));
			
			// 找到该关键字前结束位置
			for (int i = tempText.length() - 1;i >= 0;i--) {
				if (tempText.charAt(i) == ';' || tempText.charAt(i) == '\n' || tempText.charAt(i) == '\r') {
					startInex = i;
					break;
				}
			}
			
			tempText = fileContent.substring(startInex, fileContent.length());
			
			endIndex = TxtContentUtil.getTagEndIndex(tempText, '(', ')');
			
			// 找到实际结束位置
			endIndex = TxtContentUtil.getStatementEndIndex(tempText, endIndex);
			
			// 得到Vue实例化的整段代码
			vue2ResultTxt = fileContent.substring(startInex, startInex + endIndex);
			
		}
		
		dataResultList = new ArrayList<String>();
		
		// export default{} 形式
		if (fileContent.indexOf("export default ") > -1) {
			
			startInex = fileContent.indexOf("export default ");
			
			tempText = fileContent.substring(fileContent.indexOf("export default ") + "export default ".length(), fileContent.length()).trim();
			// 只是export 数据信息 
			if ('{' != tempText.charAt(0)) {
				
				return fileContent;
			}
			
			tempText = fileContent.substring(startInex, fileContent.length());
			
			endIndex = TxtContentUtil.getTagEndIndex(tempText, '{', '}');
			
			// 找到实际结束位置
			endIndex = TxtContentUtil.getStatementEndIndex(tempText, endIndex);
			
			// 得到export default 的整段代码
			vue2ResultTxt = fileContent.substring(startInex, startInex + endIndex);
			
			// 获取render 返回信息
			vueRenderContent = VueProcessUtil.getRenderContentFunction(vue2ResultTxt);
			
			if (!"".equals(vueRenderContent)) {

				addVue3ImportContent("vue", "h");// 导入h
				
				addVue3ImportContent("vue", "resolveComponent");// 导入resolveComponent
				
				dataResultList.add(TxtContentUtil.reNameVariable(vueComponentName));
				
				tempText = VueProcessUtil.getRenderAllContentFunction(vue2ResultTxt);
				
				vueComponentContent = tempText;
			}
		}
		
		// 1. 没得到options api ，则无需处理api的解析
		// 2. export default xxx
		if ("".equals(vue2ResultTxt) || vue2ResultTxt.indexOf('{') < 0) {
			
			return fileContent;
		}
		
		tempText = vue2ResultTxt;// 用于截取
		changeResultTxt = vue2ResultTxt;// 用于替换
		
		// 得到花括号里边的内容
		String optionsConfigText = tempText.substring(tempText.indexOf('{'), tempText.lastIndexOf('}') + 1);
		String optionsConfigTextBak = optionsConfigText;
		
		String propResultText = "";
		String dataResultText = "";
		
		propsResultMap = new HashMap<>();
		methodResultMap = new HashMap<>();
		optionApiPropMap = new HashMap<>();
		
		VueProcessUtil.getPropertyDetailOfObject(optionsConfigText.substring(1, optionsConfigText.length() - 1), optionApiPropMap, 1);
		
		Map<String, String> apiDataMap = new HashMap<>();
		
		String apiTempText = "";
		
		// 获取props整个信息
		if (optionApiPropMap.containsKey("props")) {
			
			apiDataMap = optionApiPropMap.get("props");
			
			apiTempText = apiDataMap.get("apiName") + apiDataMap.get("apiNameEndChar");
			
			propResultText = "";
			
			startInex = Integer.parseInt(apiDataMap.get("apiNameIndex"));
			
			// props:[""]、props:{xx:xxx;}、props:{xx:{xx:xxx}} 形式
			tempText = optionsConfigText.substring(startInex + apiTempText.length(), optionsConfigText.length()).trim();
			
			char propsStartWith = tempText.charAt(0);
			
			tempText = TxtContentUtil.getContentByTag(optionsConfigText, startInex, propsStartWith, tempText.charAt(0) == '['?']':'}');// 得到一整个props包裹信息
			
			propResultText = tempText;// 存储用于后续整个替换内容
			
			tempText = tempText.substring(tempText.indexOf(propsStartWith) + 1, tempText.lastIndexOf(propsStartWith == '['?']':'}'));
			
			// 存储props 信息用于生成文件内容
			if (propsStartWith == '[') {
				
				for (int i=0;i < tempText.split(",").length;i++) {
					
					if (!"".equals(tempText.split(",")[i].trim())) {
						
						Map<String, String> propMap = new HashMap<>();
						
						propMap.put("", "");// value : description / 值 : 备注信息
						
						propsResultMap.put(tempText.split(",")[i], propMap);
					}
				}
				
			} else {
				
				selfDefinePropsValue = propResultText;// 用于后续组装拼接，以免处理data 信息部分时，props 含有data导致解析错误
				
				// 自身定义的props 部分保留
				if (!"".equals(tempText.trim())) VueProcessUtil.processVuePropsInfo(tempText.trim(), propsResultMap);
			}
			
		}
		
		// 获取data中属性信息，从第一外层获取
		if (optionApiPropMap.containsKey("data")) {
			
			apiDataMap = optionApiPropMap.get("data");
			
			apiTempText = apiDataMap.get("apiName") + apiDataMap.get("apiNameEndChar");
			
			dataResultText = "";
			
			startInex = Integer.parseInt(apiDataMap.get("apiNameIndex"));
			
			tempText = TxtContentUtil.getContentByTag(optionsConfigText, startInex, '{', '}');// 得到一整个data包裹信息
			
			dataResultText = tempText;// 存储用于后续整个替换内容
			
			// data(){return {}} 的情况
			if (tempText.indexOf("return ") > -1) {
				
				startInex = tempText.indexOf("return ") + "return ".length();
			} else {
				
				startInex = tempText.indexOf('{');
			}
			
			tempText = TxtContentUtil.getContentByTag(tempText, startInex, '{', '}'); // 得到整个data信息
			
			tempText = tempText.substring(1, tempText.length() - 1).trim();
			
			if (!"".equals(tempText)) {
				
				addVue3ImportContent("vue", "reactive");
				
				reactiveValue = "const state = reactive({\n";// reactive 信息
				
				reactiveValue += tempText + "\n";
				
				reactiveValue += "});\n";
			}
			
			// 获得data信息
			VueProcessUtil.processVueDataInfo(tempText, dataResultList);
			
		}
		
		if (!"".equals(propResultText)) {
			
			// 传入的props 部分清除
			optionsConfigText = TxtContentUtil.deleteFirstComma(optionsConfigText, optionsConfigText.indexOf(propResultText) + propResultText.length());// 删除末尾的逗号
			optionsConfigText = optionsConfigText.replace(propResultText, "");// 替换掉整个props的内容
		}
		
		if (!"".equals(dataResultText)) {
			
			// 传入的data 部分清除
			optionsConfigText = TxtContentUtil.deleteFirstComma(optionsConfigText, optionsConfigText.indexOf(dataResultText) + dataResultText.length());// 删除末尾的逗号
			optionsConfigText = optionsConfigText.replace(dataResultText, "");// 替换掉整个data的内容
		}
		
		// 判断filters、computed、watch、method
		for (int i=0;i<ConvertParam.Vue2ToVue3SetUpMethodList.length;i++) {
			
			tempText = getMethodContent(ConvertParam.Vue2ToVue3SetUpMethodList[i], optionsConfigText);
			
			// 不相等说明找到了，则处理替换
			if (!tempText.equals(optionsConfigText)) {
				optionsConfigText = TxtContentUtil.deleteFirstComma(optionsConfigText, optionsConfigText.indexOf(tempText) + tempText.length());// 删除末尾的逗号
				optionsConfigText = optionsConfigText.replace(tempText, "");
			}
			
		}
		
		// 处理生命周期的转换
		optionsConfigText = changeComponentLifeycle(optionsConfigText);
		
		// 处理props 信息
		String selfPropDefineInfo = "";// 自身组件定义的props 定义信息
		String getSelfPropsInfo = "";// 自身组件定义的props 信息
		String getParentPropsInfo = "";// 来自父组件传入的props 信息
		
		for (Map.Entry<String, Map<String, String>> entry : propsResultMap.entrySet()) {
			
			Map<String, String> propsMap = entry.getValue();
			
			for (Map.Entry<String, String> propMap : propsMap.entrySet()) {
				
				// 说明是来自传入的props
				if ("".equals(propMap.getKey())) {
					
					getParentPropsInfo += entry.getKey() + ",\n";
				} else {
					
					getSelfPropsInfo += entry.getKey() + ",\n";
					
					selfPropDefineInfo += "const " + entry.getKey() + " = reactive(props." + entry.getKey() + ");\n";
				}
			}
		}
		
		String setUpContentText = "setup() {\n";
		
		if (!"".equals(selfPropDefineInfo)) {
			
			addVue3ImportContent("vue", "reactive");// 引入reactive
			
			setUpContentText = "setup(props) {\n" + selfPropDefineInfo;
		}
		
		if (!"".equals(vueRenderContent)) {
			
			setUpContentText += "const " + TxtContentUtil.reNameVariable(vueComponentName) + " = resolveComponent(\"" + vueComponentName + "\");\n";
		}
		
		// 拼接上自定义的props 部分信息
		if (!"".equals(selfDefinePropsValue)) {
			
			setUpContentText = selfDefinePropsValue + ",\n" + setUpContentText;
		}
		
		tempText = assembleVue3SetUpApi(getSelfPropsInfo, getParentPropsInfo);// 从得到的methodResultMap等拼接setup信息
		
		// emit 事件信息不为空
		if (!"".equals(emitTypeDefineValue)) {
			
			if (setUpContentText.indexOf("setup(props)") > -1) {
				
				setUpContentText = setUpContentText.replace("setup(props)", "setup(props, context)");
			} else if (setUpContentText.indexOf("setup()") > -1) {
				
				setUpContentText = setUpContentText.replace("setup()", "setup(context)");
			}
			
			setUpContentText = "emits: [\n" + emitTypeDefineValue.substring(0, emitTypeDefineValue.length() - 1) + "\n],\n" + setUpContentText;
		}
		
		// 路由信息插入最后一个import 后
		if (!"".equals(routerDefineValue)) {
			
			setUpContentText += routerDefineValue;
		}
		
		if (!"".equals(tempText)) {
			
			setUpContentText += tempText;
			
			setUpContentText += "}\n";
			
			optionsConfigText = optionsConfigText.substring(0, optionsConfigText.lastIndexOf('}')) + setUpContentText + "}";// 将setup信息拼接到末尾
		}
		
		optionsConfigTextBak = optionsConfigTextBak.replace(optionsConfigTextBak, optionsConfigText);
		
		// new vue => createapp
		if (isCreateApp) {
			
			String elementIdValue = "";
			String variableVueName = "";
			
			if (optionApiPropMap.containsKey("el")) {
				
				Map<String, String> elementMap = optionApiPropMap.get("el");
				
				elementIdValue = elementMap.get("apiNameValue");
				
			}
			
			tempText = changeResultTxt.substring(0, changeResultTxt.indexOf(" Vue(") + " Vue(".length() - 1);
			
			// 判断是否有 = 
			if (tempText.indexOf("=") > -1) {
				
				startInex = tempText.indexOf("=") + 1;
				
				variableVueName = tempText.substring(0, tempText.indexOf("=")).trim();
				
				if (variableVueName.indexOf(' ') > -1) variableVueName = variableVueName.substring(variableVueName.lastIndexOf(' ') + 1, variableVueName.length());
				
				// 存储起来，后续使用
				if (!"".equals(variableVueName)) {
					
					Map<String, String> variableNameMap = new HashMap<>();
					
					variableNameMap.put("newVue", variableVueName);
					
					parseResultMap.put("originDefine", variableNameMap);// 原来定义的内容
				}
				
				// 替换new vue => c
				changeResultTxt = changeResultTxt.substring(0, startInex) + " createApp" + changeResultTxt.substring(changeResultTxt.indexOf(" Vue(") + " Vue(".length() - 1, changeResultTxt.length());
				
			} else {
				
				// 替换new vue => c
				changeResultTxt = " createApp" + changeResultTxt.substring(changeResultTxt.indexOf(" Vue(") + " Vue(".length() - 1, changeResultTxt.length());
			}
			
			// 判断
			if (!"".equals(elementIdValue)) {
				
				if (!"".equals(variableVueName) && fileContent.indexOf(variableVueName + ".$mount(") < 0) {
					
					changeResultTxt += "\n" + variableVueName + ".mount('" + elementIdValue + "');";
				} else if (fileContent.substring(fileContent.indexOf(vue2ResultTxt) + vue2ResultTxt.length(), fileContent.length()).indexOf(".mount(") != 0) {
					
					changeResultTxt = "\n" + changeResultTxt + ".mount('" + elementIdValue + "');";
				}
			}
			
		} else {
			
			changeResultTxt = "export default " + optionsConfigTextBak;
		}
		
		if (!"".equals(vueComponentContent)) {
			
			changeResultTxt = TxtContentUtil.deleteFirstComma(changeResultTxt, changeResultTxt.indexOf(vueComponentContent) + vueComponentContent.length());// 删除末尾的逗号
			changeResultTxt = changeResultTxt.replace(vueComponentContent, "");
		}
		
		fileContent = fileContent.replace(vue2ResultTxt, changeResultTxt);
		
		return fileContent;
	}
	
	private static String getMethodContent(String methodType, String optionsConfigText) {
			
		if (optionsConfigText.indexOf(methodType + ":") < 0) return optionsConfigText;// 没找到的情况下直接停止
		
		int startInex = -1;// 获取截取初始位置
		
		String tempText = "";// 临时处理字段
		
		startInex = optionsConfigText.indexOf(methodType + ":");
		
		tempText = TxtContentUtil.getContentByTag(optionsConfigText, startInex, '{', '}');// 得到method整个方法
		
		if ("computed".equals(methodType) || "watch".equals(methodType)) {
			
			count = 0;
		}
		
		// 得到里边所有的方法并封装成一个map对象返回
		getMethodResultMap("methods".equals(methodType)?"":methodType, tempText.substring(tempText.indexOf('{') + 1, tempText.lastIndexOf('}')).trim());
		
		return tempText;// 得到整个方法内容用于替换
	}
	
	// 获取option api中转换到setup的所有方法
	private static void getMethodResultMap(String methodType, String methodContent){
		
		// 为空的时候无需解析
		if ("".equals(methodContent)) return;
		
		String tempText = "";
		String methodName = "";
		String methodParams = "";
		String methodBody = "";
		String methodContentText = "";
		String methodDescription = "";
		
		int endIndex = -1;// 获取截取结束位置
		
		// 判断是否有注释
		methodDescription = TxtContentUtil.getCommentInformation(methodContent);
		
		if (!"".equals(methodDescription)) methodContent = methodContent.substring(methodContent.indexOf(methodDescription) + methodDescription.length(), methodContent.length());
		
		// 防止参数里边含有{}导致解析异常
		if (methodContent.indexOf('(') < methodContent.indexOf('{')) {
			
			endIndex = TxtContentUtil.getTagEndIndex(methodContent, '(', ')') + 1;
		} else {
			
			endIndex = -1;
		}
		
		// 先得到第一个方法的所有信息
		if (endIndex == -1) {
			
			endIndex = TxtContentUtil.getTagEndIndex(methodContent, '{', '}') + 1;
		} else {
			
			tempText = methodContent.substring(endIndex, methodContent.length());
			
			endIndex += TxtContentUtil.getTagEndIndex(tempText, '{', '}') + 1;
		}
		
		methodContentText = methodContent.substring(0, endIndex);
		
		methodContentText = methodContentText.trim();
		
		/**
		value: {
	      handler(newV, oldV) {
	        this.$set(this, 'val', newV)
	      },
	      immediate: true
	    }
		
		*/
		
		if (methodContentText.indexOf(':') > -1 && methodContentText.indexOf(':') < methodContentText.indexOf('{')) {
			
			methodName = methodContentText.substring(0, methodContentText.indexOf(':'));
			
			// 此处computed 和 watch 的处理不同，分开判断
			if ("computed".equals(methodType)) {
				
				methodContentText = methodContentText.substring(methodContentText.indexOf('{'), methodContentText.length());
				
				methodBody = methodContentText;
				
			} else {
				
				methodContentText = methodContentText.substring(methodContentText.indexOf('{') + 1, methodContentText.length());
				
				if (methodContentText.indexOf('(') > -1) {
					
					tempText = methodContentText.substring(methodContentText.indexOf('(') + 1, methodContentText.length());
					
					methodParams = tempText.substring(0, tempText.indexOf(')'));
				}
				
				methodBody = TxtContentUtil.getContentByTag(tempText, tempText.indexOf('{'), '{', '}');
			}
			
		} else {
			
			// 获取方法名和参数信息
			if (methodContentText.indexOf('(') > -1) {
				methodName = methodContentText.substring(0, methodContentText.indexOf('('));
				
				tempText = methodContentText.substring(methodContentText.indexOf('(') + 1, methodContentText.length());
				methodParams = tempText.substring(0, tempText.indexOf(')'));
			}
			
			// 获取方法体信息
			tempText = methodContentText.substring(methodContentText.indexOf('{'), methodContentText.length());
			
			methodBody = TxtContentUtil.getContentByTag(tempText, 0, '{', '}');
		}
		
		methodContent = methodContent.substring(methodContent.indexOf(methodContentText) + methodContentText.length(), methodContent.length());
		
		// 组装方法信息到map对象
		Map<String, String> methodMap = new HashMap<>();
		
		methodMap.put("methodDescription", methodDescription);
		methodMap.put("methodName", methodName);
		methodMap.put("methodParams", methodParams);
		methodMap.put("methodBody", methodBody);
		
		tempText = methodType;
		
		if ("computed".equals(methodType) || "watch".equals(methodType)) {
			tempText = methodType + "_" + count;
			count++;
		}
		
		methodResultMap.put("".equals(tempText)?methodName:tempText, methodMap);
		
		// 判断是否需要递归解析
		if (methodContent.indexOf('{') > -1) {
			
			methodContent = methodContent.trim();
			
			// 第一个如果是逗号
			if (methodContent.indexOf(',') == 0) methodContent = methodContent.substring(1, methodContent.length()).trim();
			
			getMethodResultMap(methodType, methodContent);
		}
		
	}
	
	private static String changeComponentLifeycle(String fileContent) {
		
		// 1 beforeCreate -> setup()
		// 2 created -> setup()
		// 3 beforeMount -> onBeforeMount
		// 4 mounted -> onMounted
		// 5 beforeUpdate -> onBeforeUpdate
		// 6 updated -> onUpdated
		// 7 beforeDestroy -> onBeforeUnmount
		// 8 destroyed -> onUnmounted
		// 9 errorCaptured -> onErrorCaptured
		
		String tempTxt = "";
		String vue2LiftcycleName = "";
		String vue3LiftcycleName = "";
		String methodBody = "";
		String vue2MethodContent = "";
		
		int startInex = -1;// 获取截取初始位置
		
		// 转换为vue3对应生命周期函数
		for (int i=0;i<ConvertParam.Vue2ToVue3LiftcycleList.length;i++) {
			
			tempTxt = ConvertParam.Vue2ToVue3LiftcycleList[i];
			
			// 判断有无转换标志
			if (tempTxt.indexOf(ConvertParam.CONVERT_STRING) > -1) {
				vue2LiftcycleName = tempTxt.substring(0, tempTxt.indexOf(ConvertParam.CONVERT_STRING));
			} else {
				vue2LiftcycleName = tempTxt;
			}
				
			// 判断是否有对应生命周期函数
			if (fileContent.indexOf(vue2LiftcycleName + "()") > -1) {
				
				startInex = fileContent.indexOf(vue2LiftcycleName + "()");
				
				vue2MethodContent = TxtContentUtil.getContentByTag(fileContent, startInex, '{', '}');
				
				// 判断有无转换标志
				if (tempTxt.indexOf(ConvertParam.CONVERT_STRING) > -1) {
					
					vue3LiftcycleName = tempTxt.substring(tempTxt.indexOf(ConvertParam.CONVERT_STRING) + 2, tempTxt.length());
					
					startInex += (vue2LiftcycleName + "()").length();
					
					methodBody = TxtContentUtil.getContentByTag(fileContent, startInex, '{', '}');// 得到整个方法体
					
					// 组装方法信息到map对象
					Map<String, String> methodMap = new HashMap<>();
					
					methodMap.put("liftcycleFunction", "true");// 生命周期函数区分标志
					methodMap.put("methodDescription", "");
					methodMap.put("methodName", vue3LiftcycleName);
					methodMap.put("methodParams", "");
					methodMap.put("methodBody", methodBody);
					
					methodResultMap.put(vue3LiftcycleName, methodMap);
				}
				
				fileContent = TxtContentUtil.deleteFirstComma(fileContent, fileContent.indexOf(vue2MethodContent) + vue2MethodContent.length());// 删除末尾的逗号
				fileContent = fileContent.replace(vue2MethodContent, "");
			}
			
		}
		
		return fileContent;
	}
	
	/**
	 * 组装setup 信息 
	 * 
	 * @param getSelfPropsInfo
	 * @param getParentPropsInfo
	 * @return 
	 */
	private static String assembleVue3SetUpApi(String getSelfPropsInfo, String getParentPropsInfo) {
		
		routerDefineValue = "";
		emitTypeDefineValue = "";
		
		String vue3SetUpResultContent = "";
		String methodDescription = "";
		String methodBodyContent = "";
		
		Map<String, String> methodMap;
		
		// 处理reactive 部分
		vue3SetUpResultContent += reactiveValue;
		
		// 处理函数部分
		for (Map.Entry<String, Map> entry : methodResultMap.entrySet()) {
			
			methodMap = entry.getValue();
			methodBodyContent = methodMap.get("methodBody");
			
			// 处理setup方法中的this.
			methodBodyContent = replaceThisOfSetUp(methodBodyContent);
			
			// 处理setup方法中的路由读取
			methodBodyContent = replaceRouterInfoOfSetUp(methodBodyContent);
			
			// this.$emit => context.emit in setup
			methodBodyContent = replaceThisEmitWithContext(methodBodyContent);
			
			// this.$set() 和 this.$delete 为vue2 中解决无法监听复杂数据类型属性新增删除
			methodBodyContent = VueProcessUtil.removeVue2BindObjectInfoChangeProcess(methodBodyContent, "set");
			methodBodyContent = VueProcessUtil.removeVue2BindObjectInfoChangeProcess(methodBodyContent, "delete");
			
			// 判断是否为生命周期函数
			if (methodMap.containsKey("liftcycleFunction")) {
				
				vue3SetUpResultContent += methodMap.get("methodName") + "(() => " + methodBodyContent + ");\n";
				
				// 处理引入信息
				addVue3ImportContent("vue", methodMap.get("methodName"));
			} else {
				
				// watch 函数单独处理
				if (entry.getKey().indexOf("watch_") == 0) {
					vue3SetUpResultContent += "watch(" + methodMap.get("methodName") + "(" + methodMap.get("methodParams") + ") => " + methodBodyContent + ");\n";
					
					addVue3ImportContent("vue", "watch");
				} 
				// computed 函数单独处理
				else if (entry.getKey().indexOf("computed_") == 0) {
					
					dataResultList.add(methodMap.get("methodName"));
					
					vue3SetUpResultContent += "const " + methodMap.get("methodName") + " = computed(() => " + methodBodyContent + ");\n";
					
					addVue3ImportContent("vue", "computed");
				} else {
					
					dataResultList.add(methodMap.get("methodName"));
					
					methodDescription = methodMap.get("methodDescription");
					
					if (!"".equals(methodDescription)) methodDescription += "\n";
					
					vue3SetUpResultContent += methodDescription + "const " + methodMap.get("methodName") + " = (" + methodMap.get("methodParams") + ") => " + methodBodyContent + ";\n";
				}
			}
	    }
		
		// 处理data部分
		vue3SetUpResultContent += "return {\n";
		
		for(String dataValue:dataResultList)
        {
			vue3SetUpResultContent += dataValue + ",\n";
        }
		
		// 拼接传入的props 信息
		if (!"".equals(getParentPropsInfo)) vue3SetUpResultContent += getParentPropsInfo;
		
		// 拼接自身的props 信息
		if (!"".equals(getSelfPropsInfo)) vue3SetUpResultContent += getSelfPropsInfo;
		
		// 说明无需增加setup信息
		if ("return {\n".equals(vue3SetUpResultContent)) {
			
			vue3SetUpResultContent = "";
		} else {
		
			vue3SetUpResultContent += "};\n";
		}
		
		return vue3SetUpResultContent;
	}
	
	/**
	 * 替换setup 里边的 this 
	 * 
	 * @param methodBodyContent 方法体
	 * @return 
	 */
	private static String replaceThisOfSetUp(String methodBodyContent) {
		
		// 处理props 部分的  this.xxx
		for (Map.Entry<String, Map<String, String>> entry : propsResultMap.entrySet()) {
			
			methodBodyContent = preReplaceThisOfVue2Method(methodBodyContent, entry.getKey(), "");
		}
		
		// 处理state 部分的  this.xxx
		for(String dataValue:dataResultList) {
			
			methodBodyContent = preReplaceThisOfVue2Method(methodBodyContent, dataValue, "state.");
		}
		
		Map<String, String> methodMap;
		
		String methodName = "";
		
		// 处理method 部分的 this.xxx
		for (Map.Entry<String, Map> entry : methodResultMap.entrySet()) {
			
			methodMap = entry.getValue();
			
			methodName = methodMap.get("methodName") + "(";
			
			// 非生命周期部分的函数
			if (!methodMap.containsKey("liftcycleFunction")) {
				
				methodBodyContent = preReplaceThisOfVue2Method(methodBodyContent, methodName, methodName + "(");
			}
		}
		
		return methodBodyContent;
	}
	
	private static String preReplaceThisOfVue2Method(String methodBodyContent, String originValue, String repalceValue) {
		
		methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, " this.", originValue, " " + repalceValue);
		
		methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, "\nthis.", originValue, "\n" + repalceValue);
		
		methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, "(this.", originValue, "(" + repalceValue);
		
		methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, "[this.", originValue, "[" + repalceValue);
		
		return methodBodyContent;
	}
	
	/**
	 * 替换setup 里边的路由读取
	 * this.$route => vueRoute 读取到当前路由信息 
	 * this.$router => vueRouter 读取到路由表信息
	 * 
	 * @param methodBodyContent 方法体
	 * @return 
	 */
	private static String replaceRouterInfoOfSetUp(String methodBodyContent) {
		
		// this.$router
		if (methodBodyContent.indexOf("this.$router") > -1) {
			
			addVue3ImportContent("vue-router", "useRouter");// 引入useRouter
			
			routerDefineValue += "\nconst vueRouter = useRouter();\n";
			
			methodBodyContent = methodBodyContent.replace("this.$router", "vueRouter");
		}
		
		// this.$route
		if (methodBodyContent.indexOf("this.$route") > -1) {
			
			addVue3ImportContent("vue-router", "useRoute");// 引入useRoute
			
			routerDefineValue += "\nconst vueRoute = useRoute();\n";
			
			methodBodyContent = methodBodyContent.replace("this.$route", "vueRoute");
		}
		
		return methodBodyContent;
	}
	
	/**
	 * 处理vue2 this.$emit => context.emit 并获取emit 信息用于后续组装
	 * 
	 * @param methodBodyContent
	 * @return
	 */
	public static String replaceThisEmitWithContext(String methodBodyContent) {
		
		String temp = "";
		String emitName = "";
		
		int endIndex = -1;// 获取截取结束位置
		
		if (methodBodyContent.indexOf("this.$emit") > -1) {
			
			temp = methodBodyContent.substring(methodBodyContent.indexOf("this.$emit"), methodBodyContent.length());
			
			endIndex = TxtContentUtil.getTagEndIndex(temp, '(', ')') + 1;
					
			temp = temp.substring(0, endIndex);// 得到整个emit 内容
			
			emitName = temp.substring(temp.indexOf('(') + 1, temp.indexOf(','));// 得到事件名
			
			if (emitTypeDefineValue.indexOf(emitName) < 0) emitTypeDefineValue += emitName + ",\n";
			
			return methodBodyContent.substring(0, methodBodyContent.indexOf("this.$emit") + "this.$emit".length()).replace("this.$emit", "context.emit") + replaceThisEmitWithContext(methodBodyContent.substring(methodBodyContent.indexOf("this.$emit") + "this.$emit".length(), methodBodyContent.length()));
		}
		
		return methodBodyContent;
	}
	
	/**
	 * 添加定义信息部分
	 * 
	 * @param variableKey 变量名
	 * @param variableContent 具体定义内容
	 */
	private static void addVue3DefineContent(String variableKey, String variableContent) {
		
		Map<String, String> defineMap;
		
		if (parseResultMap.containsKey("define")) {
			defineMap = parseResultMap.get("define");
		} else {
			defineMap= new HashMap<>();
		}
		
		if (!defineMap.containsKey(variableKey)) {
			
			defineMap.put(variableKey, variableContent);
			
			parseResultMap.put("define", defineMap);
		}
	}
	
	/**
	 * 添加import 信息部分
	 * 
	 * @param fromKey 从那个库引入，库名
	 * @param importContent 引入的具体内容
	 */
	private static void addVue3ImportContent(String fromKey, String importContent) {
		
		fromKey = fromKey.replaceAll("'", "");
		
		fromKey = fromKey.replaceAll("\"", "");
		
		Boolean hasExist = false;
		
		Map<String, ArrayList> importMap;
		
		if (parseResultMap.containsKey(ConvertParam.IMPORT_FLG)) {
			importMap = parseResultMap.get(ConvertParam.IMPORT_FLG);
		} else {
			importMap= new HashMap<>();
		}
		
		ArrayList<String> importList;
		
		if (importMap.containsKey(fromKey)) {
			importList = importMap.get(fromKey);
			if (!importList.contains(importContent)) {
				importList.add(importContent);
			} else {
				hasExist = true;
			}
		} else {
			importList = new ArrayList<String>();
			importList.add(importContent);
		}
		
		// 存在的情况下无需更新
		if (!hasExist) {
			
			importMap.put(fromKey, importList);
			
			parseResultMap.put(ConvertParam.IMPORT_FLG, importMap);
		}
		
	}
	
	private static String processVue3ImportContent(String fileContent) {
		
		String tempText = "";
		String vue3ImportContent = "";// 单条import 内容
		String vue3ParsePartImportContent = "";// 解析出来部分需要拼接到原内容上的所有import 内容
		
		Map<String, ArrayList> vue3ImportMap;
		
		ArrayList<String> importList;
		
		int startInex = -1;// 获取截取初始位置
		
		if (parseResultMap.containsKey(ConvertParam.IMPORT_FLG)) {
			
			vue3ImportMap = parseResultMap.get(ConvertParam.IMPORT_FLG);
			
			vue3ImportContent = "";
			
			for (Map.Entry<String, ArrayList> entry : vue3ImportMap.entrySet()) {
				
				tempText = "";
				vue3ImportContent = "import { ";
				
				importList = entry.getValue();
				
				for(String importValue:importList)
		        {
					vue3ImportContent += importValue + ", ";
		        }
				
				vue3ImportContent = vue3ImportContent.substring(0, vue3ImportContent.lastIndexOf(','));
				
				vue3ImportContent += " } from '" + entry.getKey() + "'";
				
				// 判断原先内容中是否有from 这个库，有则替换，无则插入
				if (fileContent.indexOf(" from '" + entry.getKey() + "'") > -1) {
					tempText = " from '" + entry.getKey() + "'";
				} else if (fileContent.indexOf(" from \"" + entry.getKey() + "\"") > -1) {
					tempText = " from \"" + entry.getKey() + "\"";
				}
				
				if ("".equals(tempText)) {
					
					vue3ParsePartImportContent += vue3ImportContent + ";\n";// 执行拼接，用于最后的拼接到文件内容中
				} else {
					
					// 直接替换原来内容
					startInex = fileContent.substring(0, fileContent.indexOf(tempText)).lastIndexOf("import ");
					
					fileContent = fileContent.substring(0, startInex) + vue3ImportContent + fileContent.substring(fileContent.indexOf(tempText) + tempText.length(), fileContent.length());
					
				}
				
			}
			
			// 执行import 内容的插入
			if (!"".equals(vue3ParsePartImportContent)) {
				
				tempText = FileOperationUtil.getFileType(parseFileName);
				
				// 判断文件类型再添加
				if ("vue".equals(tempText)) {
					
					startInex = fileContent.indexOf("<script");
					
					if (startInex == -1) {
						
						fileContent = vue3ParsePartImportContent + "\n" + fileContent;
						
					} else {
						
						tempText = fileContent.substring(startInex, fileContent.length());
						
						startInex += tempText.indexOf('>') + 1;
						
						fileContent = fileContent.substring(0, startInex) + "\n" + vue3ParsePartImportContent + "\n" + fileContent.substring(startInex, fileContent.length());
					}
				} else {
					
					fileContent = vue3ParsePartImportContent + "\n" + fileContent;
				}
				
			}
			
		}
		
		return fileContent;
	}
	
	private static String processVue3DefineContent(String fileContent) {
		
		String tempText = "";
		String vue3DefineContent = "";// 单条定义内容
		String vue3ParsePartDefineContent = "";// 解析出来部分需要拼接到原内容上的所有定义内容
		
		Map<String, String> defineMap;
		
		int startInex = -1;// 获取截取初始位置
		
		if (parseResultMap.containsKey("define")) {
			
			defineMap = parseResultMap.get("define");
			
			for (Map.Entry<String, String> entry : defineMap.entrySet()) {
				
				vue3DefineContent = "";
				
				vue3DefineContent = entry.getValue();
				
				// 判断是否存在，存在则无需再定义
				if (fileContent.indexOf(" " + entry.getKey() + " ") < 0) {
					
					vue3ParsePartDefineContent += vue3DefineContent;
				}
			}
			
			// 执行define 内容的插入，插入到最后一个import  前面
			if (!"".equals(vue3ParsePartDefineContent)) {
				
				tempText = fileContent.substring(fileContent.lastIndexOf("import "), fileContent.length());
				
				startInex = fileContent.lastIndexOf("import ") + TxtContentUtil.getStatementEndIndex(tempText, 0);
				
				fileContent = fileContent.substring(0, startInex + 1) + "\n\n" + vue3ParsePartDefineContent + "\n\n" + fileContent.substring(startInex + 1, fileContent.length());
				
			}
		}
		
		return fileContent;
	}
	
	private static String changeVueRoute(String fileContent) {
		
		// 1 router-link -> RouterLink
		// 2 v-bind=“route" -> :to="route"
		// 3 $router.push -> router.push
		// 4 store.subscribe -> store.watch
		// 5 mapState -> useStore/mapState
		
		String temp = "";
		String vue2Route = "";
		String vue3Route = "";
		
		for (int i=0;i<ConvertParam.Vue2ToVue3RouteList.length;i++) {
			
			temp = ConvertParam.Vue2ToVue3RouteList[i];
			vue2Route = temp.substring(0, temp.indexOf(ConvertParam.CONVERT_STRING));
			vue3Route = temp.substring(temp.indexOf(ConvertParam.CONVERT_STRING) + 2, temp.length());
			
			if (fileContent.indexOf(vue2Route) > -1) {
				fileContent = fileContent.replaceAll(vue2Route, vue3Route);
			}
		}
		
		int startInex = -1;// 获取截取初始位置
		int endIndex = -1;// 获取截取结束位置
		
		// 6 new VueRouter => VueRouter.createRouter
		if (fileContent.indexOf(" VueRouter(") > -1) {
			
			routerPropMap = new HashMap<>();
			
			temp = fileContent.substring(0, fileContent.indexOf(" VueRouter(") + " VueRouter(".length() - 1);
			
			startInex = temp.lastIndexOf("new ");
			
			temp = temp.substring(startInex, temp.length());
			
			fileContent = fileContent.replace(temp, "VueRouter.createRouter");
			
			temp = fileContent.substring(startInex, fileContent.length());
			
			endIndex = TxtContentUtil.getTagEndIndex(temp, '(', ')') + 1;
			
			vue2Route = temp.substring(0, endIndex);// 得到整个router对象
			
			temp = vue2Route.substring(vue2Route.indexOf('{') + 1, vue2Route.lastIndexOf('}'));
			
			VueProcessUtil.getPropertyDetailOfObject(temp, routerPropMap, 0);
			
			// 7 { mode: 'hash' } => { history: VueRouter.createWebHashHistory() }
			if (routerPropMap.containsKey("mode")) {
				
				Map<String, String> routerMap = routerPropMap.get("mode");
				
				startInex = Integer.parseInt(routerMap.get("apiNameIndex"));
				
				vue3Route = temp.substring(startInex, temp.length());
				
				endIndex = TxtContentUtil.getStatementEndIndex(vue3Route, 0);
				
				vue3Route = vue3Route.substring(0, endIndex);
				
				if (vue3Route.indexOf("hash") > -1) {
					
					temp = vue2Route;
					
					temp = temp.replace(vue3Route, "history: VueRouter.createWebHashHistory()");
					
					fileContent = fileContent.replace(vue2Route, temp);
				}
				
			}
			
		}
		
		return fileContent;
	}
	
	private static String changeTypeScriptVersion(String fileContent) {
		
		
		
		return fileContent;
	}
	
	private static String getVue3FileResultContent(String fileContent) {
		
		// import 信息处理
		fileContent = processVue3ImportContent(fileContent);
		
		// define 信息处理
		fileContent = processVue3DefineContent(fileContent);
		
		return fileContent;
	}
}
