package me.maxwell.asyncmodule;

public interface Module {
    void register(ModuleInfo moduleInfo);
    default void onRequireResolved(
            ModuleInfo moduleInfo,
            Class<? extends Module> moduleClass,
            String name
    ) {}
    default void onMissRequire(
            ModuleInfo moduleInfo,
            Class<? extends Module> moduleClass,
            String name
    ) {}
}
