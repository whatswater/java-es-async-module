package me.maxwell.asyncmodule;

import java.util.*;

public class ClassLoaderFactoryChain extends ClassLoaderFactory {
    private ClassLoaderFactory next;
    private Set<ModuleClassLoader> reloads;

    public ClassLoaderFactoryChain(
            Map<String, ClassLoaderBuilder> config,
            ClassLoader parent,
            ClassLoaderFactory next,
            Set<ModuleClassLoader> reloads
    ) {
        super(config, parent);
        this.next = next;
        this.reloads = reloads;
    }

    public ClassLoaderFactoryChain(
            Map<String, ClassLoaderBuilder> config,
            ClassLoaderFactory next,
            Set<ModuleClassLoader> reloads
    ) {
        this(config, next.getParent(), next, reloads);
    }

    public void reBuildClassLoaders() {
        Map<String, ClassLoaderBuilder> newConfig = new TreeMap<>();
        Map<String, BuilderAndConfigNames> cacheKey2Builder = new TreeMap<>();
        Map<String, ClassLoaderBuilder> config;

        config = next.getConfig();
        for(Map.Entry<String, ClassLoaderBuilder> entry: config.entrySet()) {
            ClassLoaderBuilder builder = entry.getValue();
            String cacheKey = builder.getCacheKey(entry.getKey());

            BuilderAndConfigNames builderAndConfigNames = cacheKey2Builder.get(cacheKey);
            if(builderAndConfigNames == null) {
                builderAndConfigNames = new BuilderAndConfigNames();
                builderAndConfigNames.builder = builder;
                builderAndConfigNames.configNames = new ArrayList<>();
                cacheKey2Builder.put(cacheKey, builderAndConfigNames);
            }
            builderAndConfigNames.configNames.add(entry.getKey());
        }

        config = getConfig();
        for(Map.Entry<String, ClassLoaderBuilder> entry: config.entrySet()) {
            ClassLoaderBuilder builder = entry.getValue();
            String cacheKey = builder.getCacheKey(entry.getKey());
            BuilderAndConfigNames builderAndConfigNames = cacheKey2Builder.get(cacheKey);
            if(builderAndConfigNames != null) {
                for(String configName: builderAndConfigNames.configNames) {
                    if(!config.containsKey(configName)) {
                        newConfig.put(configName, builder);
                    }
                }
            }

            builderAndConfigNames = new BuilderAndConfigNames();
            builderAndConfigNames.builder = builder;
            cacheKey2Builder.put(cacheKey, builderAndConfigNames);
        }
        config.putAll(newConfig);

        for(ModuleClassLoader classLoader: reloads) {
            String cacheKey = classLoader.getName();
            ClassLoaderBuilder builder = cacheKey2Builder.get(cacheKey).builder;
            ModuleClassLoader newClassLoader = builder.createClassLoader(this, getParent(), cacheKey);
            putModuleClassLoader(cacheKey, newClassLoader);
        }
    }

    @Override
    public ClassLoaderBuilder getBuilder(String name) {
        Map<String, ClassLoaderBuilder> config = getConfig();
        if(!config.containsKey(name)) {
            config = next.getConfig();
            if(!config.containsKey(name)) {
                return null;
            }
        }

        return config.get(name);
    }

    @Override
    public ModuleClassLoader find(String name) {
        ModuleClassLoader oldClassLoader = next.find(name);

        if(oldClassLoader != null) {
            if(!reloads.contains(oldClassLoader)) {
                return oldClassLoader;
            }
        }
        return super.find(name);
    }

    public void merge() {
        Map<String, ClassLoaderBuilder> newConfig = super.getConfig();
        Map<String, ClassLoaderBuilder> oldConfig = next.getConfig();
        oldConfig.putAll(newConfig);

        Map<String, ModuleClassLoader> moduleClassLoaders = getAllModuleClassLoader();
        for(Map.Entry<String, ModuleClassLoader> entry: moduleClassLoaders.entrySet()) {
            ModuleClassLoader classLoader = entry.getValue();
            classLoader.setFinder(next);
            next.putModuleClassLoader(entry.getKey(), classLoader);
        }
    }
}