package me.maxwell.asyncmodule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassLoaderFinder {
    private Map<String, ModuleClassLoader> cache;
    private Map<String, ClassLoaderBuilder> config;
    private final ClassLoader parent;

    public ClassLoaderFinder(Map<String, ClassLoaderBuilder> config, ClassLoader parent) {
        this.config = config;
        this.cache = new ConcurrentHashMap<>();
        this.parent = parent;
    }

    public ModuleClassLoader find(String name, String version) {
        int lastIndex;
        String tmp = name;

        do {
            String key = tmp + ":" + version;
            if(config.containsKey(key)) {
                ClassLoaderBuilder builder = config.get(key);
                String cacheKey = builder.getCacheKey(key);

                ModuleClassLoader classLoader = cache.get(cacheKey);
                if(classLoader == null) {
                    classLoader = builder.createClassLoader(this, parent, key);
                    cache.put(cacheKey, classLoader);
                }
                return classLoader;
            }
            lastIndex = tmp.lastIndexOf(".");
            if(lastIndex > 0) {
                tmp = tmp.substring(0, lastIndex);
            }
        }
        while(lastIndex > 0);
        return null;
    }

    public Map<String, ClassLoaderBuilder> getConfig() {
        return config;
    }
}
