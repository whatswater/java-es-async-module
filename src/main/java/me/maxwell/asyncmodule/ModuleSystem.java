package me.maxwell.asyncmodule;

import java.util.Map;

public class ModuleSystem {
    public static ModuleFactory load(String modulePath, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        return load(modulePath, ModuleFactory.DEFAULT_VERSION, config);
    }

    private static ClassLoaderFinder finder;
    private static ModuleFactory factory;

    public static ModuleFactory load(String modulePath, String version, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        ClassLoaderFinder finder = new ClassLoaderFinder(config, Module.class.getClassLoader());
        ModuleClassLoader loader = finder.find(modulePath, version);
        if(loader == null) {
            throw new ModuleSystemException("ClassLoaderFinder can not find module's classLoader," + " modulePath: " + modulePath + ", version: " + version);
        }

        Class<?> cls = loader.findClass(modulePath);
        if(!Module.class.isAssignableFrom(cls)) {
            throw new ModuleSystemException("The cls: " + cls.getName() + " must implements Module Interface when loading");
        }

        Class<Module> moduleClass = (Class<Module>) cls;
        ModuleFactory factory = new ModuleFactory();
        factory.getModuleInfo(moduleClass);
        ModuleSystem.finder = finder;
        ModuleSystem.factory = factory;

        return factory;
    }

    // 实现重新加载功能
    // 如果类没有任何变化，那么实现重新加载就只是重新注册模块罢了
    // 但是如果类发生了变化，那么就很复杂了
    // 一个模块所引用的类，有很多都不是此模块自己的类加载器加载的
    // 所以类加载器需要记录依赖关系，当某个类加载器重新加载后，重新实例化新的类加载器加载其他的类
    // 某些类加载器加载的类中有Module的实现类，这个时候还需要重新创建模块
    // 系统需要提供两个命令，一个是重新加载某个模块或者多个模块，另一个是重新加载某个package
    public static void reloadModule(String modulePath, String version, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        ModuleClassLoader loader = finder.find(modulePath, version);
        Class<?> cls = loader.findClass(modulePath);
        Class<Module> moduleClass = (Class<Module>) cls;

        ModuleInfo moduleInfo = factory.getModuleInfo(moduleClass);
    }
}
