package me.maxwell.asyncmodule;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * 1、ClassLoaderFactory的配置和NameGroup类似，children属性换成map并添加classLoader属性
 * 2、类加载器应当有一个重新实例化自己的方法
 * 3、重新实例化类加载器的时候，系统需要知道原先的类加载器中包含的模块被哪些地方依赖了，然后重新调用依赖
 * 4、事务（实现事务很困难，估计需要添加事务环境对象）
 */
public class ClassLoaderFactory {
    private Map<String, ModuleClassLoader> config;

    public ClassLoaderFactory(Map<String, ModuleClassLoader> config) {
        this.config = config;
    }

    /**
     * 根据packageName找到之前ClassLoaderFactory中可能已经加载此类的其他loader，然后remove掉这个classLoader
     * 然后创建一个新得loader放到map中。然后重新加载模块
     */
    public void addClassLoader(String packageName, String version, ModuleClassLoader moduleClassLoader) {
        ModuleClassLoader origin = find(packageName, version);
        if(origin == null) {
            this.config.put(getConfigKey(packageName, version), moduleClassLoader);
        }
        else {
            // how to instance classLoader? moduleClassLoader应该有一个复制方法
            // 怎么确认origin的key(由于一个classLoader示例可能有多个key，所以需要遍历config中所有的元素)
        }
    }

    // 逻辑，获取依赖此classLoader的其他loader，并且找到对应的module，卸载module，然后删除这些loader
    public void removeClassLoader(ModuleClassLoader moduleClassLoader) {
        List<Class<? extends Module>> moduleClassList = moduleClassLoader.getModuleClassList();
        // TODO 卸载模块

        List<String> keys = new LinkedList<>();
        for(Map.Entry<String, ModuleClassLoader> entry: config.entrySet()) {
            if(entry.getValue() == moduleClassLoader) {
                keys.add(entry.getKey());
            }
        }
        for(String key: keys) {
            config.remove(key);
        }
    }

    public ModuleClassLoader find(String className, String version) {
        int lastIndex;
        String tmp = getConfigKey(className, version);
        ModuleClassLoader classLoader;

        do {
            classLoader = config.get(tmp);
            if(classLoader != null) {
                break;
            }
            lastIndex = tmp.lastIndexOf(".");
            if(lastIndex > 0) {
                tmp = tmp.substring(0, lastIndex);
            }
        }
        while(lastIndex > 0);
        return classLoader;
    }

    public Class<?> loadClass(String className, String version) throws ClassNotFoundException {
        ModuleClassLoader classLoader = find(className, version);
        if(classLoader == null) {
            throw new ModuleSystemException("ClassLoaderFactory can not find module's classLoader, className: " + className + ", version: " + version);
        }
        return classLoader.findClass(className);
    }

    private String getConfigKey(String packageName, String version) {
        return version + ModuleFactory.VERSION_SPLIT + packageName;
    }
}
