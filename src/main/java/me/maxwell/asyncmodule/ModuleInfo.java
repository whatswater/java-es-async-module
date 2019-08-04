package me.maxwell.asyncmodule;

import org.jdeferred2.Deferred;

import java.util.*;

// TODO 优化依赖列表的存储
public class ModuleInfo {
    private Module moduleInstance;
    private List<Dependency> dependencyList = new ArrayList<>();
    private Map<String, Object> exports = new TreeMap<>();

    public ModuleInfo() {

    }

    public Module getModuleInstance() {
        return moduleInstance;
    }

    public ModuleInfo(Class<? extends Module> moduleClass) {
        try {
            this.moduleInstance = moduleClass.newInstance();
        } catch(InstantiationException | IllegalAccessException e) {
            throw new InstanceModuleException(
                "instance module failed, the module class: " + moduleClass.getName(),
                e
            );
        }
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
