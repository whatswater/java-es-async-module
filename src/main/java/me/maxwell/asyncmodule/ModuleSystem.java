package me.maxwell.asyncmodule;

// 使用一个统一的入口，初始加载模块。以后可以改成通过类加载器加载模块类
public class ModuleSystem {
    public static ModuleFactory load(String modulePath) throws ClassNotFoundException {
        Class<?> cls = Class.forName(modulePath);
        if(!Module.class.isAssignableFrom(cls)) {
            throw new RuntimeException("cls must be a module");
        }

        Class<Module> moduleClass = (Class<Module>) cls;
        ModuleFactory factory = new ModuleFactory();
        factory.loadTopLevelModule(moduleClass);

        return factory;
    }
}
