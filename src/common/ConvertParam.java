package common;

public class ConvertParam {

	public ConvertParam() {
		
	}
	
	// 转换字符标志
	public static String CONVERT_STRING = "->";
	
	// import新的内容标志
	public static String IMPORT_STRING = "#";
	
	// 定义新的变量标志
	public static String DEFINE_STRING = "^";
	
	// Vue2ToVue3 属性转换对照
	public static String[] Vue2ToVue3PropertyList = {
			"v-bind=\"@->v-bind=\"","v-on:->@","Vue.filter->app.config.globalProperties.$filter",
			"Vue.directive->app.directive#import { createApp } from 'vue'^const app = createApp(App)",
			"Vue.mixin->app.mixin"
			};
	
	// Vue2ToVue3路由转换对照
	public static String[] Vue2ToVue3RouteList = {
			"router-link->RouterLink","v-bind=“route\"->:to=\"route\"","$router.push->router.push",
			"store.subscribe->store.watch"
	};
}
