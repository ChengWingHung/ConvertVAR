package converttype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.ConvertParam;
import utils.FileOperationUtil;
import utils.TxtContentUtil;
import utils.VueProcessUtil;

/**
 * 
 * @author 郑荣鸿（chengwinghung）
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
	
	private static String reactiveValue = "";// reactive信息
	
	private static String selfDefinePropsValue = "";// 自身定义的props 信息
	
	private static ArrayList<String> dataResultList;// data信息存储对象
	
	private static Map<String, Map<String, String>> propsResultMap;// props信息存储对象
	
	private static Map<String, Map> methodResultMap;// method信息存储对象
	
	private static int count = 0;
	
	public static String parseVue2FileContent(String fileName, String fileContent) {
		
		reactiveValue = "";
		
		parseFileName = fileName;
		
		parseResultMap = new HashMap<>();
		
		System.out.println("解析前：\n" + fileContent);
		
		String parseResultContent = "";
		
		parseResultContent = changeGlobalApi(fileContent);
		
		parseResultContent = changeGlobalApiTreeshaking(parseResultContent);
		
		parseResultContent = changeComponentPropertys(parseResultContent);
		
		parseResultContent = changeOptionApiToCompositionApi(parseResultContent);
		
		// 路由以及状态管理器
		parseResultContent = changeVueRoute(parseResultContent);
		
		// TypeScript 版本
		parseResultContent = changeTypeScriptVersion(parseResultContent);
		
		// 处理最终合并的结果
		parseResultContent = getVue3FileResultContent(parseResultContent);
		
		// 处理解析后的内容格式
		parseResultContent = TxtContentUtil.processFileContentFormat(parseResultContent);
		
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
					
					endIndex = TxtContentUtil.getStatementEndIndex(temp, 0);
					
					temp = temp.substring(0, endIndex);
					
					fileContent = fileContent.replaceAll(temp, "");
					
				} else {
					
					findGlobalApi = true;
					
					fileContent = fileContent.replaceAll(vue2GlobalApi, vue3GlobalApi);
				}
			}
        }
		
		if (findGlobalApi) {
			
			addVue3ImportContent("vue", "createApp");// 引入createApp
			
			addVue3DefineContent("app", "const app = createApp({});");
		}
		
		return fileContent;
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
		String templateHtmlContent = "";// 模板信息
		
		int startInex = -1;// 获取截取初始位置
		int endIndex = -1;// 获取截取结束位置
		
		// Vue.component( 形式
		if (fileContent.indexOf("Vue.component(") > -1) {
			
			startInex = fileContent.indexOf("Vue.component(");
			
			tempText = fileContent.substring(startInex, fileContent.length());
			
			endIndex = TxtContentUtil.getTagEndIndex(tempText, '(', ')');
			
			// 找到实际结束位置
			endIndex = TxtContentUtil.getStatementEndIndex(tempText, endIndex);
			
			// 得到Vue.component( 的整段代码
			vue2ResultTxt = fileContent.substring(startInex, startInex + endIndex);
			
			// 解析得到模板信息 待处理
			templateHtmlContent = "";
			
			
		}
		
		// new Vue() 形式
		if (fileContent.indexOf(" Vue(") > -1) {
			
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
		
		// export default{} 形式
		if (fileContent.indexOf("export ") > -1) {
			
			startInex = fileContent.indexOf("export ");
			
			tempText = fileContent.substring(startInex, fileContent.length());
			
			endIndex = TxtContentUtil.getTagEndIndex(tempText, '{', '}');
			
			// 找到实际结束位置
			endIndex = TxtContentUtil.getStatementEndIndex(tempText, endIndex);
			
			// 得到export default 的整段代码
			vue2ResultTxt = fileContent.substring(startInex, startInex + endIndex);
			
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
		String dataResultText = "";
		
		methodResultMap = new HashMap<>();
		propsResultMap = new HashMap<>();
		dataResultList = new ArrayList<String>();
		
		// 获取props整个信息
		if (optionsConfigText.indexOf("props:") > -1) {
			
			dataResultText = "";
			
			startInex = optionsConfigText.indexOf("props:");
			
			// props:[""]、props:{xx:xxx;}、props:{xx:{xx:xxx}} 形式
			tempText = optionsConfigText.substring(startInex + "props:".length(), optionsConfigText.length()).trim();
			
			char propsStartWith = tempText.charAt(0);
			
			tempText = TxtContentUtil.getContentByTag(optionsConfigText, startInex, propsStartWith, tempText.charAt(0) == '['?']':'}');// 得到一整个props包裹信息
			
			dataResultText = tempText;// 存储用于后续整个替换内容
			
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
				
				selfDefinePropsValue = dataResultText;// 用于后续组装拼接，以免处理data 信息部分时，props 含有data导致解析错误
				
				// 自身定义的props 部分保留
				if (!"".equals(tempText.trim())) VueProcessUtil.processVuePropsInfo(tempText.trim(), propsResultMap);
			}
			
			// 传入的props 部分清除
			optionsConfigText = TxtContentUtil.deleteFirstComma(optionsConfigText, optionsConfigText.indexOf(dataResultText) + dataResultText.length());// 删除末尾的逗号
			optionsConfigText = optionsConfigText.replace(dataResultText, "");// 替换掉整个props的内容
			
		}
		
		// 获取data中属性信息
		if (optionsConfigText.indexOf("data:") > -1 || optionsConfigText.indexOf("data()") > -1) {
			
			dataResultText = "";
			
			startInex = optionsConfigText.indexOf("data:") > -1?optionsConfigText.indexOf("data:"):optionsConfigText.indexOf("data()");
			
			tempText = TxtContentUtil.getContentByTag(optionsConfigText, startInex, '{', '}');// 得到一整个data包裹信息
			
			dataResultText = tempText;// 存储用于后续整个替换内容
			
			// 分情况解析获取内容
			if (optionsConfigText.indexOf("data()") > -1) {
				
				startInex = optionsConfigText.indexOf("data()") + "data()".length();
				
				tempText = optionsConfigText.substring(startInex, optionsConfigText.length());
				
				startInex += tempText.indexOf("return");
				
				tempText = TxtContentUtil.getContentByTag(optionsConfigText, startInex, '{', '}');
			}
			
			startInex = tempText.indexOf('{');
			
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
		
		// 拼接上自定义的props 部分信息
		if (!"".equals(selfDefinePropsValue)) {
			
			setUpContentText = selfDefinePropsValue + ",\n" + setUpContentText;
		}
		
		tempText = assembleVue3SetUpApi(getSelfPropsInfo, getParentPropsInfo);// 从得到的methodResultMap等拼接setup信息
		
		if (!"".equals(tempText)) {
			
			setUpContentText += tempText;
			
			setUpContentText += "}\n";
			
			optionsConfigText = optionsConfigText.substring(0, optionsConfigText.lastIndexOf('}')) + setUpContentText + "}";// 将setup信息拼接到末尾
		}
		
		optionsConfigTextBak = optionsConfigTextBak.replace(optionsConfigTextBak, optionsConfigText);
		
		changeResultTxt = "export default " + optionsConfigTextBak;
		
		fileContent = fileContent.replace(vue2ResultTxt, changeResultTxt);
		
		return fileContent;
	}
	
	private static String getMethodContent(String methodType, String optionsConfigText) {
			
		if (optionsConfigText.indexOf(methodType + ":") < 0) return optionsConfigText;// 没找到的情况下直接停止
		
		int startInex = -1;// 获取截取初始位置
		
		String tempText = "";// 临时处理字段
		
		startInex = optionsConfigText.indexOf(methodType + ":");
		
		tempText = TxtContentUtil.getContentByTag(optionsConfigText, startInex, '{', '}');// 得到method整个方法
		
		if ("computed".equals(methodType)) count = 0;
		
		// 得到里边所有的方法并封装成一个map对象返回
		getMethodResultMap("methods".equals(methodType)?"":methodType, tempText.substring(tempText.indexOf('{') + 1, tempText.lastIndexOf('}')).trim());
		
		return tempText;// 得到整个方法内容用于替换
	}
	
	// 获取option api中转换到setup的所有方法
	private static void getMethodResultMap(String methodType, String methodContent){
		
		// 为空的时候无需解析
		if ("".equals(methodContent)) return;
		
		String tempText = "";
		String methodContentText = "";
		
		String methodDescription = "";
		String methodName = "";
		String methodParams = "";
		String methodBody = "";
		
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
		
		// 获取方法名和参数信息
		if (methodContentText.indexOf('(') > -1) {
			methodName = methodContentText.substring(0, methodContentText.indexOf('('));
			
			tempText = methodContentText.substring(methodContentText.indexOf('(') + 1, methodContentText.length());
			methodParams = tempText.substring(0, tempText.indexOf(')'));
		}
		
		// 获取方法体信息
		tempText = methodContentText.substring(methodContentText.indexOf('{'), methodContentText.length());
		
		methodBody = TxtContentUtil.getContentByTag(tempText, 0, '{', '}');
		
		methodContent = methodContent.substring(methodContent.indexOf(methodContentText) + methodContentText.length(), methodContent.length());
		
		// 组装方法信息到map对象
		Map<String, String> methodMap = new HashMap<>();
		
		methodMap.put("methodDescription", methodDescription);
		methodMap.put("methodName", methodName);
		methodMap.put("methodParams", methodParams);
		methodMap.put("methodBody", methodBody);
		
		tempText = methodType;
		
		if ("computed".equals(methodType)) {
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
	
	private static String assembleVue3SetUpApi(String getSelfPropsInfo, String getParentPropsInfo) {
		
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
			
			// 判断是否为生命周期函数
			if (methodMap.containsKey("liftcycleFunction")) {
				
				vue3SetUpResultContent += methodMap.get("methodName") + "(() => " + methodBodyContent + ");\n";
				
				// 处理引入信息
				addVue3ImportContent("vue", methodMap.get("methodName"));
			} else {
				
				// watch 函数单独处理
				if ("watch".equals(entry.getKey())) {
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
	 * @param variableKey 变量名
	 * @param variableContent 具体定义内容
	 */
	private static String replaceThisOfSetUp(String methodBodyContent) {
		
		// 处理props 部分的  this.xxx
		for (Map.Entry<String, Map<String, String>> entry : propsResultMap.entrySet()) {
			
			methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, " this.", entry.getKey(), " ");// 直接取值或者赋值的情况
			
			methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, "\nthis.", entry.getKey(), "\n");// 换行赋值的情况
			
			methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, "(this.", entry.getKey(), "(");// 用于条件判断或运算的情况
			
			methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, "[this.", entry.getKey(), "[");// 直接取值的情况
		}
		
		// 处理state 部分的  this.xxx
		for(String dataValue:dataResultList) {
			
			methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, " this.", dataValue, " state.");
			
			methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, "\nthis.", dataValue, "\nstate.");
			
			methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, "(this.", dataValue, "(state.");
			
			methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, "[this.", dataValue, "[state.");
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
		
		int startInex = -1;// 获取截取初始位置
		
		Map<String, ArrayList> vue3ImportMap;
		ArrayList<String> importList;
		
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
					
					tempText = fileContent.substring(startInex, fileContent.length());
					
					startInex += tempText.indexOf('>') + 1;
					
					fileContent = fileContent.substring(0, startInex) + "\n" + vue3ParsePartImportContent + "\n" + fileContent.substring(startInex, fileContent.length());
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
		
		int startInex = -1;// 获取截取初始位置
		
		Map<String, String> defineMap;
		
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
				
				fileContent = fileContent.substring(0, startInex) + "\n\n" + vue3ParsePartDefineContent + "\n\n" + fileContent.substring(startInex, fileContent.length());
				
			}
		}
		
		return fileContent;
	}
	
	/**
	 * 组件属性转换
	 * 
	 * @param fileContent
	 * @return
	 */
	private static String changeComponentPropertys(String fileContent) {
		
		// 1 v-bind="@xxx" -> v-bind="xxx"
		// 2 v-on: -> @
		
		String temp = "";
		String vue2property = "";// vue2对应的属性
		String vue3property = "";// vue3对应的属性
		
		for (int i=0;i<ConvertParam.Vue2ToVue3PropertyList.length;i++) {
			
			temp = ConvertParam.Vue2ToVue3PropertyList[i];
			
			if (temp.indexOf(ConvertParam.IMPORT_STRING) > -1) {
				
				vue2property = temp.substring(0, temp.indexOf(ConvertParam.CONVERT_STRING));
				
				vue3property = temp.substring(temp.indexOf(ConvertParam.CONVERT_STRING) + 2, temp.length());
				
				if (fileContent.indexOf(vue2property) > -1) {
					
					fileContent = fileContent.replaceAll(vue2property, vue3property);
				}
			}
			
		}
		
		// 3. .sync 的部分并将其替换为 v-model
		fileContent = VueProcessUtil.replaceSyncPropsToVmodel(fileContent);
		
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
