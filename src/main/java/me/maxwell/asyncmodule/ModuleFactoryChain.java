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
        this.moduleInfoList = new ArrayList<>();
    }

    @Override
    public ModuleInfo getModuleInfo(Class<? extends Module> moduleClass) {
        String moduleName = getModuleName(moduleClass);
        ModuleInfo moduleInfo = getModuleInfoMap().get(moduleName);
        ModuleClassLoader classLoader = (ModuleClassLoader)moduleClass.getClassLoader();

        // 如果有新的模块加载，也在此工厂加载，以免工厂事件重复调用
        if(moduleInfo == null || classLoaders.contains(classLoader)) {
            ModuleInfo newModuleInfo = super.getModuleInfo(moduleClass);

            // 优化性能，改成并发的链表，且能一次add多条数据
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
    public void require(Class<? extends Module> moduleClass, Require require) {
        String moduleName = getModuleName(moduleClass);
        ModuleInfo moduleInfo = innerFactory.findModuleInfo(moduleName);
        ModuleClassLoader classLoader = (ModuleClassLoader)moduleClass.getClassLoader();

        if(moduleInfo == null || classLoaders.contains(classLoader)) {
            ModuleInfo newModuleInfo = super.getModuleInfo(moduleClass);
            newModuleInfo.addDependency(require);
        }
        else {
            moduleInfo.addDependency(require);
        }
    }

    // TODO 校验merge方法逻辑和线程安全问题
    public void merge() {
        Set<ModuleInfo> moduleNames = new HashSet<>();
        for(int i = 0; i < moduleInfoList.size(); i = i + 2) {
            if(moduleInfoList.get(i) != null) {
                moduleNames.add(moduleInfoList.get(i));
            }
        }

        for(int i = 0; i < moduleInfoList.size(); i = i + 2) {
            ModuleInfo oldModuleInfo = moduleInfoList.get(i);
            if(oldModuleInfo == null) {
                continue;
            }
            ModuleInfo newModuleInfo  = moduleInfoList.get(i + 1);

            // 从ListenerList中删除老的
            Map<Class<? extends Module>, Set<String>> requires = oldModuleInfo.getRequires();
            for(Map.Entry<Class<? extends Module>, Set<String>> entry: requires.entrySet()) {
                ModuleInfo required = innerFactory.getModuleInfo(entry.getKey());
                if(!moduleNames.contains(required)) {
                    for(String requireName: entry.getValue()) {
                        ModuleListenerList listenerList = required.getExports().get(requireName);
                        listenerList.getModuleInfoSet().remove(oldModuleInfo);
                    }
                }
            }

            // 向新加载的模块添加观察者，并且调用观察者的重新加载方法
            Map<String, ModuleListenerList> oldExports = oldModuleInfo.getExports();
            Map<String, ModuleListenerList> newExports = newModuleInfo.getExports();
            for(Map.Entry<String, ModuleListenerList> entry: oldExports.entrySet()) {
                if(entry.getValue().getModuleInfoSet() == null) {
                    continue;
                }
                for(ModuleInfo moduleInfo: entry.getValue().getModuleInfoSet()) {
                    if(moduleNames.contains(moduleInfo)) {
                        continue;
                    }

                    ModuleListenerList listenerList = newExports.get(entry.getKey());
                    if(listenerList == null || listenerList.getModuleExport() == null) {
                        moduleInfo.getModuleInstance().onReloadRequireMissed(newModuleInfo, newModuleInfo, entry.getKey());
                    }
                    else {
                        moduleInfo.getModuleInstance().onReloadRequireResolved(newModuleInfo, newModuleInfo, entry.getKey());
                        listenerList.getModuleInfoSet().add(moduleInfo);
                    }
                }
            }
        }

        Map<String, ModuleInfo> moduleInfoMap = getModuleInfoMap();
        for(Map.Entry<String, ModuleInfo> moduleInfoEntry: moduleInfoMap.entrySet()) {
            moduleInfoEntry.getValue().setFactory(innerFactory);
        }
        innerFactory.getModuleInfoMap().putAll(moduleInfoMap);
    }
}
