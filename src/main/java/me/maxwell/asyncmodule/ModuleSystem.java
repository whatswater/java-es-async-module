package me.maxwell.asyncmodule;

import java.util.*;

public class ModuleSystem {
    public ModuleFactory load(String moduleClassName, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        return load(moduleClassName, ModuleFactory.DEFAULT_VERSION, config);
    }

    private ClassLoaderFactory classLoaderFactory;
    private ModuleFactory moduleFactory;

    @SuppressWarnings("unchecked")
    public ModuleFactory load(String moduleClassName, String version, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        ClassLoaderFactory classLoaderFactory = new ClassLoaderFactory(config, Module.class.getClassLoader());
        Class<?> cls = classLoaderFactory.loadClass(moduleClassName, version);
        if(!Module.class.isAssignableFrom(cls)) {
            throw new ModuleSystemException("The cls: " + cls.getName() + " must implements Module Interface when loading");
        }

        Class<? extends Module> moduleClass = (Class<? extends Module>) cls;
        ModuleFactory moduleFactory = new ModuleFactory(new ModuleLoadedListener() {
            @Override
            public void onModuleLoaded(ModuleInfo moduleInfo, ModuleFactory factory) {
                System.out.println(moduleInfo.getModuleName() + " loaded");
            }

            @Override
            public void onAllModuleLoaded(ModuleFactory factory) {
                System.out.println("moduleFactory loaded");
            }
        });
        moduleFactory.getModuleInfo(moduleClass);

        this.classLoaderFactory = classLoaderFactory;
        this.moduleFactory = moduleFactory;

        return moduleFactory;
    }

    @SuppressWarnings("unchecked")
    public void reloadModule(String moduleClassName, String version, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        if(!moduleFactory.isLoaded()) {
            return;
        }

        ModuleClassLoader loader = classLoaderFactory.find(moduleClassName, version);
        Set<ModuleClassLoader> reloads = getReloadModules(loader);
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
                if(factory instanceof ModuleFactoryChain) {
                    ((ModuleFactoryChain) factory).merge();
                }
                System.out.println("12312312");
            }
        });

        for(String moduleName: moduleNames) {
            String thisVersion = moduleName.split(ModuleFactory.VERSION_SPLIT)[0];
            String className = moduleName.split(ModuleFactory.VERSION_SPLIT)[1];
            Class<? extends Module> moduleClass = (Class<? extends Module>)newFinder.loadClass(className, thisVersion);
            newFactory.getModuleInfo(moduleClass);
        }
    }

    public ModuleFactory getModuleFactory() {
        return this.moduleFactory;
    }

    public ClassLoaderFactory getClassLoaderFactory() {
        return this.classLoaderFactory;
    }

    private Set<ModuleClassLoader> getReloadModules(ModuleClassLoader loader) {
        Set<ModuleClassLoader> reloads = new TreeSet<>();
        Stack<Iterator<ModuleClassLoader>> stack = new Stack<>();
        reloads.add(loader);
        if(!loader.getReloadListeners().isEmpty()) {
            stack.push(loader.getReloadListeners().iterator());
            while(true) {
                Iterator<ModuleClassLoader> it = stack.lastElement();
                if(it.hasNext()) {
                    ModuleClassLoader loader1 = it.next();
                    reloads.add(loader1);
                    if(loader1.getReloadListeners() == null || loader1.getReloadListeners().isEmpty()) {
                        continue;
                    }
                    stack.push(loader1.getReloadListeners().iterator());
                }
                else if(stack.size() > 1) {
                    stack.pop();
                }
                else {
                    break;
                }
            }
        }
        return reloads;
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
