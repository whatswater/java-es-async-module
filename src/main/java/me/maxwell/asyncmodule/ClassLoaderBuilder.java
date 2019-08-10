package me.maxwell.asyncmodule;

public interface ClassLoaderBuilder {
    default String getCacheKey(String configName) {
        return configName;
    }
    ModuleClassLoader createClassLoader(ClassLoaderFinder finder, ClassLoader parent, String configName);

    static ClassLoaderBuilder builder() {
        return new DefaultBuider();
    }

    class DefaultBuider implements ClassLoaderBuilder {
        @Override
        public ModuleClassLoader createClassLoader(ClassLoaderFinder finder, ClassLoader parent, String configName) {
            ModuleClassLoader classLoader = new ModuleClassLoader(parent);
            classLoader.setFinder(finder);
            return classLoader;
        }
    }
}
