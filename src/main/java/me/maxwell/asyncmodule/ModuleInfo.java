package me.maxwell.asyncmodule;

import lombok.Setter;

import java.util.*;

public class ModuleInfo {
    private Module moduleInstance;
    private List<Dependency> dependencyList = new ArrayList<>();
    private Map<String, Object> exports = new TreeMap<>();
    @Setter
    private ModuleState moduleState;

    public Module getModuleInstance() {
        return moduleInstance;
    }

    public ModuleInfo(Class<? extends Module> moduleClass) {
        try {
            this.moduleInstance = moduleClass.newInstance();
            this.moduleState = ModuleState.INSTANCE;
        } catch(InstantiationException | IllegalAccessException e) {
            throw new InstanceModuleException(
                "instance module failed, the module class: " + moduleClass.getName(),
                e
            );
        }
    }

    public void registerModule(ModuleFactory factory) {
        this.moduleInstance.register(factory);
        this.moduleState = ModuleState.LOADING;
    }

    public List<Dependency> getDependencyList() {
        return dependencyList;
    }

    public Dependency addDependency(Dependency newDependency) {
        for(Dependency dependency: dependencyList) {
            if(dependency.getModule().equals(newDependency)) {
                dependency.addRequire(newDependency.getRequireNames());

                return dependency;
            }
        }

        dependencyList.add(newDependency);
        return newDependency;
    }

    public void addExport(String name, Object obj) {
        exports.put(name, obj);
    }

    public boolean hasExport(String name) {
        return exports.containsKey(name);
    }

    public Object getExport(String name) {
        return exports.get(name);
    }
}
