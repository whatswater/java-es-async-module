package me.maxwell.asyncmodule;

public interface ClassLoaderBuilder {
    default String getCacheKey(String configName) {
        return configName;
    }
    default ModuleClassLoader createClassLoader(
            ClassLoaderFactory classLoaderFactory,
            String cacheKey
    ) {
        return new ModuleClassLoader(cacheKey, classLoaderFactory);
    }

    static ClassLoaderBuilder builder() {
        return new ClassLoaderBuilder() {  };
    }
}
