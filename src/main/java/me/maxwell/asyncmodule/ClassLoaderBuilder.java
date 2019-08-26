package me.maxwell.asyncmodule;

public interface ClassLoaderBuilder {
    default String getCacheKey(String configName) {
        return configName;
    }
    ModuleClassLoader createClassLoader(
            ClassLoaderFactory classLoaderFactory,
            ClassLoader parent,
            String cacheKey
    );


    static ClassLoaderBuilder builder() {
        return new DefaultBuilder();
    }

    class DefaultBuilder implements ClassLoaderBuilder {
        @Override
        public ModuleClassLoader createClassLoader(
                ClassLoaderFactory finder,
                ClassLoader parent,
                String cacheKey
        ) {

            ModuleClassLoader classLoader = new ModuleClassLoader(parent, cacheKey);
            classLoader.setFinder(finder);
            return classLoader;
        }
    }
}
