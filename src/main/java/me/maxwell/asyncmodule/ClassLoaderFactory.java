package me.maxwell.asyncmodule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassLoaderFactory {
    private final Map<String, ModuleClassLoader> classLoaderHolder;
    private final Map<String, Object> lockMap;
    private Map<String, ClassLoaderBuilder> config;
    private final ClassLoader parent;

    public ClassLoaderFactory(
            Map<String, ClassLoaderBuilder> config,
            ClassLoader parent
    ) {
        this.config = config;
        this.classLoaderHolder = new ConcurrentHashMap<>();
        this.lockMap = new ConcurrentHashMap<>();
        this.parent = parent;
    }

    public ClassLoaderBuilder getBuilder(String name) {
        Map<String, ClassLoaderBuilder> config = getConfig();
        if(!config.containsKey(name)) {
            return null;
        }
        return config.get(name);
    }

    public ModuleClassLoader findExact(String name) {
        ClassLoaderBuilder builder = getBuilder(name);
        if(builder == null) {
            return null;
        }

        String cacheKey = builder.getCacheKey(name);
        ModuleClassLoader classLoader = getModuleClassLoader(cacheKey);
        if(classLoader == null) {
            synchronized(getLock(cacheKey)) {
                classLoader = getModuleClassLoader(cacheKey);
                if(classLoader == null) {
                    classLoader = builder.createClassLoader(this, parent, cacheKey);
                    putModuleClassLoader(cacheKey, classLoader);
                }
            }
        }
        return classLoader;
    }

    public ModuleClassLoader find(String name) {
        int lastIndex;
        String tmp = name;
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

    public ClassLoader getParent() {
        return parent;
    }

    public Map<String, ClassLoaderBuilder> getConfig() {
        return config;
    }

    public ModuleClassLoader getModuleClassLoader(String cacheKey) {
        return classLoaderHolder.get(cacheKey);
    }

    public void putModuleClassLoader(String cacheKey, ModuleClassLoader classLoader) {
        classLoaderHolder.put(cacheKey, classLoader);
    }

    public Map<String, ModuleClassLoader> getAllModuleClassLoader() {
        return this.classLoaderHolder;
    }

    protected Object getLock(String modulePath) {
        Object newLock = new Object();
        Object lock = lockMap.putIfAbsent(modulePath, newLock);
        if (lock == null) {
            lock = newLock;
        }
        return lock;
    }
}
