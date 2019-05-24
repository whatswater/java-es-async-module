package me.maxwell.asyncmodule;

import org.jdeferred2.Deferred;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ModuleInfo {
    private Module moduleInstance;
    private Map<String, List<Deferred<? super Object, ModuleSystemException, Void>>> dependencyListMap = new TreeMap<>();
    private Map<String, Object> exports = new TreeMap<>();

    public ModuleInfo() {

    }

    public Module getModuleInstance() {
        return moduleInstance;
    }

    public void setModuleInstance(Module moduleInstance) {
        this.moduleInstance = moduleInstance;
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

    public List<Deferred<? super Object, ModuleSystemException, Void>> getDependencyList(String name) {
        return dependencyListMap.get(name);
    }

    public void removeDependencyList(String name) {
        dependencyListMap.remove(name);
    }

    public void addDependency(String name, Deferred<Object, ModuleSystemException, Void> deferred) {
        List<Deferred<? super Object, ModuleSystemException, Void>> dependencyList = dependencyListMap.get(name);
        if(dependencyList == null) {
            dependencyList = new LinkedList<>();
            dependencyListMap.put(name, dependencyList);
        }
        dependencyList.add(deferred);
    }

    public void addExport(String name, Object obj) {
        exports.put(name, obj);
        List<Deferred<? super Object, ModuleSystemException, Void>> dependencyList = getDependencyList(name);

        if(dependencyList != null) {
            for(Deferred<Object, ModuleSystemException, Void> deferred: dependencyList) {
                deferred.resolve(obj);
            }
            removeDependencyList(name);
        }
    }

    public boolean hasExport(String name) {
        return exports.containsKey(name);
    }

    public Object getExport(String name) {
        return exports.get(name);
    }
}
