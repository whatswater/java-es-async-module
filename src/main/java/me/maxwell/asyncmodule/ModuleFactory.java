package me.maxwell.asyncmodule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// TODO 1、改写模块系统的加载方法，一次加载可以加载多个模块(已完成)
// TODO 2、将factory的getModuleInfo暴露出去，当作加载新模块的命令(已完成)
// TODO 3、将对加载器的几种操作单独写成方法（删除、新增、重新加载等），代替之前的传递一个config
// TODO 4、根据工作3，重构之前的重新加载模块方法
// TODO 5、编写模块卸载方法
// TODO 6、编写文档、注释并充分测试各个地方的代码
// TODO 7、改成多工程项目，编写模块系统cli，编写示例应用
public class ModuleFactory {
    public static final String DEFAULT_VERSION = "default";
    public static final String VERSION_SPLIT = ":";
    public static final String DEFAULT_NAME = "default";

    private final Map<String, ModuleInfo> moduleInfoMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lock1 = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lock2 = new ConcurrentHashMap<>();
    private final AtomicInteger count = new AtomicInteger(0);
    private final ModuleLoadedListener moduleLoadedListener;
    private final ClassLoaderFactory classLoaderFactory;

    public ModuleFactory(ClassLoaderFactory classLoaderFactory) {
        this(classLoaderFactory, null);
    }

    public ModuleFactory(ClassLoaderFactory classLoaderFactory, ModuleLoadedListener listener) {
        this.moduleLoadedListener = listener;
        this.classLoaderFactory = classLoaderFactory;
    }

    /**
     * 根据moduleClass获取此模块的名字
     * @param moduleClass 模块类型cls
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
     * @param moduleClass 模块类型cls
     * @return
     */
    public ModuleInfo getModuleInfo(final Class<? extends Module> moduleClass) {
        String moduleName = getModuleName(moduleClass);

        ModuleInfo moduleInfo = findModuleInfo(moduleName);
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
    public final ModuleInfo findModuleInfo(String moduleName) {
        return moduleInfoMap.get(moduleName);
    }

    /**
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
     * 判断模块类是否存在
     * @param className 类名
     * @param version 版本
     * @return
     */
    public boolean isModuleClassExist(String className, String version) {
        Class<?> cls;
        try {
            cls = classLoaderFactory.loadClass(className, version);
        } catch(ClassNotFoundException e) {
            return false;
        }
        return Module.class.isAssignableFrom(cls);
    }

    /**
     * 根据名字和版本require
     * @param className 类名
     * @param version 版本
     * @param require 依赖
     * @return
     */
    @SuppressWarnings("unchecked")
    public ModuleInfo require(String className, String version, Require require) {
        ModuleInfo moduleInfo = findModuleInfo(version + ModuleFactory.VERSION_SPLIT + className);
        if(moduleInfo != null) {
            moduleInfo.addDependency(require);
            return moduleInfo;
        }

        Class<?> cls;
        try {
            cls = classLoaderFactory.loadClass(className, version);
        } catch(ClassNotFoundException e) {
            throw new InstanceModuleException("The " + className + " with version: " + version + " can not be find", e);
        }

        if(!Module.class.isAssignableFrom(cls)) {
            throw new InstanceModuleException("The " + className + " must implements Module interface" + version, null);
        }

        return require((Class<? extends Module>)cls, require);
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
