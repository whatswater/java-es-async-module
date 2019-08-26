package me.maxwell.asyncmodule;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// TODO 合并两个ModuleFactory
public class ModuleFactoryChain extends ModuleFactory {
    private ModuleFactory innerFactory;
    private Set<ModuleClassLoader> filter;

    public ModuleFactoryChain(ModuleFactory factory, Set<ModuleClassLoader> filter) {
        this.innerFactory = factory;
        this.filter = filter;
    }

    private Map<String, ModuleInfo> moduleInfoMap = new TreeMap<>();

    public ModuleInfo getModuleInfo(Class<? extends Module> moduleClass) {
        String key = getModuleName(moduleClass);
        ModuleClassLoader classLoader = (ModuleClassLoader)moduleClass.getClassLoader();
        if(filter.contains(classLoader)) {
            ModuleInfo moduleInfo = moduleInfoMap.get(key);
            if(moduleInfo == null) {
                moduleInfo = new ModuleInfo(moduleClass, this);
                moduleInfoMap.put(key, moduleInfo);
                moduleInfo.registerModule();
            }
            return moduleInfo;

        }
        else {
            return innerFactory.getModuleInfo(moduleClass);
        }
    }

    public void resetFactory() {
        for(Map.Entry<String, ModuleInfo> moduleInfoEntry: moduleInfoMap.entrySet()) {
            moduleInfoEntry.getValue().setFactory(innerFactory);
        }
    }
}
