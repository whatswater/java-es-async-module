package me.maxwell.asyncmodule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassLoaderFactory {
    private final Map<String, ModuleClassLoader> classLoaderHolder;
    private Map<String, ClassLoaderBuilder> config;
    private final ClassLoader parent;

    public ClassLoaderFactory(
            Map<String, ClassLoaderBuilder> config,
            ClassLoader parent
    ) {
        this.config = config;
        this.classLoaderHolder = new ConcurrentHashMap<>();
        this.parent = parent;
    }

    public ClassLoaderBuilder getBuilder(String name) {
        Map<String, ClassLoaderBuilder> config = getConfig();
        return config.get(name);
    }

    public ModuleClassLoader findExact(String configName) {
        ClassLoaderBuilder builder = getBuilder(configName);
        if(builder == null) {
            return null;
        }

        String classLoaderName = builder.getCacheKey(configName);
        CreateLoaderFunction function = new CreateLoaderFunction(this, builder);
        return classLoaderHolder.computeIfAbsent(classLoaderName, function);
    }

    public ModuleClassLoader find(String className, String version) {
        int lastIndex;
        String tmp = version + ModuleFactory.VERSION_SPLIT + className;
        ModuleClassLoader classLoader;

        do {
            classLoader = findExact(tmp);
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

    public ClassLoader getParent() {
        return parent;
    }

    public Map<String, ClassLoaderBuilder> getConfig() {
        return config;
    }

    public Map<String, ModuleClassLoader> getAllModuleClassLoader() {
        return this.classLoaderHolder;
    }

    public ModuleClassLoader getModuleClassLoader(String name) {
        return classLoaderHolder.get(name);
    }
}
