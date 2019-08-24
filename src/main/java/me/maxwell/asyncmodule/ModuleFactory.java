package me.maxwell.asyncmodule;

import java.util.Map;
import java.util.TreeMap;

// 模块本身是否存在，根据类加载器确定；模块是否导出了某个对象，由模块系统支持
// 不同工厂的NAME_SPACE实例属性可以相同，使用NAME_SPACE实现资源隔离。
// 使用版本号实现模块路径隔离。
// TODO 测试现在的加载逻辑 已经完成
// TODO 编写顶层模块的load方法 已经完成
// TODO 优化ModuleFactory中moduleInfoMap的存储，优化ModuleInfo的dependencyList存储 已经完成
// TODO 在工厂中添加一个方法，模块能够通知模块已经完全加载完毕 已经完成
// TODO require的时候，Name加类型属性，为Symbol，所有的和Name相关的，均加上泛型
// TODO 当模块加载完毕后，如果有其他模块的依赖未满足，那么需要通知其他的模块 已经完成
// TODO 模块工厂需要提供所有的模块均已加载完毕的事件。
// TODO 设计类加载器。已经完成。
// TODO 当某个模块不使用模块类加载器时，此模块依赖的类也不会使用模块类加载器，建立一个公共类加载器，作为没有找到模块类加载器的后备。
// TODO 解决由类加载器所带来的权限问题 已经解决
// TODO 当模块加载完毕后，此模块所导出的对象的类型全部在另外一个类加载器中，那么记录此模块的此特性，在重新加载的时候，不需要更新其上层模块
// TODO 设计模块卸载的接口，以释放资源。
// TODO 编写模块重新加载的方法
public class ModuleFactory {
    public static final String DEFAULT_VERSION = "default";

    private Map<String, ModuleInfo> moduleInfoMap = new TreeMap<>();

    public String getModuleName(Class<? extends Module> moduleClass) {
        String name = moduleClass.getName();
        ModuleVersion version = moduleClass.getDeclaredAnnotation(ModuleVersion.class);
        String v = DEFAULT_VERSION;
        if(version != null) {
            v = version.value();
        }
        return name + ":" + v;
    }

    public ModuleInfo getModuleInfo(Class<? extends Module> moduleClass) {
        String key = getModuleName(moduleClass);
        ModuleInfo moduleInfo = moduleInfoMap.get(key);
        if(moduleInfo == null) {
            moduleInfo = new ModuleInfo(moduleClass, this);
            moduleInfoMap.put(key, moduleInfo);
            moduleInfo.registerModule();
        }
        return moduleInfo;
    }

    public void require(Class<? extends Module> moduleClass, Require require) {
        ModuleInfo moduleInfo = getModuleInfo(moduleClass);
        moduleInfo.addDependency(require);
    }

    public Object getExport(Class<? extends Module> moduleClass, String name) {
        ModuleInfo moduleInfo = getModuleInfo(moduleClass);
        return moduleInfo.getExport(name);
    }

    public void onModuleRequireResolved(Module module, Class<? extends Module> requiredModuleClass, String name) {
        ModuleInfo info = getModuleInfo(module.getClass());
        module.onRequireResolved(info, requiredModuleClass, name);
    }

    public void onModuleRequireMiss(Module module, Class<? extends Module> requiredModuleClass, String name) {
        ModuleInfo info = getModuleInfo(module.getClass());
        module.onMissRequire(info, requiredModuleClass, name);
    }
}
