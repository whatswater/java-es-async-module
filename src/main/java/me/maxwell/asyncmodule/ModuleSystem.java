package me.maxwell.asyncmodule;

import java.util.*;

public class ModuleSystem {
    private ClassLoaderFactory classLoaderFactory;
    private ModuleFactory moduleFactory;

    public ModuleSystem(Map<String, ClassLoaderBuilder> config) {
        this(config, null);
    }

    public ModuleSystem(Map<String, ClassLoaderBuilder> config, ModuleLoadedListener listener) {
        /*this.classLoaderFactory = new ClassLoaderFactory(config);
        this.moduleFactory = new ModuleFactory(this.classLoaderFactory, listener);*/
    }

    /**
     * 添加类加载器
     */
    public void addClassLoader(String packageName, ModuleClassLoader classLoader) {
    }

    /**
     *移除类加载器
     */
    public void removeClassLoader() {

    }

    public void loadModule(String moduleClassName) throws ClassNotFoundException {
        loadModule(moduleClassName, ModuleFactory.DEFAULT_VERSION);
    }

    @SuppressWarnings("unchecked")
    public void loadModule(String moduleClassName, String version) throws ClassNotFoundException {
        Class<?> cls = classLoaderFactory.loadClass(moduleClassName, version);
        if(!Module.class.isAssignableFrom(cls)) {
            throw new ModuleSystemException("The cls: " + cls.getName() + " must implements Module Interface when loading");
        }

        Class<? extends Module> moduleClass = (Class<? extends Module>) cls;
        moduleFactory.getModuleInfo(moduleClass);
    }

    @SuppressWarnings("unchecked")
    public void reloadModule(String moduleClassName, String version, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        /*if(!moduleFactory.isLoaded()) {
            return;
        }

        ModuleClassLoader loader = classLoaderFactory.find(moduleClassName, version);
        Set<ModuleClassLoader> reloads = loader.getReloadModules();
        List<String> moduleNames = getModuleNames(reloads);

        ClassLoaderFactoryChain newFinder = new ClassLoaderFactoryChain(config, classLoaderFactory, reloads);
        ModuleFactoryChain newFactory = new ModuleFactoryChain(moduleFactory, newFinder, new ModuleLoadedListener() {
            @Override
            public void onModuleLoaded(ModuleInfo moduleInfo, ModuleFactory factory) {
                System.out.println(moduleInfo.getModuleName() + " loaded");
            }

            @Override
            public void onAllModuleLoaded(ModuleFactory factory) {
                System.out.println("moduleFactory loaded");
                newFinder.merge();
                for(ModuleClassLoader classLoader: reloads) {
                    classLoader.unListen(reloads);
                }
                if(factory instanceof ModuleFactoryChain) {
                    ((ModuleFactoryChain) factory).merge();
                }
            }
        });

        for(String moduleName: moduleNames) {
            String thisVersion = moduleName.split(ModuleFactory.VERSION_SPLIT)[0];
            String className = moduleName.split(ModuleFactory.VERSION_SPLIT)[1];
            Class<? extends Module> moduleClass = (Class<? extends Module>)newFinder.loadClass(className, thisVersion);
            newFactory.getModuleInfo(moduleClass);
        }*/
    }

    public ModuleFactory getModuleFactory() {
        return this.moduleFactory;
    }
    public ClassLoaderFactory getClassLoaderFactory() {
        return this.classLoaderFactory;
    }

    private List<String> getModuleNames(Set<ModuleClassLoader> reloads) {
        List<String> moduleNames = new ArrayList<>();
        for(ModuleClassLoader classLoader: reloads) {
            for(Class<? extends Module> moduleClass: classLoader.getModuleClassList()) {
                moduleNames.add(moduleFactory.getModuleName(moduleClass));
            }
        }
        return moduleNames;
    }
}
