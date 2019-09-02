package me.maxwell.asyncmodule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// 模块本身是否存在，根据类加载器确定；模块是否导出了某个对象，由模块系统支持
// 不同工厂的NAME_SPACE实例属性可以相同，使用NAME_SPACE实现资源隔离。
// 使用版本号实现模块路径隔离。
// TODO getExport的时候添加泛型
// TODO 设计模块卸载的接口，以释放资源。
// TODO 论证整个系统的多线程安全性
// TODO 优化代码的结构，命名和性能
// TODO 充分测试各个地方的代码
// TODO 编写模块系统cli
public class ModuleFactory {
    public static final String DEFAULT_VERSION = "default";
    public static final String VERSION_SPLIT = ":";
    public static final String DEFAULT_NAME = "default";

    private Map<String, ModuleInfo> moduleInfoMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lock2 = new ConcurrentHashMap<>();
    private int count = 0;
    private boolean loaded = false;
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
     * 获取模块信息，如果没有会创建
     * @param moduleClass 模块类
     * @return
     */
    public ModuleInfo getModuleInfo(final Class<? extends Module> moduleClass) {
        String key = getModuleName(moduleClass);

        return moduleInfoMap.computeIfAbsent(key, new Function<String, ModuleInfo>() {
            @Override
            public ModuleInfo apply(String s) {
                ModuleInfo newModuleInfo = new ModuleInfo(moduleClass, ModuleFactory.this);
                count++;
                newModuleInfo.getModuleInstance().register(newModuleInfo);
                newModuleInfo.moduleState = ModuleState.LOADING;
                return newModuleInfo;
            }
        });
    }

    /**
     * 根据moduleName查找模块信息
     * @param moduleName 模块名
     * @return
     */
    public ModuleInfo findModuleInfo(String moduleName) {
        return moduleInfoMap.get(moduleName);
    }

    /**
     * 将模块设置为运行状态
     * 注意：如果有其他地方能够更改moduleInfo的状态，需要和此处共用一个锁，才可以保证一致性
     * @param moduleName 模块名称
     */
    public void setModuleLoaded(String moduleName) {
        ModuleInfo moduleInfo = moduleInfoMap.get(moduleName);
        if(ModuleState.LOADING  == moduleInfo.getModuleState()) {
            synchronized(getLock2(moduleName)) {
                if(ModuleState.LOADING  == moduleInfo.getModuleState()) {
                    moduleInfo.moduleState = ModuleState.RUNNING;
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

    public boolean isLoaded() {
        return this.loaded;
    }

    public Map<String, ModuleInfo> getModuleInfoMap() {
        return this.moduleInfoMap;
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
