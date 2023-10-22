package converttype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.ConvertParam;
import utils.FileOperationUtil;
import utils.TxtContentUtil;

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
	
	private static ArrayList<String> dataResultList;// data信息存储对象
	
	private static Map<String, Map> methodResultMap;// method信息存储对象
	
	public static String parseVue2FileContent(String fileName, String fileContent) {
		
		reactiveValue = "";
		
		parseFileName = fileName;
		
		parseResultMap = new HashMap<>();
		
		System.out.println("解析前：\n" + fileContent);
		
		String parseResultContent = "";
		
		parseResultContent = changeOptionApiToCompositionApi(fileContent);
		
		// 组件属性及全局API变化
		parseResultContent = changeComponentPropertys(parseResultContent);
		
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
		
		// 没得到options api ，则无需处理api的解析
		if ("".equals(vue2ResultTxt)) {
			
			return fileContent;
		}
		
		tempText = vue2ResultTxt;// 用于截取
		changeResultTxt = vue2ResultTxt;// 用于替换
		
		// 得到花括号里边的内容
		String optionsConfigText = tempText.substring(tempText.indexOf('{'), tempText.lastIndexOf('}') + 1);
		String optionsConfigTextBak = optionsConfigText;
		
		methodResultMap = new HashMap<>();
		dataResultList = new ArrayList<String>();
		
		// 获取data中属性信息
		if (optionsConfigText.indexOf("data:") > -1 || optionsConfigText.indexOf("data()") > -1) {
			
			String dataResultText = "";
			
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
			
			String[] dataList = tempText.substring(1, tempText.length() - 1).trim().split(",");
			
			reactiveValue = "const state = reactive({\n";// reactive 信息
					
			for (int i = 0;i < dataList.length;i++) {
				if (!"".equals(dataList[i])) {
					dataResultList.add(dataList[i]);
					reactiveValue += dataList[i] + ",\n";
				}
			}
			
			if (!"const state = reactive({\n".equals(reactiveValue)) {
				reactiveValue = reactiveValue.substring(0, reactiveValue.lastIndexOf(',')) + "\n";// 去除最后一个逗号
				addVue3ImportContent("vue", "reactive");
			}
			
			reactiveValue += "});\n";
			
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
		
		String setUpContentText = "setup() {\n";
		
		tempText = assembleVue3SetUpApi();// 从得到的methodResultMap等拼接setup信息
		
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
		
		// 得到里边所有的方法并封装成一个map对象返回
		getMethodResultMap("method".equals(methodType)?"":methodType, tempText.substring(tempText.indexOf('{') + 1, tempText.lastIndexOf('}')).trim());
		
		return tempText;// 得到整个方法内容用于替换
	}
	
	// 获取option api中转换到setup的所有方法
	private static void getMethodResultMap(String methodType, String methodContent){
		
		String tempText = "";
		String methodContentText = "";
		
		String methodDescription = "";
		String methodName = "";
		String methodParams = "";
		String methodBody = "";
		
		int endIndex = -1;// 获取截取结束位置
		
		// 先得到第一个方法的所有信息
		endIndex = TxtContentUtil.getTagEndIndex(methodContent, '{', '}') + 1;
		
		methodContentText = methodContent.substring(0, endIndex);
				
		// 判断是否有注释
		if (methodContentText.indexOf("/**") > -1) {
			
			endIndex = TxtContentUtil.getTagEndIndex(methodContent, '/', '/');
			methodDescription = methodContentText.substring(methodContentText.indexOf("/**"), endIndex);
		} else if (methodContentText.indexOf("//") > -1) {
			
			tempText = methodContentText.substring(methodContentText.indexOf("//"), methodContentText.length());
			
			for (int i = 0;i<tempText.length();i++) {
				if (tempText.charAt(i) == '\n') {
					endIndex = i;
					break;
				}
			}
			
			methodDescription = methodContentText.substring(methodContentText.indexOf("//"), endIndex);
			
		}
		
		if (!"".equals(methodDescription)) methodContentText = methodContentText.substring(methodContentText.indexOf(methodDescription) + methodDescription.length(), methodContentText.length());
		methodContentText = methodContentText.trim();
		
		// 获取方法名和参数信息
		if (methodContentText.indexOf('(') > -1) {
			methodName = methodContentText.substring(0, methodContentText.indexOf('('));
			
			tempText = methodContentText.substring(methodContentText.indexOf('(') + 1, methodContentText.length());
			methodParams = tempText.substring(0, tempText.indexOf(')'));
		}
		
		// 获取方法体信息
		tempText = methodContentText.substring(methodContentText.indexOf('{'), methodContentText.length());
		methodBody = tempText.substring(0, tempText.indexOf('}') + 1);
		
		methodContent = methodContent.substring(methodContent.indexOf(methodContentText) + methodContentText.length(), methodContent.length());
		
		// 组装方法信息到map对象
		Map<String, String> methodMap = new HashMap<>();
		
		methodMap.put("methodDescription", methodDescription);
		methodMap.put("methodName", methodName);
		methodMap.put("methodParams", methodParams);
		methodMap.put("methodBody", methodBody);
		
		methodResultMap.put("".equals(methodType)?methodName:methodType, methodMap);
		
		// 判断是否需要递归解析
		if (methodContent.indexOf('{') > -1) {
			getMethodResultMap(methodType, methodContent.trim());
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
	
	private static String assembleVue3SetUpApi() {
		
		String vue3SetUpResultContent = "";
		String methodBodyContent = "";
		
		Map<String, String> methodMap;
		
		// 处理reactive 部分
		vue3SetUpResultContent += reactiveValue;
		
		// 处理函数部分
		for (Map.Entry<String, Map> entry : methodResultMap.entrySet()) {
			
			methodMap = entry.getValue();
			methodBodyContent = methodMap.get("methodBody");
			
			// 处理方法中的this. 为 state.
			if (methodBodyContent.indexOf(" this.") > -1) {
				methodBodyContent = methodBodyContent.replaceAll(" this.", " state.");
			}
			
			if (methodBodyContent.indexOf("\nthis.") > -1) {
				methodBodyContent = methodBodyContent.replaceAll("\\nthis.", "\\nstate.");
			}
			
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
				else if ("computed".equals(entry.getKey())) {
					
					dataResultList.add(methodMap.get("methodName"));
					
					vue3SetUpResultContent += "const " + methodMap.get("methodName") + " = computed(() => " + methodBodyContent + ");\n";
					
					addVue3ImportContent("vue", "computed");
				} else {
					
					dataResultList.add(methodMap.get("methodName"));
					
					vue3SetUpResultContent += "const " + methodMap.get("methodName") + " = (" + methodMap.get("methodParams") + ") => " + methodBodyContent + ";\n";
				}
			}
	    }
		
		// 处理data部分
		vue3SetUpResultContent += "return {\n";
		
		for(String dataValue:dataResultList)
        {
			vue3SetUpResultContent += dataValue + ",\n";
        }
		
		// 说明无需增加setup信息
		if ("return {\n".equals(vue3SetUpResultContent)) {
			vue3SetUpResultContent = "";
		} else {
			vue3SetUpResultContent += "};\n";
		}
		
		return vue3SetUpResultContent;
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
			
			// 执行define 内容的插入，插入到export default 前面
			if (!"".equals(vue3ParsePartDefineContent)) {
				
				startInex = fileContent.indexOf("export default ");
				
				// 找不到的情况下，则添加到最后一个import 的后面
				if (startInex == -1) {
					
					tempText = fileContent.substring(fileContent.lastIndexOf("import "), fileContent.length());
					
					startInex = fileContent.lastIndexOf("import ") + TxtContentUtil.getStatementEndIndex(tempText, 0);
				}
				
				fileContent = fileContent.substring(0, startInex) + "\n" + vue3ParsePartDefineContent + "\n\n" + fileContent.substring(startInex, fileContent.length());
				
			}
		}
		
		return fileContent;
	}
	
	private static String changeComponentPropertys(String fileContent) {
		
		// 1 v-bind="@xxx" -> v-bind="xxx"
		// 2 v-on: -> @
		// 3 Vue.filter -> app.config.globalProperties.$filter
		// 4 Vue.directive -> app.directive
		// 5 Vue.mixin -> app.mixin
		
		String temp = "";
		String vue2property = "";// vue2对应的属性
		String vue3property = "";// vue3对应的属性
		String vue3import = "";// 获取需要在vue3 import部分
		String vue3define = "";// 获取需要在vue3 define部分
		String vue3importContent = "";//
		 
		Map<String, String> defineMap;
		
		if (parseResultMap.containsKey("define")) {
			defineMap = parseResultMap.get("define");
		} else {
			defineMap= new HashMap<>();
		}
		
		for (int i=0;i<ConvertParam.Vue2ToVue3PropertyList.length;i++) {
			
			temp = ConvertParam.Vue2ToVue3PropertyList[i];
			vue2property = temp.substring(0, temp.indexOf(ConvertParam.CONVERT_STRING));
			vue3import = "";
			vue3define = "";
			
			if (temp.indexOf(ConvertParam.IMPORT_STRING) > -1) {
				vue3property = temp.substring(temp.indexOf(ConvertParam.CONVERT_STRING) + 2, temp.indexOf(ConvertParam.IMPORT_STRING));
				
				if (temp.indexOf(ConvertParam.DEFINE_STRING) > -1) {
					vue3import = vue3property = temp.substring(temp.indexOf(ConvertParam.IMPORT_STRING) + 1, temp.indexOf(ConvertParam.DEFINE_STRING));
					vue3define = temp.substring(temp.indexOf(ConvertParam.DEFINE_STRING) + 1, temp.length());
				} else {
					vue3import = vue3property = temp.substring(temp.indexOf(ConvertParam.IMPORT_STRING) + 1, temp.length());
				}
				
			} else {
				vue3property = temp.substring(temp.indexOf(ConvertParam.CONVERT_STRING) + 2, temp.length());
			}
			
			if (fileContent.indexOf(vue2property) > -1) {
				fileContent = fileContent.replaceAll(vue2property, vue3property);
			}
			
			// 需要import和define的存入对应list中
			if (vue3import != "") {
				
				vue3importContent = vue3import.substring(vue3import.indexOf('{') + 1, vue3import.indexOf('}')).trim();
				
				temp = vue3import.substring(vue3import.indexOf(" from ") + " from ".length(), vue3import.length()).trim();
				
				addVue3ImportContent(temp, vue3importContent);
			}
			
			if (vue3define != "") {
				
				temp = vue3define.substring(vue3define.indexOf("const ") + "const ".length(), vue3define.indexOf(" = ")).trim();
				
				if (!defineMap.containsKey(temp)) {
					defineMap.put(temp, vue3define + ";");
				}
			}
			
		}
		
		if (defineMap.size() > 0) {
			parseResultMap.put("define", defineMap);
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
