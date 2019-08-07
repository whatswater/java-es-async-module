package me.maxwell.asyncmodule;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// 设计取舍：1、是否需要专门写一个holder类，持有module实例和factory，来简化对ModuleFactory.require的调用。
// 设计取舍：不需要，当需要简化时，可以调用ModuleFactory.require的其他方法。在Module对象的register方法内，
// 设计取舍：2、ModuleFactory.require方法，是否需要提供根据字符串确定模块空间的方法，以免在类加载的时候发生classNotFound
// 设计取舍：需要。并且ModuleFactory需要提供某个模块是否存在的判断方法。
// 设计取舍：3、在一个模块工厂实例中，是否允许两个路径一样的模块（类加载器不同）
// 设计取舍：不允许，这样会造成系统过于复杂。应当新建一个新的工厂实例。
// 但是不同工厂的NAME_SPACE实例属性可以相同，使用NAME_SPACE实现资源隔离。使用版本号实现模块路径隔离。
// TODO 测试现在的加载逻辑 已经完成
// TODO 编写顶层模块的load方法 已经完成
// TODO 优化ModuleFactory中moduleInfoMap的存储，优化ModuleInfo的dependencyList存储
// TODO 在工厂中添加一个方法，模块能够通知模块已经完全加载完毕
// TODO 当模块加载完毕后，如果有其他模块的依赖未满足，那么需要通知其他的模块
// TODO 模块工厂需要提供所有的模块均已加载完毕的事件。
// TODO 设计类加载器
// TODO 当模块加载完毕后，此模块所导出的对象的类型全部在另外一个类加载器中，那么记录此模块的此特性，在重新加载的时候，不需要更新其上层模块
// TODO 设计模块卸载的接口，以释放资源。
public class ModuleFactory {
    private Map<String, ModuleInfo> moduleInfoMap = new TreeMap<>();
    public void loadTopLevelModule(Class<? extends Module> moduleClass) {
        this.getModuleInfo(moduleClass);
    }

    private ModuleInfo getModuleInfo(Class<? extends Module> moduleClass) {
        String name = moduleClass.getName();
        ModuleVersion version = moduleClass.getDeclaredAnnotation(ModuleVersion.class);
        String v = "default";
        if(version != null) {
            v = version.value();
        }
        String key = name + ":" + v;
        ModuleInfo moduleInfo = moduleInfoMap.get(key);
        if(moduleInfo == null) {
            moduleInfo = new ModuleInfo(moduleClass);
            moduleInfoMap.put(key, moduleInfo);
            moduleInfo.registerModule(this);
        }
        return moduleInfo;
    }

    public final void require(
            Module module,
            Class<? extends Module> moduleClass
    ) {
        require(module, moduleClass, "default");
    }

    public final void require(
        Module module,
        Class<? extends Module> moduleClass,
        String... moduleNames
    ) {
        Dependency dependency = new Dependency(module, moduleNames);
        require(dependency, moduleClass);
    }

    public final void require(
        Dependency dependency,
        Class<? extends Module> moduleClass
    ) {
        ModuleInfo moduleInfo = getModuleInfo(moduleClass);
        moduleInfo.addDependency(dependency);

        Set<String> requireNames = dependency.getRequireNames();
        for(String name: requireNames) {
            if(moduleInfo.hasExport(name)) {
                dependency.getModule().onDependencyResolved(this, moduleClass, name);
            }
        }
    }

    public void export(String name, Object obj, Class<? extends Module> moduleClass) {
        ModuleInfo moduleInfo = getModuleInfo(moduleClass);
        moduleInfo.addExport(name, obj);

        List<Dependency> dependencyList = moduleInfo.getDependencyList();
        for(Dependency dependency: dependencyList) {
            if(dependency.getRequireNames().contains(name)) {
                dependency.getModule().onDependencyResolved(this, moduleClass, name);
            }
        }
     }

    public void export(Object obj, Class<? extends Module> moduleClass) {
        export("default", obj, moduleClass);
    }

    public <M> M getExport(Class<? extends Module> moduleClass, String name) {
        ModuleInfo moduleInfo = getModuleInfo(moduleClass);
        return  (M)moduleInfo.getExport(name);
    }

    public void setModudleLoaded(Module modudleInstance) {
        Class<? extends Module> moduleClass = modudleInstance.getClass();
        ModuleInfo moduleInfo = getModuleInfo(moduleClass);
        // 查看是否有未满足的依赖
        moduleInfo.setModuleState(ModuleState.RUNNING);
    }
}
