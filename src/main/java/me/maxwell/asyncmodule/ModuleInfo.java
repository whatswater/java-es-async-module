package me.maxwell.asyncmodule;

import java.util.*;
import java.util.function.Function;

public class ModuleInfo {
    private String moduleName;
    private Module moduleInstance;
    private Map<String, ModuleListenerList> exports = new TreeMap<>();
    private Map<Class<? extends Module>, Set<String>> requires = new HashMap<>();
    ModuleState moduleState;
    private ModuleFactory factory;

    ModuleInfo(Class<? extends Module> moduleClass, ModuleFactory factory) {
        try {
            this.factory = factory;
            this.moduleName = factory.getModuleName(moduleClass);
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
            String... moduleNames
    ) {
        Require require = new Require(this, moduleNames);
        factory.require(moduleClass, require);
        addRequire(moduleClass, require);
    }

    private synchronized void addRequire(Class<? extends Module> moduleClass, Require newRequire) {
        Set<String> names = requires.computeIfAbsent(moduleClass, new Function<Class<? extends Module>, Set<String>>() {
            @Override
            public Set<String> apply(Class<? extends Module> cls) {
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
        if(moduleInfoSet == null) {
            moduleInfoSet = new HashSet<>();
            moduleListenerList.setModuleInfoSet(moduleInfoSet);
        }

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
        if(moduleInfoSet != null) {
            for(ModuleInfo moduleInfo: moduleInfoSet) {
                Module module = moduleInfo.getModuleInstance();
                module.onRequireResolved(moduleInfo, this, name);
            }
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
            if(moduleInfoSet != null) {
                String name = entry.getKey();
                for(ModuleInfo moduleInfo: moduleInfoSet) {
                    Module module = moduleInfo.getModuleInstance();
                    module.onRequireMissed(moduleInfo, this, name);
                }
            }
        }
    }

    public Module getModuleInstance() {
        return this.moduleInstance;
    }

    public Class<? extends Module> getModuleClass() {
        return moduleInstance.getClass();
    }

    public ModuleState getModuleState() {
        return moduleState;
    }

    public Map<String, ModuleListenerList> getExports() {
        return this.exports;
    }

    public Map<Class<? extends Module>, Set<String>> getRequires() {
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
