package me.maxwell.asyncmodule;

import java.util.function.Function;

public class CreateLoaderFunction implements Function<String, ModuleClassLoader> {
    private ClassLoaderFactory factory;
    private ClassLoaderBuilder builder;

    public CreateLoaderFunction(ClassLoaderFactory factory, ClassLoaderBuilder builder) {
        this.factory = factory;
        this.builder = builder;
    }

    @Override
    public ModuleClassLoader apply(String classLoaderName) {
        return builder.createClassLoader(
                factory,
                factory.getParent(),
                classLoaderName
        );
    }
}
