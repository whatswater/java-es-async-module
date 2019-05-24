package me.maxwell.asyncmodule;

public class Dependency<M> {
    private String name;
    private Class<M> type;

    private Dependency(
        String moduleName,
        Class<M> moduleType
    ) {
        this.name = moduleName;
        this.type = moduleType;
    }

    public static <M> Dependency<M> of(
        String moduleName,
        Class<M> moduleType
    ) {
        return new Dependency<>(moduleName, moduleType);
    }

    public String getName() {
        return name;
    }

    public Class<M> getType() {
        return type;
    }
}
