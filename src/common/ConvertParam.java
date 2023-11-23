package common;

public class ConvertParam {

	public ConvertParam() {
		
	}
	
	// 工具名
	public static String TOOL_NAME = "ConvertVAR";
		
	// 版本号信息
	public static String TOOL_VERSION = "1.0.0 Beta";
	
	// 当前环境标志信息，true 为本地开发测试
	public static Boolean IS_DEV_FLAG = true;
	
	// ConvertVAR 建议
	public static String RECOMMEND_BY_CONVERTVAR = " => by ConvertVAR";
	
	// 本地测试输出文件夹
	public static String LOCAL_TEST_OUTPUT_FILE_PATH = "";
		
	// vue2 => vue3实际输出文件夹
	public static String VUE3_OUTPUT_FILE_PATH = "convertResult_vue3";
	
	// react class => function实际输出文件夹
	public static String REAT_OUTPUT_FILE_PATH = "convertResult_react";
		
	// 转换字符标志
	public static String CONVERT_STRING = "->";
	
	// import新的内容标志
	public static char IMPORT_STRING = '#';
	
	// import新的内容
	public static String IMPORT_FLG = "import";
	
	// 定义新的变量标志
	public static char DEFINE_STRING = '^';
	
	// 需要记录值的key
	public static String RECORD_PROPERTY_NAME = "el,render,mixins";
	
	// 定义变量命名规则
	public static String JS_VARIABLE_REG = "^[A-Za-z0-9_\\$]";
	
	// Vue2ToVue3 option api function part -> setup 方法
	public static String[] Vue2ToVue3SetUpMethodList = {
			"filters", "computed", "watch", "methods"
	};
	
	// Vue2ToVue3 全局api对照
	public static String[] Vue2ToVue3GlobalApiList = {
			"Vue.config.productionTip", "Vue.extend",
			"Vue.config->app.config", "Vue.config.ignoredElements->app.config.compilerOptions.isCustomElement",
			"Vue.component->app.component", "Vue.directive->app.directive", "Vue.mixin->app.mixin",
			"Vue.use->app.use", "Vue.prototype->app.config.globalProperties",
			"config.ignoredElements->config.isCustomElement"
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
			"inline-template",
			"v-bind=\"@->v-bind=\"","v-on:->@",
			"this.$scopedSlots->this.$slots","slot-scope->v-slot"
	};
	
	// Vue2ToVue3 路由转换对照
	public static String[] Vue2ToVue3RouteList = {
			"router-link->RouterLink","v-bind=“route\"->:to=\"route\"","$router.push->router.push",
			"store.subscribe->store.watch"
	};
	
	// Vue2ToVue3 需要移除的实例
	public static String[] clearVue2InstanceList = {
			"$children->$refs", // $children 实例，如果需要访问子组件实例可用 $refs
			"$destroy" // 移除 $destroy 实例，不应该手动管理单个 Vue 组件的生命周期
	};
	
	// Vue2ToVue3 键盘码值转换为修饰符
	public static String[] keyCodeToKecharList = {
			"8->backspace","9->tab","12->clear","13->enter","16->shift",
			"17->control","18->alt","20->capelock","27->esc","32->spacebar",
			"33->pageup","34->pagedown","35->end","36->home","37->left",
			"38->up","39->right","40->down","45->insert","46->delete",
			"144->numlock"
	};
	
	// react class组件执行涉及的函数，如果是二次封装在此处修改为对应的函数名
	public static String[] ReactClassLifeMethodList = {
			"constructor",
			"componentWillmount","componentDidMount","componentWillUnmount",
			"componentWillReceiveProps",
			"componentDidUpdate","shouldComponentUpdate",
			"componentDidCatch",
			"render", "state"
	};
	
}
