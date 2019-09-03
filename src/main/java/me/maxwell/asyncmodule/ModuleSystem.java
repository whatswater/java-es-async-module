package me.maxwell.asyncmodule;

import java.util.*;
import java.util.function.Function;

public class ModuleSystem {
    public ModuleFactory load(String modulePath, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        return load(modulePath, ModuleFactory.DEFAULT_VERSION, config);
    }

    private ClassLoaderFactory classLoaderFactory;
    private ModuleFactory moduleFactory;

    public ModuleFactory load(String className, String version, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        ClassLoaderFactory classLoaderFactory = new ClassLoaderFactory(config, Module.class.getClassLoader());
        String loaderName = version + ModuleFactory.VERSION_SPLIT + className;
        Class<?> cls = classLoaderFactory.loadClass(loaderName, className);
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

    // 实现重新加载功能
    // 如果类没有任何变化，那么实现重新加载就只是重新注册模块罢了
    // 但是如果类发生了变化，那么就很复杂了
    // 一个模块所引用的类，有很多都不是此模块自己的类加载器加载的
    // 所以类加载器需要记录依赖关系，当某个类加载器重新加载后，重新实例化新的类加载器加载其他的类
    // 某些类加载器加载的类中有Module的实现类，这个时候还需要重新创建模块
    // 系统需要提供两个命令，一个是重新加载某个模块或者多个模块，另一个是重新加载某个package
    public void reloadModule(String modulePath, String version, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        if(!moduleFactory.isLoaded()) {
            return;
        }

        ModuleClassLoader loader = classLoaderFactory.find(version + ModuleFactory.VERSION_SPLIT + modulePath);
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

        ClassLoaderFactoryChain newFinder = new ClassLoaderFactoryChain(config, classLoaderFactory, reloads);
        newFinder.reBuildClassLoaders();
        Map<String, List<String>> names = new TreeMap<>();
        for(ModuleClassLoader classLoader: reloads) {
            String name = classLoader.getName();

            List<String> moduleClassList = names.computeIfAbsent(name, new Function<String, List<String>>() {
                @Override
                public List<String> apply(String name) {
                    return  new ArrayList<>();
                }
            });
            for(Class<? extends Module> cls1: classLoader.getModuleClassList()) {
                moduleClassList.add(cls1.getName());
            }
        }
        newFinder.merge();

        ModuleFactoryChain newFactory = new ModuleFactoryChain(moduleFactory, reloads, new ModuleLoadedListener() {
            @Override
            public void onModuleLoaded(ModuleInfo moduleInfo, ModuleFactory factory) {
                System.out.println(moduleInfo.getModuleName() + " loaded");
            }

            @Override
            public void onAllModuleLoaded(ModuleFactory factory) {
                if(factory instanceof ModuleFactoryChain) {
                    ((ModuleFactoryChain) factory).merge();
                }
            }
        });
        for(Map.Entry<String, List<String>> name: names.entrySet()) {
            for(String moduleClassName: name.getValue()) {
                ModuleClassLoader classLoader = newFinder.getModuleClassLoader(name.getKey());
                Class<? extends Module> moduleClass = (Class<? extends Module>)classLoader.findClass(moduleClassName);
                newFactory.getModuleInfo(moduleClass);
            }
        }
    }

    public ModuleFactory getModuleFactory() {
        return this.moduleFactory;
    }
}
