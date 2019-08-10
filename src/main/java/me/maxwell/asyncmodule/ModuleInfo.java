package me.maxwell.asyncmodule;

import java.util.*;

public class ModuleInfo {
    private Module moduleInstance;
    private Map<String, ModuleListenerList> exports = new TreeMap<>();
    private ModuleState moduleState;
    private ModuleFactory factory;

    public ModuleInfo(Class<? extends Module> moduleClass, ModuleFactory factory) {
        try {
            this.factory = factory;
            this.moduleInstance = moduleClass.newInstance();
            this.moduleState = ModuleState.INSTANCE;
        } catch(InstantiationException | IllegalAccessException e) {
            throw new InstanceModuleException(
                "instance module failed, the module class: " + moduleClass.getName(),
                e
            );
        }
    }

    protected void registerModule() {
        this.moduleInstance.register(this);
        this.moduleState = ModuleState.LOADING;
    }

    public final void require(
            Class<? extends Module> moduleClass
    ) {
        require(moduleClass, ModuleFactory.DEFAULT_VERSION);
    }

    public final void require(
            Class<? extends Module> moduleClass,
            String... moduleNames
    ) {
        Require require = new Require(moduleInstance, moduleNames);
        factory.require(moduleClass, require);
    }

    public void addDependency(Require newRequire) {
        Set<String> names = newRequire.getRequireNames();
        for(String name: names) {
            addDependency(name, newRequire.getModule());
        }
    }

    public void addDependency(String name, Module module) {
        ModuleListenerList moduleListenerList = exports.get(name);
        if(moduleListenerList == null) {
            moduleListenerList = new ModuleListenerList();
            exports.put(name, moduleListenerList);
        }
        List<Module> moduleList = moduleListenerList.getModuleList();
        if(moduleList == null) {
            moduleList = new ArrayList<>();
            moduleListenerList.setModuleList(moduleList);
        }

        if(moduleListenerList.getModuleExport() != null) {
            factory.onModuleRequireResolved(module, getModuleClass(), name);
        }
        moduleList.add(module);
    }

    public void export(Object obj) {
        export(ModuleFactory.DEFAULT_VERSION, obj);
    }

    public void export(String name, Object obj) {
        ModuleListenerList moduleListenerList = exports.get(name);
        if(moduleListenerList == null) {
            moduleListenerList = new ModuleListenerList();
            exports.put(name, moduleListenerList);
        }
        moduleListenerList.setModuleExport(obj);
        List<Module> moduleList = moduleListenerList.getModuleList();
        if(moduleList != null) {
            for(Module module: moduleList) {
                factory.onModuleRequireResolved(module, getModuleClass(), name);
            }
        }
    }

    public <T> T getModuleExport(Class<? extends Module> cls, String name) {
        return (T)factory.getExport(cls, name);
    }

    public Object getExport(String name) {
        ModuleListenerList moduleListenerList = exports.get(name);
        if(moduleListenerList == null) {
            return null;
        }
        return moduleListenerList.getModuleExport();
    }

    public void setModuleLoaded() {
        moduleState = ModuleState.RUNNING;
        for(Map.Entry<String, ModuleListenerList> entry: exports.entrySet()) {
            if(entry.getValue().getModuleExport() != null) {
                continue;
            }
            List<Module> moduleList = entry.getValue().getModuleList();
            if(moduleList != null) {
                for(Module module: moduleList) {
                    factory.onModuleRequireMiss(module, getModuleClass(), entry.getKey());
                }
            }
        }
    }

    public Class<? extends Module> getModuleClass() {
        return moduleInstance.getClass();
    }

    public ModuleState getModuleState() {
        return moduleState;
    }

    public ModuleFactory getFactory() {
        return factory;
    }
}
