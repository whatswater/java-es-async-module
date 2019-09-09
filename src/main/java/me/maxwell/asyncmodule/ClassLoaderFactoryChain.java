package me.maxwell.asyncmodule;

import java.util.*;


public class ClassLoaderFactoryChain extends ClassLoaderFactory {
    private ClassLoaderFactory next;
    private Set<ModuleClassLoader> reloads;

    public ClassLoaderFactoryChain(
            Map<String, ClassLoaderBuilder> config,
            ClassLoaderFactory next,
            Set<ModuleClassLoader> reloads
    ) {
        super(config);
        this.next = next;
        this.reloads = reloads;
    }

    @Override
    public ClassLoaderBuilder getBuilder(String name) {
        Map<String, ClassLoaderBuilder> config = getConfig();
        ClassLoaderBuilder builder = config.get(name);
        if(builder == null) {
            builder = next.getConfig().get(name);
        }
        return builder;
    }

    /**
     * 此方法的基本假设
     * 1、cacheKey相同的ModuleClassLoader是等效的
     * 2、通过version+className查找ModuleClassLoaderBuilder时，
     *    先根据全名在本classLoader查找，如果没有找到然后再next中查找；
     *    如果还没有找到，那么会根据上一级packageName查找；
     * 3、找到classLoaderBuilder后，获取cacheKey，然后去next查询，看看next有没有已经实例化好的ClassLoader
     * 4、如果next实例化好的ClassLoader在重新加载的范围内，那么会重新builder一个
     * 5、所有新创建的classLoader实例都放在本classLoaderFactory中
     * 6、当在重新加载范围内的classLoader重新加载时，由于假设一，
     *    无论builder来自于哪一个工厂，都是等效的
     * 7、如果next实例化好的ClassLoader不在重新加载的范围内，那么会直接使用next中实例化好的classLoader实例
     * 8、合并的时候直接合并config，然后将holder和合并即可。
     * 9、只有执行了find的类找到的类加载器才能生效，所以本工厂中的config可能完全不会生效，但是合并的时候还是会将所有的config和合并到next
     * @param name version + 类名
     * @return
     */
    @Override
    public ModuleClassLoader findExact(String name) {
        ClassLoaderBuilder builder = getBuilder(name);
        if(builder == null) {
            return null;
        }

        String cacheKey = builder.getCacheKey(name);
        ModuleClassLoader moduleClassLoader = next.getModuleClassLoader(cacheKey);
        if(moduleClassLoader == null || reloads.contains(moduleClassLoader)) {
            CreateLoaderFunction function = new CreateLoaderFunction(this, builder);
            return getAllModuleClassLoader().computeIfAbsent(cacheKey, function);
        }
        return moduleClassLoader;
    }

    @Override
    public ModuleClassLoader getModuleClassLoader(String name) {
        ModuleClassLoader classLoader = getAllModuleClassLoader().get(name);
        if(classLoader == null) {
            classLoader = next.getModuleClassLoader(name);
        }
        return classLoader;
    }

    public boolean isNewLoad(ModuleClassLoader moduleClassLoader) {
        return getAllModuleClassLoader().containsValue(moduleClassLoader);
    }

    public void merge() {
        Map<String, ClassLoaderBuilder> newConfig = super.getConfig();
        Map<String, ClassLoaderBuilder> oldConfig = next.getConfig();
        oldConfig.putAll(newConfig);

        Map<String, ModuleClassLoader> moduleClassLoaders = getAllModuleClassLoader();
        for(Map.Entry<String, ModuleClassLoader> entry: moduleClassLoaders.entrySet()) {
            ModuleClassLoader classLoader = entry.getValue();
            classLoader.resetFactory(next);
        }
        next.getAllModuleClassLoader().putAll(moduleClassLoaders);
    }
}