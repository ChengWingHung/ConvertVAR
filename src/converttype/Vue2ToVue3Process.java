package converttype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import common.ConvertParam;

/**
 * 
 * @author zhengronghong
 * 
 * vue2升级vue3处理类
 *
 */

public class Vue2ToVue3Process {

	public Vue2ToVue3Process() {
		
	}
	
	private static Map<String, Object> parseResultMap;// 解析后的信息存储对象
	
	public static String parseVue2FileContent(String fileContent) {
		
		parseResultMap = new HashMap<>();
		
		System.out.println(fileContent);
		
		String parseResultContent = "";
		
		// options API -> composition API
		parseResultContent = changeOptionApiToCompositionApi(fileContent);
			
		// 生命周期差异
		parseResultContent = changeComponentLifeycle(parseResultContent);
		
		// 组件属性及全局API变化
		parseResultContent = changeComponentPropertys(parseResultContent);
		
		// 路由以及状态管理器
		parseResultContent = changeVueRoute(parseResultContent);
		
		// TypeScript 版本
		parseResultContent = changeTypeScriptVersion(parseResultContent);
		
		// 处理最终合并的结果
		parseResultContent = getVue3FileResultContent(parseResultContent);
		
		
		return parseResultContent;
	}
	
	private static String changeOptionApiToCompositionApi(String fileContent) {
		
		// new Vue 形式
		
		
		// export default 形式
		
		
		return fileContent;
	}
	
	private static String changeComponentLifeycle(String fileContent) {
		
		
		
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
		
		ArrayList<String> importList = new ArrayList<String>();
		ArrayList<String> defineList = new ArrayList<String>();
		
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
				importList.add(vue3import + ";");
			}
			
			if (vue3define != "") {
				defineList.add(vue3define + ";");
			}
			
		}
		
		if (importList.size() > 0) {
			parseResultMap.put("import", importList);
		}
		
		if (defineList.size() > 0) {
			parseResultMap.put("define", defineList);
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
		
		
		
		return fileContent;
	}
}
