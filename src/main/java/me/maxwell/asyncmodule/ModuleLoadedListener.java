package me.maxwell.asyncmodule;

public interface ModuleLoadedListener {
    void onModuleLoaded(ModuleInfo moduleInfo, ModuleFactory factory);
    void onAllModuleLoaded(ModuleFactory factory);
}
