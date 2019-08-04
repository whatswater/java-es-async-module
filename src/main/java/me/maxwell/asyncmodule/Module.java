package me.maxwell.asyncmodule;

public interface Module {
    void register(ModuleFactory factory);
    default void onDependencyResolved(
            ModuleFactory factory,
            Class<? extends Module> moduleClass,
            String name
    ) {

    }
}
