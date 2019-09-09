package me.maxwell.asyncmodule;

import java.util.*;

public class ModuleFactoryChain extends ModuleFactory {
    private final ModuleFactory innerFactory;
    private final ClassLoaderFactoryChain classLoaderFactory;
    private final List<ModuleInfo> moduleInfoList;
    private final Object lock = new Object();

    public ModuleFactoryChain(ModuleFactory factory, ClassLoaderFactoryChain classLoaderFactory, ModuleLoadedListener listener) {
        super(listener);
        this.innerFactory = factory;
        this.classLoaderFactory = classLoaderFactory;
        this.moduleInfoList = new LinkedList<>();
    }

    @Override
    public ModuleInfo getModuleInfo(Class<? extends Module> moduleClass) {
        String moduleName = getModuleName(moduleClass);
        ModuleInfo moduleInfo = innerFactory.findModuleInfo(moduleName);
        ModuleClassLoader classLoader = (ModuleClassLoader)moduleClass.getClassLoader();

        // 如果有新的模块加载，也在此工厂加载，以免工厂事件重复调用
        if(moduleInfo == null || classLoaderFactory.isNewLoad(classLoader)) {
            ModuleInfo newModuleInfo = super.getModuleInfo(moduleClass);
            synchronized(lock) {
                moduleInfoList.add(moduleInfo);
                moduleInfoList.add(newModuleInfo);
            }

            return newModuleInfo;
        }
        else {
            return moduleInfo;
        }
    }

    @Override
    public ModuleInfo require(Class<? extends Module> moduleClass, Require require) {
        String moduleName = getModuleName(moduleClass);
        ModuleInfo moduleInfo = innerFactory.findModuleInfo(moduleName);
        ModuleClassLoader classLoader = (ModuleClassLoader)moduleClass.getClassLoader();

        if(moduleInfo == null || classLoaderFactory.isNewLoad(classLoader)) {
            ModuleInfo newModuleInfo = super.getModuleInfo(moduleClass);
            newModuleInfo.addDependency(require);
            return newModuleInfo;
        }
        else {
            moduleInfo.addDependency(require);
            return moduleInfo;
        }
    }

    /**
     * 合并两个ModuleFactory，此方法是线程不安全的
     */
    public void merge() {
        Set<String> moduleNames = new HashSet<>();
        for(int i = 0; i < moduleInfoList.size(); i = i + 2) {
            ModuleInfo moduleInfo = moduleInfoList.get(i);
            if(moduleInfo != null) {
                moduleNames.add(moduleInfo.getModuleName());
                if(i % 2 == 1) {
                    moduleInfo.setFactory(innerFactory);
                }
            }
        }

        for(int i = 0; i < moduleInfoList.size(); i = i + 2) {
            ModuleInfo oldModuleInfo = moduleInfoList.get(i);
            if(oldModuleInfo == null) {
                continue;
            }
            ModuleInfo newModuleInfo  = moduleInfoList.get(i + 1);
            oldModuleInfo.unListen(moduleNames);
            newModuleInfo.reExport(oldModuleInfo, moduleNames);
        }
        innerFactory.getModuleInfoMap().putAll(getModuleInfoMap());
    }
}
