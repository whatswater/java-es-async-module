package me.maxwell.asyncmodule;

import java.util.Set;
import java.util.TreeSet;

public class Require {
    private final ModuleInfo moduleInfo;
    private Set<String> requireNames;

    public Require(ModuleInfo moduleInfo, String ...names) {
        requireNames = new TreeSet<>();
        for(String name: names) {
            requireNames.add(name);
        }

        this.moduleInfo = moduleInfo;
    }

    public void addRequire(String name) {
        requireNames.add(name);
    }

    public void addRequire(Set<String> names) {
        requireNames.addAll(names);
    }

    public void removeRequire(String name) {
        requireNames.remove(name);
    }

    public void removeRequire(Set<String> names) {
        requireNames.removeAll(names);
    }

    public ModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    public Set<String> getRequireNames() {
        return requireNames;
    }
}
