package utils;

import java.util.Map;

public class GenerateClassFileUtil {

	public GenerateClassFileUtil() {
		
	}
	
	public static Map<String, String> fileListMap;
	
	// 封装的vuex方法用于vue3
	public static String VuexMethodContent = "\n" + 
			"import { reactive, computed } from \"vue\";\n" + 
			"import { useStore , mapState, mapGetters, createNamespacedHelpers } from \"vuex\";\n" + 
			"\n" +  
			"// 组合mapState和mapGetters\n" + 
			"function useMapper(data, mapFn) {\n" + 
			"    const store = useStore()\n" + 
			"    const storeGettersFns = mapFn(data)\n" + 
			"    const storeGetters = {}\n" + 
			"    Object.keys(storeGettersFns).forEach( fnkey => {\n" + 
			"        const fn = storeGettersFns[fnkey].bind({$store: store})\n" + 
			"        storeGetters[fnkey] = computed(fn)\n" + 
			"    }) \n" + 
			"    return storeGetters\n" + 
			"}\n" + 
			"\n" + 
			"function mapGetter(name, getters) {\n" + 
			"    let mapFn = mapGetters\n" + 
			"    if (typeof name === 'string' && name.length > 0) {\n" + 
			"        mapFn = createNamespacedHelpers(name).mapGetters\n" + 
			"    }\n" + 
			"    return useMapper(getters, mapFn)\n" + 
			"}\n" + 
			"\n" + 
			"function mapStates(name, states) {\n" + 
			"    let mapFn = mapState\n" + 
			"    if(typeof name === 'string' && name.length > 0 ) {\n" + 
			"        mapFn = createNamespacedHelpers(name).mapState\n" + 
			"    }\n" + 
			"    return useMapper(states, mapFn)\n" + 
			"}\n" + 
			"\n" + 
			"function mapMutations () {\n" + 
			"    const store = useStore()\n" + 
			"    if (arguments.length === 1) {\n" + 
			"        let mapper = arguments[0]\n" + 
			"        if (typeof mapper === \"string\" && mapper.length > 0) {\n" + 
			"            return param =>{\n" + 
			"                store.commit(mapper, param)\n" + 
			"            }\n" + 
			"        }\n" + 
			"        if (mapper instanceof Array && mapper.length > 0) {\n" + 
			"            let mappers = reactive({})\n" + 
			"            mapper.forEach(item => {\n" + 
			"                mappers[item] = mapMutations(item)\n" + 
			"            })\n" + 
			"            return mappers\n" + 
			"        }\n" + 
			"    }\n" + 
			"    if (arguments.length === 2) {\n" + 
			"        const moduleName = arguments[0]\n" + 
			"        const mapper = arguments[1]\n" + 
			"        if (typeof moduleName !== \"string\") {\n" + 
			"            console.error(\"传入的moduleName类型或格式错误！\")\n" + 
			"            return null\n" + 
			"        }\n" + 
			"        if (typeof mapper === \"string\" && mapper.length > 0) {\n" + 
			"            let mapperName = moduleName + \"/\" + mapper\n" + 
			"            return mapMutations(mapperName)\n" + 
			"        }\n" + 
			"        if (mapper instanceof Array && mapper.length > 0) {\n" + 
			"            let mappers = reactive({})\n" + 
			"            mapper.forEach(item => {\n" + 
			"                mappers[item] = mapMutations(moduleName, item)\n" + 
			"            })\n" + 
			"            return mappers\n" + 
			"        }\n" + 
			"    }\n" + 
			"    console.error(\"使用方法有误，请检查输入参数的格式！\")\n" + 
			"    return null\n" + 
			"}\n" + 
			"\n" + 
			"function mapActions () {\n" + 
			"    const store = useStore()\n" + 
			"    if (arguments.length === 1) {\n" + 
			"        let mapper = arguments[0]\n" + 
			"        if (typeof mapper === \"string\" && mapper.length > 0) {\n" + 
			"            return param =>{\n" + 
			"                return new Promise((resolve, reject) => {\n" + 
			"                    store.dispatch(mapper, param)\n" + 
			"                        .then(res => resolve(res)).catch(err => reject(err))\n" + 
			"                })\n" + 
			"            }\n" + 
			"        }\n" + 
			"        if (mapper instanceof Array && mapper.length > 0) {\n" + 
			"            let mappers = reactive({})\n" + 
			"            mapper.forEach(item => {\n" + 
			"                mappers[item] = mapActions(item)\n" + 
			"            })\n" + 
			"            return mappers\n" + 
			"        }\n" + 
			"    }\n" + 
			"    console.error(\"使用方法有误，请检查输入参数的格式！\")\n" + 
			"    return null\n" + 
			"}\n" +
			"\n" + 
			"export { mapStates, mapGetter, mapMutations, mapActions }\n" + 
			"\n";
}
