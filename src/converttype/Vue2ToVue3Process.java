package converttype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import common.ConvertParam;
import utils.ConvertLogUtil;
import utils.FileOperationUtil;
import utils.GenerateClassFileUtil;
import utils.TxtContentUtil;
import utils.VueProcessUtil;

/**
 * vue2升级vue3处理类
 * 
 * @author 郑荣鸿（ChengWingHung）
 * @date 20231010 21:00:00 - 20231115 22:00:00
 * @version 1.0.0
 *
 */

public class Vue2ToVue3Process {

	public Vue2ToVue3Process() {
		
	}
	
	private static String parseFileName;// 当前要解析的文件名
	
	private static String parseFileNamePath;
	
	private static Map<String, Map> parseResultMap;// 解析后的信息存储对象
	
	private static Map<String, Map> methodResultMap;// method info map
	
	private static Map<String, Map<String, String>> optionApiPropMap;// optionsApi map
	
	private static Map<String, Map<String, String>> routerPropMap;// router map
	
	private static Map<String, Map<String, String>> propsResultMap;// props map
	
	private static Map<String, Map<String, String>> vuexResultMap;// vuex info map
	
	private static Map<String, Map<String, String>> clearInfoMap;// vue3 中移除的 vue2 信息
	
	private static Map<String, String> stateDataResultMap;// state data 信息存储对象
	
	private static ArrayList<String> setUpReturnResultList;// setup return data 信息
	
	private static String templateRef = "";// 模板引用
	
	private static String selfDefinePropsValue = "";// 自身定义的 props 信息
	
	private static String emitTypeDefineValue = "";// emit 类型信息
	
	private static int count = 0;// 计数器
	
	public static String parseVue2FileContent(String fileName, String relativeFilePath, String parseResultContent) throws Exception {
		
		// 先判断文件是否为vue3 版本，是的话则不继续处理
		if (VueProcessUtil.isVue3FileContent(parseResultContent)) return parseResultContent;
		
		templateRef = "";
		
		parseFileName = fileName;
		
		parseFileNamePath = relativeFilePath;
		
		parseResultMap = new HashMap<>();
		
		clearInfoMap = new HashMap<>();
		
		ConvertLogUtil.printConvertLog("local", "解析前：\n" + parseResultContent);
		
		// 全局api处理
		parseResultContent = changeGlobalApi(parseResultContent);
		
		// 全局api Treeshaking处理
		parseResultContent = changeGlobalApiTreeshaking(parseResultContent);
		
		// options API -> composition API
		parseResultContent = changeOptionApiToCompositionApi(parseResultContent);
		
		// 组件属性转换
		parseResultContent = changeComponentPropertys(parseResultContent, parseResultMap);
		
		// 国际化处理 this.$t => globalProperties.$t
		parseResultContent = replaceIn18TmethodWithGlobalT(parseResultContent);
		
		// 状态管理 this.$store => store
		parseResultContent = replaceThisStoreWithVuex(parseResultContent);
		
		// 路由
		parseResultContent = changeVueRouteInNewWay(parseResultContent);
		
		// css
		parseResultContent = changeCssDifferentUse(parseResultContent);
		
		// TypeScript 版本
		parseResultContent = changeTypeScriptVersion(parseResultContent);
		
		parseResultContent = clearVuexImportAndThisRef(parseResultContent);
		
		// 处理最终合并的结果
		parseResultContent = getVue3FileResultContent(parseResultContent);
		
		// 处理解析后的内容格式
		parseResultContent = processFileContentFormat(parseResultContent, FileOperationUtil.getFileType(parseFileName), "vue");
		
		ConvertLogUtil.printConvertLog("local", "解析后：\n" + parseResultContent);
		
		return parseResultContent;
	}
	
	/**
	 * 全局api处理
	 * 
	 * @param fileContent 要处理的内容
	 * @return String 处理后的内容
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
	
	/**
	 * 全局api Treeshaking处理
	 * 
	 * @param fileContent 要处理的内容
	 * @return String 处理后的内容
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
	 * @return String 处理后的内容
	 */
	private static String changeOptionApiToCompositionApi(String fileContent) {
		
		String tempText = "";// 临时处理字段
		String vue2ResultTxt = "";// vue2的语句截取结果
		String changeResultTxt = "";// 转换后的结果
		
		String vueComponentName = "";// Vue.component 注册的组件名
		String vueRenderContent = "";// Vue.component render 部分内容
		String vueComponentContent = "";// Vue.component 部分内容
		
		Boolean isCreateApp = false;// 是否new vue 形式
		
		int startInex = -1;// 获取截取初始位置
		int endIndex = -1;// 获取截取结束位置
		
		// $children => ref  这里先判断是否使用到了$children 有的话添加ref ，名字为文件名小写，模板引用用于替换$children
		fileContent = setTemplateChildrenWithRef(fileContent);
		
		// Vue.component( 形式，单独处理，前面全局api 处已替换为 app.component
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
			
			// 获取render 返回信息，render的处理参照https://cn.vuejs.org/api/render-function.html#h
			vueRenderContent = VueProcessUtil.getRenderContentFunction(vueComponentContent);
			
			if (!"".equals(vueRenderContent)) {

				addVue3ImportContent("vue", "h");// 导入h
				
				tempText = vueComponentContent.substring(vueComponentContent.indexOf("render"), vueComponentContent.indexOf(vueRenderContent));
				
				fileContent = fileContent.replace(tempText, "render" + (tempText.indexOf('(') > -1?tempText.substring(tempText.indexOf('('), tempText.lastIndexOf(')') + 1):"()"));
				
			}
			
			if (fileContent.indexOf(" Vue(") < 0 && fileContent.indexOf("export ") < 0) {
				
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
		
		stateDataResultMap = new HashMap<>();
		setUpReturnResultList = new ArrayList<String>();
		
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
				
				if (!"".equals(vueComponentName)) {
					
					addVue3ImportContent("vue", "resolveComponent");// 导入resolveComponent
					
					setUpReturnResultList.add(TxtContentUtil.reNameVariable(vueComponentName));
					
				}
				
				tempText = VueProcessUtil.getRenderAllContentFunction(vue2ResultTxt);
				
				vueComponentContent = tempText;
				
			}
		}
		
