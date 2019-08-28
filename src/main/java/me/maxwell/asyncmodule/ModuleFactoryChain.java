package me.maxwell.asyncmodule;

import java.util.Map;
import java.util.Set;

public class ModuleFactoryChain extends ModuleFactory {
    private final ModuleFactory innerFactory;
    private final Set<ModuleClassLoader> filter;

    public ModuleFactoryChain(ModuleFactory factory, Set<ModuleClassLoader> filter) {
        this.innerFactory = factory;
        this.filter = filter;
    }
    public ModuleFactoryChain(ModuleFactory factory, Set<ModuleClassLoader> filter, ModuleLoadedListener listener) {
        super(listener);
        this.innerFactory = factory;
        this.filter = filter;
    }

    @Override
    public ModuleInfo getModuleInfo(Class<? extends Module> moduleClass) {
        ModuleClassLoader classLoader = (ModuleClassLoader)moduleClass.getClassLoader();
        if(filter.contains(classLoader)) {
            return super.getModuleInfo(moduleClass);
        }
        else {
            return innerFactory.getModuleInfo(moduleClass);
        }
    }

    @Override
    public void require(Class<? extends Module> moduleClass, Require require) {
        ModuleClassLoader classLoader = (ModuleClassLoader)moduleClass.getClassLoader();
        if(!filter.contains(classLoader)) {
            super.getModuleInfo(moduleClass).addDependency(require);
        }
        else {
            ModuleInfo moduleInfo = innerFactory.getModuleInfo(moduleClass);
            moduleInfo.addDependency(require);

            /*String moduleName = getModuleName(require.getModule().getClass());
            for(String name: require.getRequireNames()) {
                ModuleListenerList moduleListenerList = moduleInfo.getListenerList(name);
                if(moduleListenerList == null) {
                    continue;
                }

                for(Module module: moduleListenerList.getModuleList()) {
                    ModuleClassLoader loader = (ModuleClassLoader) module.getClass().getClassLoader();
                    final boolean del = !filter.contains(loader) && moduleName.equals(getModuleName(module.getClass()));
                    if(del) {
                        moduleListenerList.getModuleList().remove(module);
                    }
                }
            }*/
        }
    }

    public void add() {

    }

    public void merge() {
        Map<String, ModuleInfo> moduleInfoMap = getModuleInfoMap();
        for(Map.Entry<String, ModuleInfo> moduleInfoEntry: moduleInfoMap.entrySet()) {
            moduleInfoEntry.getValue().setFactory(innerFactory);
        }
        innerFactory.getModuleInfoMap().putAll(moduleInfoMap);
    }
}
