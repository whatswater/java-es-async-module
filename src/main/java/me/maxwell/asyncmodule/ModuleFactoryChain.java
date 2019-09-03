package me.maxwell.asyncmodule;

import java.util.*;

public class ModuleFactoryChain extends ModuleFactory {
    private final ModuleFactory innerFactory;
    private final Set<ModuleClassLoader> classLoaders;
    private final List<ModuleInfo> moduleInfoList;
    private final Object lock = new Object();

    public ModuleFactoryChain(ModuleFactory factory, Set<ModuleClassLoader> filter, ModuleLoadedListener listener) {
        super(listener);
        this.innerFactory = factory;
        this.classLoaders = filter;
        this.moduleInfoList = new LinkedList<>();
    }

    @Override
    public ModuleInfo getModuleInfo(Class<? extends Module> moduleClass) {
        String moduleName = getModuleName(moduleClass);
        ModuleInfo moduleInfo = getModuleInfoMap().get(moduleName);
        ModuleClassLoader classLoader = (ModuleClassLoader)moduleClass.getClassLoader();

        // 如果有新的模块加载，也在此工厂加载，以免工厂事件重复调用
        if(moduleInfo == null || classLoaders.contains(classLoader)) {
            ModuleInfo newModuleInfo = super.getModuleInfo(moduleClass);
            synchronized(lock) {
                moduleInfoList.add(moduleInfo);
                moduleInfoList.add(newModuleInfo);
            }

            return newModuleInfo;
        }
        else {
            return innerFactory.getModuleInfo(moduleClass);
        }
    }

    @Override
    public ModuleInfo require(Class<? extends Module> moduleClass, Require require) {
        String moduleName = getModuleName(moduleClass);
        ModuleInfo moduleInfo = innerFactory.findModuleInfo(moduleName);
        ModuleClassLoader classLoader = (ModuleClassLoader)moduleClass.getClassLoader();

        if(moduleInfo == null || classLoaders.contains(classLoader)) {
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
        innerFactory.getModuleInfoMap().putAll(getModuleInfoMap());

        for(int i = 0; i < moduleInfoList.size(); i = i + 2) {
            ModuleInfo oldModuleInfo = moduleInfoList.get(i);
            if(oldModuleInfo == null) {
                continue;
            }
            ModuleInfo newModuleInfo  = moduleInfoList.get(i + 1);

            // 从ListenerList中删除老的
            Map<String, Set<String>> requires = oldModuleInfo.getRequires();
            for(Map.Entry<String, Set<String>> entry: requires.entrySet()) {
                String moduleName = entry.getKey();
                // 只有不在重新加载范围内的才需要去除监听
                if(moduleNames.contains(moduleName)) {
                    continue;
                }

                ModuleInfo required = innerFactory.findModuleInfo(moduleName);
                for(String requireName: entry.getValue()) {
                    ModuleListenerList listenerList = required.getExports().get(requireName);
                    listenerList.getModuleInfoSet().remove(oldModuleInfo);
                }
            }

            // 向新加载的模块添加观察者，并且调用观察者的重新加载方法
            Map<String, ModuleListenerList> oldExports = oldModuleInfo.getExports();
            for(Map.Entry<String, ModuleListenerList> entry: oldExports.entrySet()) {
                for(ModuleInfo moduleInfo: entry.getValue().getModuleInfoSet()) {
                    if(moduleNames.contains(moduleInfo.getModuleName())) {
                        continue;
                    }
                    String requireName = entry.getKey();
                    Map<String, ModuleListenerList> newExports = newModuleInfo.getExports();
                    ModuleListenerList listenerList = newExports.get(requireName);

                    // 当重新加载的模块被其他模块依赖而又没有满足时，listenerList不为null但是moduleExport为null
                    if(listenerList == null || listenerList.getModuleExport() == null) {
                        moduleInfo.getModuleInstance().onReloadRequireMissed(moduleInfo, newModuleInfo, requireName);
                    }
                    else {
                        listenerList.getModuleInfoSet().add(moduleInfo);
                        moduleInfo.getModuleInstance().onReloadRequireMissed(moduleInfo, newModuleInfo, requireName);
                    }
                }
            }
        }
    }
}
