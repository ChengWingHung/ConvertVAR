package common;

public class ConvertParam {

	public ConvertParam() {
		
	}
	
	// 转换字符标志
	public static String CONVERT_STRING = "->";
	
	// import新的内容标志
	public static String IMPORT_STRING = "#";
	
	// import新的内容标志
	public static String IMPORT_FLG = "import";
	
	// 定义新的变量标志
	public static String DEFINE_STRING = "^";
	
	// Vue2ToVue3 option api -> setup 方法
	public static String[] Vue2ToVue3SetUpMethodList = {
			"filters", "computed", "watch", "methods"
	};
	
	// Vue2ToVue3 生命周期转换对照，无箭头转换的直接删除
	public static String[] Vue2ToVue3LiftcycleList = {
			"beforeCreate","created",
			"beforeMount->onBeforeMount","mounted->onMounted","beforeUpdate->onBeforeUpdate",
			"updated->onUpdated","deactivated->onDeactivated","activated->onActivated",
			"beforeDestroy->onBeforeUnmount","destroyed->onUnmounted","errorCaptured->onErrorCaptured"
	};
	
	// Vue2ToVue3 属性转换对照
	public static String[] Vue2ToVue3PropertyList = {
			"v-bind=\"@->v-bind=\"","v-on:->@","Vue.filter->app.config.globalProperties.$filter",
			"Vue.directive->app.directive#import { createApp } from 'vue'^const app = createApp(App)",
			"Vue.mixin->app.mixin"
	};
	
	// Vue2ToVue3 路由转换对照
	public static String[] Vue2ToVue3RouteList = {
			"router-link->RouterLink","v-bind=“route\"->:to=\"route\"","$router.push->router.push",
			"store.subscribe->store.watch"
	};
	
	
}