		// 1. 没得到options api ，则无需处理api的解析
		// 2. export default xxx
		if ("".equals(vue2ResultTxt) || vue2ResultTxt.indexOf('{') < 0) {
			
			fileContent = getVue3FileResultContent(fileContent);
			
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
		
		// 说明无需处理
		if (optionApiPropMap.size() == 0) return fileContent;
		
		Map<String, String> apiDataMap = new HashMap<>();
		
		String apiTempText = "";
		
		selfDefinePropsValue = "";
		
		// 获取mixins信息，直接添加到return中
		if (optionApiPropMap.containsKey("mixins")) {
			
			apiDataMap = optionApiPropMap.get("mixins");
			
			for (String mixinValue:apiDataMap.get("apiNameValue").split(",")) {
				
				if(!"".equals(mixinValue)) setUpReturnResultList.add(mixinValue);
			}
			
		}
		
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
				if (!"".equals(tempText.trim())) VueProcessUtil.processVuePropsInfo(tempText.trim(), propsResultMap, "");
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
			if (tempText.indexOf("return") > -1) {
				
				startInex = tempText.indexOf("return") + "return".length();
				
				startInex += tempText.substring(startInex, tempText.length()).indexOf('{');
			} else {
				
				startInex = tempText.indexOf('{');
			}
			
			tempText = TxtContentUtil.getContentByTag(tempText, startInex, '{', '}'); // 得到整个data信息
			
			tempText = tempText.substring(1, tempText.length() - 1).trim();
			
			// 获得data信息
			VueProcessUtil.processVueDataInfo(tempText, stateDataResultMap);
			
			// return state
			if (stateDataResultMap.size() > 0) setUpReturnResultList.add("state");
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
		
		vuexResultMap = new HashMap<>();
		
		// 判断filters、computed、watch、methods
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
					
					tempText = entry.getKey();
					
					tempText = tempText.trim();
					
					if (tempText.charAt(0) == tempText.charAt(tempText.length() - 1) && !String.valueOf(tempText.charAt(0)).matches(ConvertParam.JS_VARIABLE_REG)) {
						
						tempText = tempText.substring(1, tempText.length() - 1);
					}
						
					getParentPropsInfo += tempText + ",\n";
				} else {
					
					getSelfPropsInfo += entry.getKey() + ",\n";
					
					selfPropDefineInfo += "const " + entry.getKey() + " = reactive(props." + entry.getKey() + ");\n";
				}
			}
		}
		
		// vuex 信息
		String importVuexContent = "";
		String vuexDefineContent = "";
		
		if (vuexResultMap.size() > 0) {
			
			Map<String, String> vuexMap;
			
			for (Map.Entry<String, Map<String, String>> entry : vuexResultMap.entrySet()) {
				
				vuexMap = entry.getValue();
				
				tempText = vuexMap.get("vuexName");
				
				importVuexContent += tempText + ", ";
				
				// 变量名
				tempText = "iNeedYouMapStore" + tempText.substring(3, tempText.length());
				
				// 添加到return 
				setUpReturnResultList.add("..." + tempText);
				
				vuexDefineContent += "const " + tempText + " = " + vuexMap.get("vuexName") + vuexMap.get("vuexParamValue") + ";\n";
			}
			
			// 计算相对路径
			String vuexHooksRelativePath = "";
			
			if (parseFileNamePath.split("/").length - 1 > 0) {
				
				for (int i=0;i<parseFileNamePath.split("/").length - 1;i++) {
					
					vuexHooksRelativePath += "../";
				}
				
				vuexHooksRelativePath += "vuex-hooks/vuexHooks.js";
			} else {
				
				vuexHooksRelativePath = "./vuex-hooks/vuexHooks.js";
			}
			
			addVue3ImportContent(vuexHooksRelativePath, importVuexContent.substring(0, importVuexContent.length() - 2));// vuex-hooks
			
			GenerateClassFileUtil.fileListMap.put("vuex-hooks/vuexHooks.js", GenerateClassFileUtil.VuexMethodContent);
		}
		
		String setUpContentText = "setup() {\n";
		
		if (!"".equals(selfPropDefineInfo)) {
			
			addVue3ImportContent("vue", "reactive");// 引入reactive
			
			setUpContentText = "setup(props) {\n" + selfPropDefineInfo;
		}
		
		// vuex 定义部分
		if (!"".equals(vuexDefineContent)) {
			
			setUpContentText += vuexDefineContent;
		}
		
		if (!"".equals(vueComponentName)) {
			
			setUpContentText += "const " + TxtContentUtil.reNameVariable(vueComponentName) + " = resolveComponent(\"" + vueComponentName + "\");\n";
		}
		
		// 拼接上自定义的props 部分信息
		if (!"".equals(selfDefinePropsValue)) {
			
			setUpContentText = selfDefinePropsValue + ",\n" + setUpContentText;
		}
		
		tempText = assembleVue3SetUpApi(getSelfPropsInfo, getParentPropsInfo);// 从得到的methodResultMap等拼接setup信息
		
		// 处理state reactive 部分
		if (stateDataResultMap.size() > 0) {
			
			addVue3ImportContent("vue", "reactive");
			
			apiTempText = "";
			
			for (Map.Entry<String, String> entry : stateDataResultMap.entrySet()) {
				
				if (!"".equals(entry.getValue())) {
					
					apiTempText += entry.getValue() + ",\n";
				} else {
					
					apiTempText += entry.getKey() + ",\n";
				}
				
			}
			
			tempText = "// 推荐使用ref" + ConvertParam.RECOMMEND_BY_CONVERTVAR + "\n" + "const state = reactive({\n" + apiTempText + "});\n" + tempText;
		}
		
		// 处理setup 中需要增加的定义信息
		if (parseResultMap.containsKey("setupDefine")) {
			
			Map<String, String> setupDefineMap = parseResultMap.get("setupDefine");
			
			for (Map.Entry<String, String> entry : setupDefineMap.entrySet()) {
				
				tempText = entry.getValue() + "\n" + tempText;
			}
		}
		
		// emit 事件信息不为空
		if (!"".equals(emitTypeDefineValue)) {
			
			if (!parseResultMap.containsKey("setupContext")) {
				
				Map<String, String> contextMap = new HashMap<>();
				
				parseResultMap.put("setupContext", contextMap);
			}
			
			setUpContentText = "emits: [\n" + emitTypeDefineValue.substring(0, emitTypeDefineValue.length() - 1) + "\n],\n" + setUpContentText;
		}
		
		// 引入context
		if (parseResultMap.containsKey("setupContext")) {
			
			if (setUpContentText.indexOf("setup(props)") > -1) {
				
				setUpContentText = setUpContentText.replace("setup(props)", "setup(props, context)");
			} else if (setUpContentText.indexOf("setup()") > -1) {
				
				setUpContentText = setUpContentText.replace("setup()", "setup(context)");
			}
		}
		
		if (!"".equals(tempText)) {
			
			setUpContentText += tempText;
			
			setUpContentText += "}\n";
			
			tempText = optionsConfigText.substring(0, optionsConfigText.lastIndexOf('}'));
			
			if ('{' != tempText.trim().charAt(tempText.trim().length() - 1) && ',' != tempText.trim().charAt(tempText.trim().length() - 1)) tempText += ",\n";
			
			optionsConfigText = tempText + setUpContentText + "}";// 将setup信息拼接到末尾
		}
		
		optionsConfigTextBak = optionsConfigTextBak.replace(optionsConfigTextBak, optionsConfigText);
		
		// new vue => createapp
		if (isCreateApp) {
			
			String variableVueName = "";// 组件别名
			String elementIdValue = "";// 组件el id值
			String elementRenderValue = "";// 组件render 内容
			
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
			
			// 1. 判断router 信息 => use(router)
			if (optionApiPropMap.containsKey("router")) {
				
				changeResultTxt = VueProcessUtil.getNewVueOptionsUseContent(optionApiPropMap, changeResultTxt, "router");
			}
			
			// 2. 判断store 信息 => use(store)
			if (optionApiPropMap.containsKey("store")) {
				
				changeResultTxt = VueProcessUtil.getNewVueOptionsUseContent(optionApiPropMap, changeResultTxt, "store");
			}
			
			// 3. 判断i18n 信息 => use(i18n)
			if (optionApiPropMap.containsKey("i18n")) {
				
				changeResultTxt = VueProcessUtil.getNewVueOptionsUseContent(optionApiPropMap, changeResultTxt, "i18n");
			}
			
			// 4. 处理render 信息
			if (optionApiPropMap.containsKey("render")) {
				
				tempText = optionApiPropMap.get("render").get("apiNameValue");
				
				if (tempText.indexOf(" h(") > -1) {
					
					tempText = tempText.substring(tempText.indexOf(" h(") + " h(".length(), tempText.length());
					
					elementRenderValue = tempText.substring(0, tempText.lastIndexOf(')'));
					
				}
			}
			
			// 5. 判断el 信息
			if (!"".equals(elementIdValue)) {
				
				if (!"".equals(variableVueName) && fileContent.indexOf(variableVueName + ".$mount(") < 0) {
					
					changeResultTxt += "\n" + variableVueName + ".mount('" + elementIdValue + "');";
				} else if (fileContent.substring(fileContent.indexOf(vue2ResultTxt) + vue2ResultTxt.length(), fileContent.length()).indexOf(".mount(") != 0) {
					
					changeResultTxt = "\n" + changeResultTxt + ".mount('" + elementIdValue + "');";
				}
				
				// 去除el 信息
				tempText = changeResultTxt.substring(changeResultTxt.indexOf("el:"), changeResultTxt.indexOf(elementIdValue) + elementIdValue.length() + 1);
				
				if ("'\"".indexOf(tempText.charAt(tempText.length() - 1)) < 0) {
					
					tempText = tempText.substring(0, tempText.length() - 1);
				}
				
				changeResultTxt = TxtContentUtil.deleteFirstComma(changeResultTxt, changeResultTxt.indexOf(tempText) + tempText.length());// 删除末尾的逗号
				changeResultTxt = changeResultTxt.replace(tempText, "");
				
			}
			
			// 替换createApp内容
			if (!"".equals(elementRenderValue)) {
				
				endIndex = TxtContentUtil.getTagEndIndex(changeResultTxt, '(', ')');
				
				changeResultTxt = changeResultTxt.substring(0, changeResultTxt.indexOf("createApp(") + "createApp(".length()) + elementRenderValue + changeResultTxt.substring(endIndex, changeResultTxt.length());
			}
			
		} else {
			
			changeResultTxt = "export default " + optionsConfigTextBak;
		}
		
		/*if (!"".equals(vueComponentContent)) {
			
			changeResultTxt = TxtContentUtil.deleteFirstComma(changeResultTxt, changeResultTxt.indexOf(vueComponentContent) + vueComponentContent.length());// 删除末尾的逗号
			changeResultTxt = changeResultTxt.replace(vueComponentContent, "");
		}*/
		
		fileContent = fileContent.replace(vue2ResultTxt, changeResultTxt);
		
		// 还有new Vue 的情况，继续处理
		if (fileContent.indexOf(" Vue(") > -1) {
			
			return changeOptionApiToCompositionApi(fileContent);
		}
		
		return fileContent;
	}
	
	public static String changeComponentPropertys(String fileContent, Map<String, Map> parseResultMap) {
		
		return VueProcessUtil.changeComponentPropertys(fileContent, parseResultMap);
	}
	
	private static String getMethodContent(String methodType, String optionsConfigText) {
			
		if (!optionApiPropMap.containsKey(methodType)) return optionsConfigText;// 没找到的情况下直接停止
		
		int startInex = -1;// 获取截取初始位置
		
		String tempText = "";// 临时处理字段
		String methodHeader = "";
		String methodContent = "";
		
		tempText = optionApiPropMap.get(methodType).get("apiNameEndChar");
		
		if (tempText.length() > 0) {
			
			if (':' == tempText.charAt(0)) {
				
				methodHeader = methodType + ":";
				
				startInex = optionsConfigText.indexOf(methodType + ":");
			} else if ('(' == tempText.charAt(0)) {
				
				tempText = optionsConfigText.substring(optionsConfigText.indexOf(methodType + "("), optionsConfigText.length());
				
				methodHeader = methodType + ":" + tempText.substring(0, tempText.indexOf(')') + 1);
				
				startInex = optionsConfigText.indexOf(methodHeader);
			}
		} else {
			
			methodHeader = methodType + ":";
			
			startInex = optionsConfigText.indexOf(methodType + ":");
		}
		
		tempText = optionsConfigText.substring(startInex, optionsConfigText.length());
		
		if (tempText.indexOf(',') > -1) {
			
			tempText = tempText.substring(0, tempText.indexOf(','));
		}
		
		if ("computed".equals(methodType) || "watch".equals(methodType) || "filters".equals(methodType)) {
			
			count = 0;
		}
		
		if (tempText.indexOf('{') > -1) {
			
			tempText = TxtContentUtil.getContentByTag(optionsConfigText, startInex, '{', '}');// 得到method整个方法
			
			methodContent = tempText.substring(tempText.indexOf('{') + 1, tempText.lastIndexOf('}'));
		} else if (tempText.indexOf('(') > -1) {
			
			tempText = optionsConfigText.substring(startInex, optionsConfigText.length());
			
			tempText = tempText.substring(0, TxtContentUtil.getTagEndIndex(tempText, '(', ')') + 1);
			
			methodContent = tempText;
			
		} else {
			
			methodContent = tempText;
		}
		
		// 得到里边所有的方法并封装成一个map对象返回
		getMethodResultMap("methods".equals(methodType)?"":methodType, methodContent.trim(), "", methodHeader);
		
		return tempText;// 得到整个方法内容用于替换
	}
	
	/**
	 * 获取option api中转换到setup的所有方法
	 * 
	 * @param methodType
	 * @param methodContent
	 */ 
	private static void getMethodResultMap(String methodType, String methodContent, String methodDescription, String methodHeader){
		
		// 处理vuex 中的四个map方法，处理一次即可
		if ("".equals(methodDescription)) methodContent = VueProcessUtil.processVuexMapMethod(methodContent, vuexResultMap);
		
		// 为空的时候无需解析
		if ("".equals(methodContent) || methodContent.trim().equals(methodHeader)) return;
		
		String tempText = "";
		String methodName = "";
		String methodParams = "";
		String methodBody = "";
		String methodContentText = "";
		
		int endIndex = -1;// 获取截取结束位置
		
		tempText = TxtContentUtil.getCommentInformation(methodContent);
		
		if (!"".equals(tempText)) {
			
			methodContent = methodContent.substring(methodContent.indexOf(tempText) + tempText.length(), methodContent.length());
			
			// 判断是否有注释
			if ("".equals(methodDescription)) {
				
				methodDescription = tempText;
			} else {
				
				methodDescription += "\n" + tempText;
			}
			
		}
				
		// 还有注释信息，继续清除
		if (methodContent.trim().indexOf("<--") == 0 || methodContent.trim().indexOf("/*") == 0 || methodContent.trim().indexOf("//") == 0) {
			getMethodResultMap(methodType, methodContent.trim(), methodDescription, methodHeader);
			return;
		}
		
		if ("".equals(methodContent.trim())) return;
		
		// 防止参数里边含有{}导致解析异常
		if (methodContent.indexOf('(') < methodContent.indexOf('{')) {
			
			endIndex = TxtContentUtil.getTagEndIndex(methodContent, '(', ')') + 1;
		} else {
			
			endIndex = -1;
		}
		
		Boolean isArrowMethod = false;
		
		// 先得到第一个方法的所有信息
		if (endIndex == -1) {
			
			endIndex = TxtContentUtil.getVariableStartIndex(methodContent, 0);
			
			if (endIndex != -1) methodContent = methodContent.substring(endIndex, methodContent.length());
			
			endIndex = TxtContentUtil.getNotVariableIndex(methodContent, 0);
			
			// 箭头函数的情况
			if (':' == methodContent.charAt(endIndex)) {
				
				tempText = methodContent.substring(endIndex + 1, methodContent.length());
				
				if (tempText.indexOf("{") > -1 && tempText.indexOf("=>") > -1 && tempText.indexOf("=>") < tempText.indexOf("{")) {
					
					// 参数无括号
					if ('(' != tempText.substring(0, tempText.indexOf("=>")).trim().charAt(0)) {
						
						methodContent = methodContent.substring(0, endIndex + 1) + "(" + tempText.substring(0, tempText.indexOf("=>")).trim() + ")" + methodContent.substring(endIndex + 1 + tempText.indexOf("=>"), methodContent.length());
					
						tempText = methodContent.substring(endIndex + 1, methodContent.length());
					}
					
					isArrowMethod = true;
					
				} else {
					
					int startIndex = -1;
					
					if ('(' == tempText.trim().charAt(0)) {
						
						startIndex = TxtContentUtil.getTagEndIndex(tempText, '(', ')');
						
						if (tempText.length() > startIndex + 2 && tempText.substring(startIndex + 2, tempText.length()).trim().indexOf("=>") == 0) {
							
							isArrowMethod = true;
						}
					} else {
						
						tempText = tempText.trim();
						
						startIndex = TxtContentUtil.getNotVariableIndex(tempText, 0);
						
						tempText = tempText.substring(startIndex, tempText.length()).trim();
						
						if (tempText.indexOf("=>") == 0) {
							// 添加参数括号
							methodContent = methodContent.substring(0, endIndex + 1) + " (" + methodContent.substring(endIndex + 1, methodContent.indexOf("=>")).trim() + ") " + methodContent.substring(methodContent.indexOf("=>"), methodContent.length());
							
							isArrowMethod = true;
						}
						
					}
				}
			}
			
			if (isArrowMethod && '{' != methodContent.substring(methodContent.indexOf("=>") + 2, methodContent.length()).trim().charAt(0)) {
				
				endIndex = methodContent.indexOf("=>");
				
				tempText = methodContent.substring(methodContent.indexOf("=>"), methodContent.length());
				
				endIndex += TxtContentUtil.getStatementEndIndex(tempText, 0);
				
				tempText = methodContent.substring(methodContent.indexOf("=>") + 2, endIndex);
				
				if ('[' == tempText.trim().charAt(0)) {
					
					endIndex = methodContent.indexOf("=>") + 2 + TxtContentUtil.getTagEndIndex(tempText, '[', ']');
				} else if (tempText.indexOf(',') > -1) {
					
					endIndex = methodContent.indexOf("=>") + 2 + tempText.indexOf(',');
				} else {
					
					endIndex++;
				}
				
				isArrowMethod = true;
			} else {
				
				if (methodContent.indexOf('{') > -1) {
					
					endIndex = TxtContentUtil.getTagEndIndex(methodContent, '{', '}') + 1;
				} else {
					
					endIndex = methodContent.length();
				}
				
				isArrowMethod = false;
			}
			
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
		
		if (isArrowMethod) {
			
			methodName = methodContentText.substring(0, methodContentText.indexOf(':'));
			
			tempText = methodContentText.substring(methodContentText.indexOf('(') + 1, methodContentText.length());
			methodParams = tempText.substring(0, tempText.indexOf(')'));
			
			methodBody = methodContentText.substring(methodContentText.indexOf("=>") + 2, methodContentText.length());
			
		} else if (methodContentText.indexOf(':') > -1 && methodContentText.indexOf(':') < methodContentText.indexOf('{')) {
			
			methodName = methodContentText.substring(0, methodContentText.indexOf(':'));
			
			// 此处computed 和 watch 的处理不同，分开判断
			if ("computed".equals(methodType)) {
				
				methodContentText = methodContentText.substring(methodContentText.indexOf('{'), methodContentText.length());
				
				methodBody = methodContentText;
				
			} else {
				
				tempText = methodContentText.substring(methodContentText.indexOf(':') + 1, methodContentText.length());
				
				if (tempText.trim().indexOf("function") > -1) {
					
					methodContentText = methodContentText.substring(methodContentText.indexOf("function") + "function".length(), methodContentText.length());
				}
				
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
			} else if (methodContentText.indexOf(':') > -1 && "watch".equals(methodType)) {
				
				methodName = methodContentText.substring(0, methodContentText.indexOf(':')).trim();
				
				if (!String.valueOf(methodName.charAt(0)).matches(ConvertParam.JS_VARIABLE_REG)) {
					
					methodName = methodName.substring(1, methodName.length() - 1);
				}
				
				methodParams = "";
				
				tempText = methodContentText.substring(methodContentText.indexOf(':') + 1, methodContentText.length()).trim();
				
				if ("'\"".indexOf(tempText.charAt(0)) > -1) {
					
					methodBody = TxtContentUtil.getContentByTag(tempText, 0, tempText.charAt(0), tempText.charAt(0));
					
					methodBody = "{\n" + methodBody.substring(1, methodBody.length() - 1) + "();\n}\n";
				}
			}
			
			if ("".equals(methodBody)) {
				
				// 获取方法体信息
				if (methodContentText.indexOf('{') > -1) {
					
					tempText = methodContentText.substring(methodContentText.indexOf('{'), methodContentText.length());
					
					methodBody = TxtContentUtil.getContentByTag(tempText, 0, '{', '}');
				} else {
					
					methodBody = methodContentText;
				}
				
			}
			
		}
		
		methodContent = methodContent.substring(methodContent.indexOf(methodContentText) + methodContentText.length(), methodContent.length());
		
		String isAsync = "no";
		
		if (methodName.indexOf("async ") > -1) {
			
			methodName = methodName.substring(methodName.indexOf("async ") + "async ".length(), methodName.length()).trim();
			
			isAsync = "yes";
		}
		
		// 组装方法信息到map对象
		Map<String, String> methodMap = new HashMap<>();
		
		methodMap.put("methodDescription", methodDescription);
		methodMap.put("isAsync", isAsync);
		methodMap.put("methodName", methodName);
		methodMap.put("methodParams", methodParams);
		methodMap.put("methodBody", methodBody);
		
		tempText = methodType;
		
		if ("computed".equals(methodType) || "watch".equals(methodType) || "filters".equals(methodType)) {
			tempText = methodType + "_" + count;
			count++;
		}
		
		methodResultMap.put("".equals(tempText)?methodName:tempText, methodMap);
		
		// 判断是否需要递归解析
		if (methodContent.indexOf('{') > -1 || methodContent.indexOf("=>") > -1) {
			
			methodContent = methodContent.trim();
			
			// 第一个如果是逗号
			if (methodContent.indexOf(',') == 0) methodContent = methodContent.substring(1, methodContent.length()).trim();
			
			getMethodResultMap(methodType, methodContent, "", methodHeader);
		}
		
	}
	
	private static String changeComponentLifeycle(String fileContent) {
		
		// 1 beforeCreate -> setup
		// 2 created -> setup
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
			
			vue2LiftcycleName = VueProcessUtil.getExistFunction(fileContent, vue2LiftcycleName);
				
			// 判断是否有对应生命周期函数
			if (!"".equals(vue2LiftcycleName) && fileContent.indexOf(vue2LiftcycleName + "()") > -1) {
				
				startInex = fileContent.indexOf(vue2LiftcycleName + "()");
				
				vue2MethodContent = TxtContentUtil.getContentByTag(fileContent, startInex, '{', '}');
				
				// 判断有无转换标志
				if (tempTxt.indexOf(ConvertParam.CONVERT_STRING) > -1) {
					
					vue3LiftcycleName = tempTxt.substring(tempTxt.indexOf(ConvertParam.CONVERT_STRING) + 2, tempTxt.length());
					
				} else {
					
					vue3LiftcycleName = tempTxt;
				}
				
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
	 * @return String
	 */
	private static String assembleVue3SetUpApi(String getSelfPropsInfo, String getParentPropsInfo) {
		
		emitTypeDefineValue = "";
		
		String tempMethodText = "";
		String methodDescription = "";
		String methodBodyContent = "";
		String vue3SetUpResultValue = "";
		String vue3SetUpResultContent = "";
		
		String beforeCreateAndCreatedContent = "";
		
		Map<String, String> methodMap;
		
		// 处理函数部分
		for (Map.Entry<String, Map> entry : methodResultMap.entrySet()) {
			
			vue3SetUpResultValue = "";
			
			methodMap = entry.getValue();
			methodBodyContent = methodMap.get("methodBody");
			
			// 判断是否为生命周期函数
			if (methodMap.containsKey("liftcycleFunction")) {
				
				getAllVariableOfMethodUse(methodBodyContent);
				
				if ("created,beforeCreate".indexOf(methodMap.get("methodName")) > -1) {
					
					if ('{' == methodBodyContent.trim().charAt(0)) {
						
						methodBodyContent = methodBodyContent.substring(methodBodyContent.indexOf('{') + 1, methodBodyContent.lastIndexOf('}'));
					}
					
					methodBodyContent = replaceThisKeyWordOfSetUp(methodBodyContent);
					
					methodBodyContent = "// generated from vue2 method of " + methodMap.get("methodName") + " start\n" + methodBodyContent + "// generated from vue2 method of " + methodMap.get("methodName") + " end\n";
					
					// beforeCreate 排到 created 之前
					if ("beforeCreate".equals(methodMap.get("methodName"))) {
						
						beforeCreateAndCreatedContent = methodBodyContent + beforeCreateAndCreatedContent;
					} else {
						
						beforeCreateAndCreatedContent += methodBodyContent;
					}
					
				} else {
					
					vue3SetUpResultValue = methodMap.get("methodName") + "(() => " + methodBodyContent + ");\n";
					
					// 处理引入信息
					addVue3ImportContent("vue", methodMap.get("methodName"));
				}
				
			} else {
				
				// watch 函数单独处理
				if (entry.getKey().indexOf("watch_") == 0) {
					
					getAllVariableOfMethodUse(methodBodyContent);
					
					// 是否异步调用
					if ("yes".equals(methodMap.get("isAsync"))) {
						
						vue3SetUpResultValue = "watch(" + methodMap.get("methodName") + ", async (" + methodMap.get("methodParams") + ") => " + methodBodyContent + ");\n";
					} else {
						
						vue3SetUpResultValue = "watch(" + methodMap.get("methodName") + ", (" + methodMap.get("methodParams") + ") => " + methodBodyContent + ");\n";
					}
					
					addVue3ImportContent("vue", "watch");
				} 
				// computed 函数单独处理
				else if (entry.getKey().indexOf("computed_") == 0) {
					
					setUpReturnResultList.add(methodMap.get("methodName"));
					
					// set 和 get 方法处理
					methodBodyContent = processComputedSetOrGetFunc(methodBodyContent, "set");
					methodBodyContent = processComputedSetOrGetFunc(methodBodyContent, "get");
					
					if (methodBodyContent.indexOf("set(") > -1 || methodBodyContent.indexOf("get(") > -1) {
						
						vue3SetUpResultValue += "const " + methodMap.get("methodName") + " = computed(" + methodBodyContent + ");\n";
					} else {
						
						vue3SetUpResultValue += "const " + methodMap.get("methodName") + " = computed(() => " + methodBodyContent + ");\n";
					}
					
					addVue3ImportContent("vue", "computed");
				} else {
					
					setUpReturnResultList.add(methodMap.get("methodName"));
					
					methodDescription = methodMap.get("methodDescription");
					
					if (!"".equals(methodDescription)) methodDescription += "\n";
					
					getAllVariableOfMethodUse(methodBodyContent);
					
					// 是否异步调用
					if ("yes".equals(methodMap.get("isAsync"))) {
						
						vue3SetUpResultValue = methodDescription + "const " + methodMap.get("methodName") + " = async (" + methodMap.get("methodParams") + ") => " + methodBodyContent + ";\n";
					} else {
						
						vue3SetUpResultValue = methodDescription + "const " + methodMap.get("methodName") + " = (" + methodMap.get("methodParams") + ") => " + methodBodyContent + ";\n";
					}
					
				}
			}
			
			if (!"".equals(vue3SetUpResultValue)) {
				
				tempMethodText = methodBodyContent;
						
				// 处理setup方法中的this
				tempMethodText = replaceThisKeyWordOfSetUp(tempMethodText);
				
				vue3SetUpResultValue = vue3SetUpResultValue.substring(0, vue3SetUpResultValue.lastIndexOf(methodBodyContent)) + tempMethodText + vue3SetUpResultValue.substring(vue3SetUpResultValue.lastIndexOf(methodBodyContent) + methodBodyContent.length(), vue3SetUpResultValue.length());
				
				vue3SetUpResultContent += vue3SetUpResultValue;
			}
	    }
		
		// created,beforeCreate
		vue3SetUpResultContent += beforeCreateAndCreatedContent;
		
		// 处理data部分
		vue3SetUpResultContent += "return {\n";
		
		for(String dataValue:setUpReturnResultList)
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
	 * @return String
	 */
	private static String replaceThisKeyWordOfSetUp(String methodBodyContent) {
		
		Map<String, String> methodMap;
		
		String methodName = "";
		
		// 1. 处理method 部分的 this.xxx
		for (Map.Entry<String, Map> entry : methodResultMap.entrySet()) {
			
			methodMap = entry.getValue();
			
			// 非生命周期部分的函数
			if (!methodMap.containsKey("liftcycleFunction")) {
				
				methodName = methodMap.get("methodName") + "(";
				
				methodBodyContent = preReplaceThisOfVue2Method(methodBodyContent, methodName, "");
			}
		}
		
		// 2. 处理props 部分的  this.xxx
		for (Map.Entry<String, Map<String, String>> entry : propsResultMap.entrySet()) {
			
			methodBodyContent = preReplaceThisOfVue2Method(methodBodyContent, entry.getKey(), "");
		}
		
		// 3. 处理state 部分的  this.xxx
		for (Map.Entry<String, String> entry : stateDataResultMap.entrySet()) {
			
			methodBodyContent = preReplaceThisOfVue2Method(methodBodyContent, entry.getKey(), "state.");
		}
		
		// 4. 处理setup方法中的路由读取
		// methodBodyContent = replaceRouterInfoOfSetUp(methodBodyContent, "this.$router", "this.$route");
		// methodBodyContent = replaceRouterInfoOfSetUp(methodBodyContent, "$router", "$route");
		
		// 5. this.$emit => context.emit in setup
		methodBodyContent = replaceThisEmitWithContext(methodBodyContent);
		
		// 6. this.$set() 和 this.$delete 为vue2 中解决无法监听复杂数据类型属性新增删除
		methodBodyContent = VueProcessUtil.removeVue2BindObjectInfoChangeProcess(methodBodyContent, "set");
		methodBodyContent = VueProcessUtil.removeVue2BindObjectInfoChangeProcess(methodBodyContent, "delete");
		
		// 7. this.$message => Message
		methodBodyContent = replaceThisMessageWithElMessage(methodBodyContent, false);
		
		if (!"".equals(templateRef)) {
			
			// 8. this.$children => this.$ref.templateRef
			methodBodyContent = replaceThisChildrenByRef(methodBodyContent);
		}
		
		// 9. this.$refs => ref
		methodBodyContent = replaceRefInfoGetMethod(methodBodyContent);
		
		methodName = methodBodyContent;
		
		// 10. $listeners => context.attrs
		methodBodyContent = TxtContentUtil.replaceAll(methodBodyContent, "$listeners", "context.attrs");
		
		if (methodName.length() != methodBodyContent.length()) {
			
			Map<String, String> contextMap = new HashMap<>();
			
			parseResultMap.put("setupContext", contextMap);
		}
		
		return methodBodyContent;
	}
	
	private static String preReplaceThisOfVue2Method(String methodBodyContent, String originValue, String repalceValue) {
		
		methodBodyContent = VueProcessUtil.replaceThisOfVue2Method(methodBodyContent, "this.", originValue, originValue, repalceValue);
		
		return methodBodyContent;
	}
	
	/**
	 * 替换setup 里边的路由读取
	 * this.$route => vueRoute 读取到当前路由信息 
	 * this.$router => vueRouter 读取到路由表信息
	 * 
	 * @param methodBodyContent 方法体
	 * @param routerType
	 * @param routeType
	 * @return String
	 */
	private static String replaceRouterInfoOfSetUp(String methodBodyContent, String routerType, String routeType) {
		
		int startIndex = -1;
		
		// this.$router
		if (methodBodyContent.indexOf(routerType) > -1) {
			
			addVue3ImportContent("vue-router", "useRouter");// 引入useRouter
			
			addVue3DefineContent("vueRouter", "\nconst vueRouter = useRouter();\n", "define");
			
			startIndex = methodBodyContent.indexOf(routerType) + routerType.length();
			
			return methodBodyContent.substring(0, startIndex).replace(routerType, "vueRouter") + replaceRouterInfoOfSetUp(methodBodyContent.substring(startIndex, methodBodyContent.length()), routerType, routeType);
		}
		
		// this.$route
		if (methodBodyContent.indexOf(routeType) > -1) {
			
			addVue3ImportContent("vue-router", "useRoute");// 引入useRoute
			
			addVue3DefineContent("vueRoute", "\nconst vueRoute = useRoute();\n", "define");
			
			startIndex = methodBodyContent.indexOf(routeType) + routeType.length();
			
			return methodBodyContent.substring(0, startIndex).replace(routeType, "vueRoute") + replaceRouterInfoOfSetUp(methodBodyContent.substring(startIndex, methodBodyContent.length()), routerType, routeType);
		}
		
		return methodBodyContent;
	}
	
	/**
	 * 处理vue2 this.$emit => context.emit 并获取emit 信息用于后续组装
	 * 
	 * @param methodBodyContent
	 * @return String
	 */
	public static String replaceThisEmitWithContext(String methodBodyContent) {
		
		String temp = "";
		String emitName = "";
		
		int endIndex = -1;// 获取截取结束位置
		
		if (methodBodyContent.indexOf("this.$emit") > -1) {
			
			temp = methodBodyContent.substring(methodBodyContent.indexOf("this.$emit"), methodBodyContent.length());
			
			endIndex = TxtContentUtil.getTagEndIndex(temp, '(', ')') + 1;
					
			temp = temp.substring(0, endIndex);// 得到整个emit 内容
			
			// this.$emit('callback')
			if (temp.indexOf(',') < 0) {
				
				emitName = temp.substring(temp.indexOf('(') + 1, temp.indexOf(')'));// 得到事件名
			} else {
				
				emitName = temp.substring(temp.indexOf('(') + 1, temp.indexOf(','));// 得到事件名
			}
			
			if (emitTypeDefineValue.indexOf(emitName) < 0) emitTypeDefineValue += emitName + ",\n";
			
			return methodBodyContent.substring(0, methodBodyContent.indexOf("this.$emit") + "this.$emit".length()).replace("this.$emit", "context.emit") + replaceThisEmitWithContext(methodBodyContent.substring(methodBodyContent.indexOf("this.$emit") + "this.$emit".length(), methodBodyContent.length()));
		}
		
		return methodBodyContent;
	}
	
	/**
	 * this.$store => store 
	 * 引入 import {useStore} from 'vuex'; 
	 * setup 中 const store = useStore()
	 * 
	 * @param methodBodyContent
	 * @return String
	 */
	public static String replaceThisStoreWithVuex(String methodBodyContent) {
		
		methodBodyContent = replaceStoreInfoWithVuex(methodBodyContent, "this.$store.");
		
		methodBodyContent = replaceStoreInfoWithVuex(methodBodyContent, "$store.");
		
		return methodBodyContent;
	}
	
	public static String replaceStoreInfoWithVuex(String methodBodyContent, String storeType) {
		
		if (methodBodyContent.indexOf(storeType) > -1) {
			
			addVue3ImportContent("vuex", "useStore");// 从 vuex 引入 useStore
			
			addVue3DefineContent("store", "\nconst store = useStore();\n", "define");
			
			// $无法替换，使用Matcher.quoteReplacement解决
			methodBodyContent = methodBodyContent.replaceAll(Matcher.quoteReplacement(storeType), "store.");
		}
		
		return methodBodyContent;
	}
	
	/**
	 * this.$message => ElMessage 
	 * 引入 import {ElMessage} from 'element-plus'; 
	 * 
	 * @param methodBodyContent
	 * @param addElMessage 是否添加过标志
	 * @return String
	 */
	public static String replaceThisMessageWithElMessage(String methodBodyContent, Boolean addElMessage) {
		
		String temp = "";
		String nextContent = "";
		
		int endIndex = -1;
		
		if (methodBodyContent.indexOf("this.$message") > -1) {
			
			if (!addElMessage) {
				
				addVue3ImportContent("element-plus", "ElMessage");// 从 element-plus 引入 ElMessage
				
				addElMessage = true;
			}
			
			endIndex = methodBodyContent.indexOf("this.$message");
			
			temp = methodBodyContent.substring(endIndex + "this.$message".length(), methodBodyContent.length());
					
			// options 的情况直接替换
			if ('.' != temp.charAt(0)) {
				
				nextContent = temp;
				
				methodBodyContent = methodBodyContent.substring(0, endIndex + "this.$message".length()).replace("this.$message", "ElMessage"); 
			} else {
				
				String messageType = temp.substring(1, temp.indexOf('('));
				
				endIndex = TxtContentUtil.getTagEndIndex(temp, '(', ')') + 1;
				
				temp = temp.substring(0, endIndex);
				
				String replaceMessage = "ElMessage({\nmessage:" + temp.substring(temp.indexOf('(') + 1, temp.length() - 1) + ",\ntype:'" + messageType + "'\n});";
				
				endIndex = methodBodyContent.indexOf(temp) + temp.length();
				
				nextContent = methodBodyContent.substring(endIndex, methodBodyContent.length());
				
				methodBodyContent = methodBodyContent.substring(0, endIndex);
				
				methodBodyContent = methodBodyContent.replace("this.$message", ""); 
				
				methodBodyContent = methodBodyContent.replace(temp, replaceMessage); 
			}
			
			return methodBodyContent + replaceThisMessageWithElMessage(nextContent, addElMessage);
		}
		
		return methodBodyContent;
	}
	
	/**
	 * this.$t => globalProperties.$t 
	 * 引入 import { getCurrentInstance } from "vue"; 
	 * const { appContext : { config: { globalProperties } } } = getCurrentInstance();
	 * 
	 * @param methodBodyContent
	 * @return String
	 */
	public static String replaceIn18TmethodWithGlobalT(String methodBodyContent) {
		
		if (methodBodyContent.indexOf("this.$t(") > -1 || methodBodyContent.indexOf("this.$i18n.") > -1) {
			
			addVue3ImportContent("vue", "getCurrentInstance");
			
			addVue3DefineContent("appContext", "const { appContext : { config: { globalProperties } } } = getCurrentInstance();", "define");
			
			String keyWordTxt = "";
			
			int endIndex = -1;
			
			// 两者同时存在，取索引值小的
			if (methodBodyContent.indexOf("this.$t(") > -1 && methodBodyContent.indexOf("this.$i18n.") > -1) {
				
				if (methodBodyContent.indexOf("this.$t(") > methodBodyContent.indexOf("this.$i18n.")) {
					
					keyWordTxt = "this.$i18n.";
				} else {
					
					keyWordTxt = "this.$t(";
				}
			} else if (methodBodyContent.indexOf("this.$t(") > -1) {
				
				keyWordTxt = "this.$t(";
			} else {
				
				keyWordTxt = "this.$i18n.";
			}
			
			endIndex = methodBodyContent.indexOf(keyWordTxt) + keyWordTxt.length();
			
			return methodBodyContent.substring(0, endIndex).replace(keyWordTxt, keyWordTxt.indexOf("$i18n.") > -1?"globalProperties.$i18n.":"globalProperties.$t(") + replaceIn18TmethodWithGlobalT(methodBodyContent.substring(endIndex, methodBodyContent.length()));
		}
		
		return methodBodyContent;
	}
	
	/**
	 * this.$children => this.$ref.templateRef
	 * 
	 * @param methodBodyContent
	 * @return String
	 */
	public static String replaceThisChildrenByRef(String methodBodyContent) {
		
		if (methodBodyContent.indexOf("this.$children") > -1) {
			
			int startIndex = methodBodyContent.indexOf("this.$children") + "this.$children".length();
			
			return methodBodyContent.substring(0, startIndex).replace("this.$children", "this.$refs." + templateRef) + replaceThisChildrenByRef(methodBodyContent.substring(startIndex, methodBodyContent.length()));
		}
		
		return methodBodyContent;
	}
	
	/**
	 * this.$refs => ref
	 * 引入 import { ref } from "vue"; 
	 * 
	 * @param methodBodyContent
	 * @return String
	 */
	public static String replaceRefInfoGetMethod(String methodBodyContent) {
		
		if (methodBodyContent.indexOf("this.$refs") > -1) {
			
			String temp = "";
			String originRef = "";
			String replaceRef = "";
			
			int startIndex = -1;
			int endIndex = -1;
			
			startIndex = methodBodyContent.indexOf("this.$refs") + "this.$refs".length();
			
			temp = methodBodyContent.substring(startIndex, methodBodyContent.length());
			
			// . 或者 [ 或者直接整个对象
			if ('.' == temp.charAt(0)) {
				
				originRef = "this.$refs.";
				
				startIndex += 1;
				
				endIndex = TxtContentUtil.getNotVariableIndex(temp, 1);
				
				temp = temp.substring(1, endIndex);// 得到变量名
			} else if ('[' == temp.charAt(0)) {
				
				endIndex = TxtContentUtil.getTagEndIndex(temp, '[', ']');
				
				originRef = "this.$refs" + temp.substring(0, endIndex + 1);
				
				startIndex += originRef.length() - "this.$refs".length();
				
				temp = temp.substring(temp.indexOf('[') + 1, endIndex).trim();
				
				// 字符串标志
				if (!String.valueOf(temp.charAt(0)).matches(ConvertParam.JS_VARIABLE_REG)) {
					
					temp = temp.substring(1, temp.length() - 1);
				}
				
				replaceRef = temp;
			} else {
				
				temp = templateRef;// 模板根ref
				
				replaceRef = temp;
				
				originRef = "this.$refs";
			}
			
			if (!"".equals(temp)) {
				
				if (!setUpReturnResultList.contains(temp)) {
					
					setUpReturnResultList.add(temp);
					
				}
				
				addVue3ImportContent("vue", "ref");// 从 vue 引入 ref
				
				// 此处得到的ref temp变量可能不符合变量命名要求，待处理
				addVue3DefineContent(temp, "const " + temp + " = ref(null);", "setupDefine");
			}
			
			return methodBodyContent.substring(0, startIndex).replace(originRef, replaceRef) + replaceRefInfoGetMethod(methodBodyContent.substring(startIndex, methodBodyContent.length()));
		}
		
		return methodBodyContent;
	}
	
	/**
	 * 添加定义信息部分
	 * 
	 * @param variableKey 变量名
	 * @param variableContent 具体定义内容
	 * @param defineType 定义的内容类型 
	 */
	private static void addVue3DefineContent(String variableKey, String variableContent, String defineType) {
		
		Map<String, String> defineMap;
		
		if (parseResultMap.containsKey(defineType)) {
			
			defineMap = parseResultMap.get(defineType);
		} else {
			
			defineMap= new HashMap<>();
		}
		
		if (!defineMap.containsKey(variableKey)) {
			
			defineMap.put(variableKey, variableContent);
			
			parseResultMap.put(defineType, defineMap);
		}
	}
	
	/**
	 * 添加import 信息部分
	 * 
	 * @param fromKey 从哪个库引入，库名
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
				
				// 判断原先内容中是否有from 这个库，有则替换，无则插入
				tempText = TxtContentUtil.findImportContentByKey(fileContent, entry.getKey(), 0);
				
				if ("".equals(tempText)) {
					
					vue3ImportContent += " } from '" + entry.getKey() + "'";
					
					vue3ParsePartImportContent += vue3ImportContent + ";\n";// 执行拼接，用于最后的拼接到文件内容中
				} else {
					
					String originImportContent = tempText.substring(tempText.indexOf('#') + 1, tempText.length());
					
					tempText = originImportContent;
					
					if (tempText.indexOf('{') > -1) {
						
						tempText = tempText.substring(tempText.indexOf('{') + 1, tempText.lastIndexOf('}'));
					} else {
						
						tempText = tempText.substring(tempText.indexOf("import ") + "import ".length(), tempText.lastIndexOf(" from "));
					}
					
					tempText = tempText.trim();
					
					if (',' != tempText.charAt(0)) tempText = ", " + tempText;
					
					vue3ImportContent += tempText;
					
					vue3ImportContent += " } from '" + entry.getKey() + "'";
					
					// 直接替换原来内容
					fileContent = fileContent.replace(originImportContent, vue3ImportContent);
					
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
				
				startInex = findLastImportKeyIndex(fileContent);
				
				fileContent = fileContent.substring(0, startInex + 1) + "\n\n" + vue3ParsePartDefineContent + "\n\n" + fileContent.substring(startInex + 1, fileContent.length());
				
			}
		}
		
		return fileContent;
	}
	
	private static int findLastImportKeyIndex(String fileContent) {
		
		String tempText = "";
		
		int startInex = -1;
		
		tempText = fileContent.substring(0, fileContent.lastIndexOf("import "));
		
		//css @import
		if (tempText.length() > 0 && '@' == tempText.charAt(tempText.length() - 1)) {
			
			return findLastImportKeyIndex(tempText);
		}
		
		tempText = fileContent.substring(fileContent.lastIndexOf("import "), fileContent.length());
		
		startInex = fileContent.lastIndexOf("import ") + TxtContentUtil.getStatementEndIndex(tempText, 0);
		
		return startInex;
	}
	
	private static String changeVueRouteInNewWay(String fileContent) {
		
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
		
		fileContent = replaceRouterInfoOfSetUp(fileContent, "this.$router", "this.$route");
		fileContent = replaceRouterInfoOfSetUp(fileContent, "$router", "$route");
		
		return fileContent;
	}
	
	/**
	 * 处理computed 中的set 和 get
	 * 
	 * @param methodBodyContent 方法体
	 * @param method
	 * @return String
	 */
	public static String processComputedSetOrGetFunc(String methodBodyContent, String method) {
		
		String temp = "";
		String methodContent = "";
		
		int endIndex = -1;
		
		if (methodBodyContent.indexOf(method + ":") > -1) {
			
			temp = methodBodyContent.substring(methodBodyContent.indexOf(method + ":"), methodBodyContent.length());
			
			temp = temp.substring(0, temp.indexOf("("));
			
			endIndex = TxtContentUtil.getTagEndIndex(methodBodyContent.substring(methodBodyContent.indexOf(method + ":"), methodBodyContent.length()), '{', '}') + 1;
			
			methodContent = methodBodyContent.substring(methodBodyContent.indexOf(method + ":"), methodBodyContent.indexOf(method + ":") + endIndex);// 得到整个method
			
			methodContent = methodContent.substring(methodContent.indexOf("{"), methodContent.length());
			
			getAllVariableOfMethodUse(methodBodyContent);
			
			methodBodyContent = methodBodyContent.replace(temp, method);
		}
		
		return methodBodyContent;
	}
	
	private static void getAllVariableOfMethodUse(String methodBodyContent) {
		
		String temp = "";
		
		int endIndex = -1;
		
		if (methodBodyContent.indexOf("this.") > -1) {
			
			temp = methodBodyContent.substring(methodBodyContent.indexOf("this.") + "this.".length(), methodBodyContent.length());
			
			endIndex = TxtContentUtil.getNotVariableIndex(temp, 0);
			
			// 判断不是方法调用并且stateDataResultMap中无此变量则添加
			if ('(' != temp.charAt(endIndex)) {
				
				temp = temp.substring(0, endIndex);
				
				// 非$开头 并且不是 props 同时没有存储过
				if ('$' != temp.charAt(0) && !propsResultMap.containsKey(temp.trim()) && !stateDataResultMap.containsKey(temp.trim())) {
					
					stateDataResultMap.put(temp, "");
					
					if (!setUpReturnResultList.contains("state")) setUpReturnResultList.add("state");
				}
			}
			
			getAllVariableOfMethodUse(methodBodyContent.substring(methodBodyContent.indexOf("this.") + endIndex, methodBodyContent.length()));
		}
	}
	
	/**
	 * 为模板元素添加ref 引用
	 * 
	 * @param fileContent
	 * @return String
	 */
	public static String setTemplateChildrenWithRef(String fileContent) {
		
		// 存在模板标签且存在$children 引用
		if (fileContent.indexOf("<template") > -1 && fileContent.indexOf("</template") > -1 && fileContent.indexOf("<template") < fileContent.indexOf("</template") && (fileContent.indexOf("this.$ref") > -1 || fileContent.indexOf("$children") > -1)) {
			
			int startIndex = -1;
			
			templateRef = TxtContentUtil.reNameVariable(parseFileName.substring(0, parseFileName.lastIndexOf('.'))).toLowerCase() + "Ref";
			
			startIndex = fileContent.indexOf("<template") + "<template".length();
			
			fileContent = fileContent.substring(0, startIndex) + " ref=\"" + templateRef + "\"" + fileContent.substring(startIndex, fileContent.length());
			
		}
		
		return fileContent;
	}
	
	private static String changeCssDifferentUse(String fileContent) {
		
		
		
		return fileContent;
	}
	
	private static String changeTypeScriptVersion(String fileContent) {
		
		
		
		return fileContent;
	}
	
	public static String clearVuexImportAndThisRef(String fileContent) {
		
		fileContent = VueProcessUtil.clearVuexImportMapMethodContent(fileContent);
		
		fileContent = VueProcessUtil.clearSetUpContentThisRef(fileContent);
		
		return fileContent;
	}
	
	private static String getVue3FileResultContent(String fileContent) {
		
		// import 信息处理
		fileContent = processVue3ImportContent(fileContent);
		
		// define 信息处理
		fileContent = processVue3DefineContent(fileContent);
		
		return fileContent;
	}
	
	private static String processFileContentFormat(String sourceText, String fileType, String framworkName) {
		
		return TxtContentUtil.processFileContentFormat(sourceText, fileType, framworkName);
	}
	
	private static void importCreateApp() {
		
		addVue3ImportContent("vue", "createApp");// 引入createApp
		
		addVue3DefineContent("app", "const app = createApp({});", "define");
	}
	
}
