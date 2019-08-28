package me.maxwell.asyncmodule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    public static final String VERSION_SPLIT = ":";

    private Map<String, ModuleInfo> moduleInfoMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lock1 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lock2 = new ConcurrentHashMap<>();
    private int count = 0;
    private volatile boolean loaded = false;
    private ModuleLoadedListener moduleLoadedListener;

    public ModuleFactory() {

    }

    public ModuleFactory(ModuleLoadedListener listener) {
        this.moduleLoadedListener = listener;
    }

    public String getModuleName(Class<? extends Module> moduleClass) {
        String name = moduleClass.getName();
        ModuleVersion version = moduleClass.getDeclaredAnnotation(ModuleVersion.class);
        String v = DEFAULT_VERSION;
        if(version != null) {
            v = version.value();
        }
        return v + VERSION_SPLIT + name;
    }

    /**
     * 获取模块信息，如果有其他地方需要向moduleInfoMap中添加数据，需要和此处共用一个锁，才可以保证一致性
     * @param moduleClass 模块类
     * @return
     */
    public ModuleInfo getModuleInfo(Class<? extends Module> moduleClass) {
        String key = getModuleName(moduleClass);
        ModuleInfo moduleInfo = moduleInfoMap.get(key);
        if(moduleInfo == null) {
            synchronized(getLock1(key)) {
                if(moduleInfoMap.get(key) == null) {
                    moduleInfo = new ModuleInfo(moduleClass, this);
                    moduleInfoMap.put(key, moduleInfo);
                    count++;
                }
            }
        }
        return moduleInfo;
    }

    /**
     * 将模块设置为运行状态
     * 注意：如果有其他地方能够更改moduleInfo的状态，需要和此处共用一个锁，才可以保证一致性
     * @param moduleClass 模块类
     */
    public void setModuleLoaded(Class<? extends Module> moduleClass) {
        String key = getModuleName(moduleClass);
        ModuleInfo moduleInfo = moduleInfoMap.get(key);
        if(ModuleState.LOADING  == moduleInfo.getModuleState()) {
            synchronized(getLock2(key)) {
                if(ModuleState.LOADING  == moduleInfo.getModuleState()) {
                    moduleInfo.setModuleState(ModuleState.RUNNING);
                    count--;
                    if(count == 0) {
                        loaded = true;
                    }

                    if(moduleLoadedListener != null) {
                        moduleLoadedListener.onModuleLoaded(moduleInfo, this);
                        if(loaded) {
                            moduleLoadedListener.onAllModuleLoaded(this);
                        }
                    }
                }
            }
        }
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

    public boolean isLoaded() {
        return this.loaded;
    }

    public Map<String, ModuleInfo> getModuleInfoMap() {
        return this.moduleInfoMap;
    }

    protected Object getLock1(String modulePath) {
        Object newLock = new Object();
        Object lock = lock1.putIfAbsent(modulePath, newLock);
        if (lock == null) {
            lock = newLock;
        }
        return lock;
    }
    protected Object getLock2(String modulePath) {
        Object newLock = new Object();
        Object lock = lock2.putIfAbsent(modulePath, newLock);
        if (lock == null) {
            lock = newLock;
        }
        return lock;
    }
}
