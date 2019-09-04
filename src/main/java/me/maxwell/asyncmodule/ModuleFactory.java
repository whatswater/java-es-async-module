package me.maxwell.asyncmodule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// 模块本身是否存在，根据类加载器确定；模块是否导出了某个对象，由模块系统支持
// 不同工厂的NAME_SPACE实例属性可以相同，使用NAME_SPACE实现资源隔离。
// 使用版本号实现模块路径隔离。
// TODO getExport的时候添加泛型
// TODO 设计模块卸载的接口，以释放资源
// TODO 论证整个系统的多线程安全性，避免发生死锁(已经验证moduleFactory系统的安全性，还差classloader系统的)
// TODO 优化代码的结构，命名和性能
// TODO 编写文档、注释并充分测试各个地方的代码
// TODO 编写模块系统cli
public class ModuleFactory {
    public static final String DEFAULT_VERSION = "default";
    public static final String VERSION_SPLIT = ":";
    public static final String DEFAULT_NAME = "default";

    private final Map<String, ModuleInfo> moduleInfoMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lock1 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lock2 = new ConcurrentHashMap<>();
    private final AtomicInteger count = new AtomicInteger(0);
    private final ModuleLoadedListener moduleLoadedListener;

    public ModuleFactory() {
        this.moduleLoadedListener = null;
    }

    public ModuleFactory(ModuleLoadedListener listener) {
        this.moduleLoadedListener = listener;
    }

    /**
     * 根据moduleClass获取此模块的名字
     * @param moduleClass
     * @return
     */
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
        String moduleName = getModuleName(moduleClass);

        ModuleInfo moduleInfo = moduleInfoMap.get(moduleName);
        if(moduleInfo == null) {
            synchronized(getLock1(moduleName)) {
                ModuleInfo re = moduleInfoMap.get(moduleName);
                if(moduleInfoMap.get(moduleName) == null) {
                    moduleInfo = new ModuleInfo(moduleClass, moduleName, ModuleFactory.this);
                    count.getAndIncrement();
                    moduleInfoMap.put(moduleName, moduleInfo);

                    moduleInfo.moduleState = ModuleState.LOADING;
                    moduleInfo.getModuleInstance().register(moduleInfo);
                }
                else {
                    moduleInfo = re;
                }
            }
        }
        return moduleInfo;
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
     * 此方法有线程安全问题，moduleState是volatile变量，所以保证了同一个模块只会触发一次count--，
     * 但是count本身是int类型的，为了并发执行，此方法的锁是一个moduleName一个锁，count此时就会出现线程安全问题
     * 所以count要设置为AtomicInteger，用CAS去更新
     * 将模块设置为运行状态
     * 注意：如果有其他地方能够更改moduleInfo的状态，需要和此处共用一个锁，才可以保证一致性
     * @param moduleName 模块名称
     */
    public void setModuleLoaded(String moduleName) {
        ModuleInfo moduleInfo = moduleInfoMap.get(moduleName);
        if(ModuleState.LOADING  == moduleInfo.moduleState) {
            synchronized(getLock2(moduleName)) {
                if(ModuleState.LOADING  == moduleInfo.getModuleState()) {
                    moduleInfo.moduleState = ModuleState.RUNNING;
                    count.getAndDecrement();
                    if(moduleLoadedListener != null) {
                        moduleLoadedListener.onModuleLoaded(moduleInfo, this);
                        if(count.get() == 0) {
                            moduleLoadedListener.onAllModuleLoaded(this);
                        }
                    }
                }
            }
        }
    }

    /**
     * 向moduleClass代表的模块注册依赖
     * @param moduleClass 模块class
     * @param require 依赖
     * @return
     */
    public ModuleInfo require(Class<? extends Module> moduleClass, Require require) {
        ModuleInfo moduleInfo = getModuleInfo(moduleClass);
        moduleInfo.addDependency(require);
        return moduleInfo;
    }

    /**
     * 模块系统是否加载完毕
     * @return
     */
    public boolean isLoaded() {
        return this.count.get() == 0;
    }

    public Map<String, ModuleInfo> getModuleInfoMap() {
        return this.moduleInfoMap;
    }

    protected Object getLock1(String moduleName) {
        Object newLock = new Object();
        Object lock = lock1.putIfAbsent(moduleName, newLock);
        if (lock == null) {
            lock = newLock;
        }
        return lock;
    }
    protected Object getLock2(String moduleName) {
        Object newLock = new Object();
        Object lock = lock2.putIfAbsent(moduleName, newLock);
        if (lock == null) {
            lock = newLock;
        }
        return lock;
    }
}
