package me.maxwell.asyncmodule;

import java.util.Set;
import java.util.TreeSet;

public class Require {
    private final Module module;
    private Set<String> requireNames;

    public Require(Module module, String ...names) {
        requireNames = new TreeSet<>();
        for(String name: names) {
            requireNames.add(name);
        }

        this.module = module;
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

    public Module getModule() {
        return module;
    }

    public Set<String> getRequireNames() {
        return requireNames;
    }
}
