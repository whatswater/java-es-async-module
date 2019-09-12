package me.maxwell.asyncmodule;

import java.util.*;
import java.util.function.Function;

public class ModuleInfo {
    private String moduleName;
    private Module moduleInstance;
    private Map<String, ModuleListenerList> exports = new TreeMap<>();
    private Map<String, Set<String>> requires = new HashMap<>();
    volatile ModuleState moduleState;
    private ModuleFactory factory;

    ModuleInfo(Class<? extends Module> moduleClass, String moduleName, ModuleFactory factory) {
        try {
            this.factory = factory;
            this.moduleName = moduleName;
            this.moduleInstance = moduleClass.newInstance();
            this.moduleState = ModuleState.INSTANCE;
        } catch(InstantiationException | IllegalAccessException e) {
            throw new InstanceModuleException(
                "instance module failed, the module class: " + moduleClass.getName(),
                e
            );
        }
    }

    public final synchronized void require(
            Class<? extends Module> moduleClass
    ) {
        require(moduleClass, ModuleFactory.DEFAULT_VERSION);
    }

    public final synchronized void require(
            Class<? extends Module> moduleClass,
            String... requireNames
    ) {
        Require require = new Require(this, requireNames);
        ModuleInfo moduleInfo = factory.require(moduleClass, require);
        addRequire(moduleInfo.getModuleName(), require);
    }

    public final synchronized void require(String className) {
        require(className, ModuleFactory.DEFAULT_VERSION, ModuleFactory.DEFAULT_NAME);
    }

    public final synchronized void require(String className, String requireName) {
        require(className, ModuleFactory.DEFAULT_VERSION, requireName);
    }

    public final synchronized void require(String className, String version, String... requireNames) {
        Require require = new Require(this, requireNames);
        ModuleInfo moduleInfo = factory.require(className, version, require);
        addRequire(moduleInfo.getModuleName(), require);
    }

    private synchronized void addRequire(String moduleName, Require newRequire) {
        Set<String> names = requires.computeIfAbsent(moduleName, new Function<String, Set<String>>() {
            @Override
            public Set<String> apply(String moduleName) {
                return new TreeSet<>();
            }
        });
        names.addAll(newRequire.getRequireNames());
    }

    synchronized void addDependency(Require newRequire) {
        Set<String> names = newRequire.getRequireNames();
        for(String name: names) {
            addDependency(name, newRequire.getModuleInfo());
        }
    }

    private synchronized void addDependency(String name, ModuleInfo moduleInfo) {
        ModuleListenerList moduleListenerList = exports.get(name);
        if(moduleListenerList == null) {
            moduleListenerList = new ModuleListenerList();
            exports.put(name, moduleListenerList);
        }
        Set<ModuleInfo> moduleInfoSet = moduleListenerList.getModuleInfoSet();
        Module module = moduleInfo.getModuleInstance();
        if(moduleListenerList.getModuleExport() != null) {
            module.onRequireResolved(moduleInfo, this, name);
        }
        moduleInfoSet.add(moduleInfo);
    }

    public synchronized void export(Object obj) {
        export(ModuleFactory.DEFAULT_NAME, obj);
    }

    public synchronized void export(String name, Object obj) {
        ModuleListenerList moduleListenerList = exports.get(name);
        if(moduleListenerList == null) {
            moduleListenerList = new ModuleListenerList();
            exports.put(name, moduleListenerList);
        }
        moduleListenerList.setModuleExport(obj);
        Set<ModuleInfo> moduleInfoSet = moduleListenerList.getModuleInfoSet();
        for(ModuleInfo moduleInfo: moduleInfoSet) {
            Module module = moduleInfo.getModuleInstance();
            module.onRequireResolved(moduleInfo, this, name);
        }
    }

    public synchronized <T> T getExport(Symbol<T> name) {
        ModuleListenerList moduleListenerList = exports.get(name.getKey());
        if(moduleListenerList == null) {
            return null;
        }
        return (T)moduleListenerList.getModuleExport();
    }

    public synchronized void setModuleLoaded() {
        factory.setModuleLoaded(moduleName);
        for(Map.Entry<String, ModuleListenerList> entry: exports.entrySet()) {
            if(entry.getValue().getModuleExport() != null) {
                continue;
            }
            Set<ModuleInfo> moduleInfoSet = entry.getValue().getModuleInfoSet();
            String name = entry.getKey();
            for(ModuleInfo moduleInfo: moduleInfoSet) {
                Module module = moduleInfo.getModuleInstance();
                module.onRequireMissed(moduleInfo, this, name);
            }
        }
    }

    // 去除监听
    public synchronized void unListen(Set<String> moduleNames) {
        for(Map.Entry<String, Set<String>> entry: requires.entrySet()) {
            String moduleName = entry.getKey();
            if(moduleNames.contains(moduleName)) {
                continue;
            }

            ModuleInfo required = factory.findModuleInfo(moduleName);
            for(String requireName: entry.getValue()) {
                ModuleListenerList listenerList = required.getExports().get(requireName);
                listenerList.getModuleInfoSet().remove(this);
            }
        }
    }

    // 向新加载的模块添加观察者，并且调用观察者的重新加载方法
    public synchronized void reExport(ModuleInfo oldModuleInfo, Set<String> exceptNames) {
        Map<String, ModuleListenerList> oldExports = oldModuleInfo.getExports();
        for(Map.Entry<String, ModuleListenerList> entry: oldExports.entrySet()) {
            for(ModuleInfo moduleInfo: entry.getValue().getModuleInfoSet()) {
                if(exceptNames.contains(moduleInfo.getModuleName())) {
                    continue;
                }
                String requireName = entry.getKey();
                ModuleListenerList listenerList = exports.get(requireName);

                // 当重新加载的模块被其他模块依赖而又没有满足时，listenerList不为null但是moduleExport为null
                if(listenerList == null || listenerList.getModuleExport() == null) {
                    moduleInfo.getModuleInstance().onReloadRequireMissed(moduleInfo, this, requireName);
                }
                else {
                    listenerList.getModuleInfoSet().add(moduleInfo);
                    moduleInfo.getModuleInstance().onReloadRequireResolved(moduleInfo, this, requireName);
                }
            }
        }
    }

    public Module getModuleInstance() {
        return this.moduleInstance;
    }

    public ModuleState getModuleState() {
        return moduleState;
    }

    public Map<String, ModuleListenerList> getExports() {
        return this.exports;
    }

    public Map<String, Set<String>> getRequires() {
        return this.requires;
    }

    public void setFactory(ModuleFactory factory) {
        this.factory = factory;
    }

    public ModuleFactory getFactory() {
        return factory;
    }

    public String getModuleName() {
        return this.moduleName;
    }

    @Override
    public boolean equals(Object object) {
        if(this == object) {
            return true;
        }

        if(object == null) {
            return false;
        }

        if(object.getClass() != this.getClass()) {
            return false;
        }

        ModuleInfo moduleInfo = (ModuleInfo) object;
        if(!this.moduleName.equals(moduleInfo.moduleName)) {
            return false;
        }
        return moduleInstance.getClass().equals(moduleInfo.moduleInstance.getClass());
    }

    @Override
    public int hashCode() {
        return this.moduleName.hashCode();
    }
}
