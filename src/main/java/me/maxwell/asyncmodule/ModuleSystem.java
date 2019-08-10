package me.maxwell.asyncmodule;

import java.util.Map;

public class ModuleSystem {
    public static ModuleFactory load(String modulePath, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        return load(modulePath, ModuleFactory.DEFAULT_VERSION, config);
    }

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

        return factory;
    }
}
