package me.maxwell.asyncmodule;

public interface Module {
    /**
     * 向模块工厂注册模块，可以在此方法内设置模块依赖
     * @param moduleInfo 本模块的模块信息
     */
    void register(ModuleInfo moduleInfo);

    /**
     * 模块依赖满足事件，A依赖B模块的一个名为C的对象，当B模块导出C对象时，A模块的此方法将被调用
     * @param moduleInfo 本模块的模块信息
     * @param requireModuleInfo 依赖的模块的模块信息
     * @param name 依赖名
     */
    default void onRequireResolved(
            ModuleInfo moduleInfo,
            ModuleInfo requireModuleInfo,
            String name
    ) {}

    /**
     * 模块依赖未满足事件，A依赖B模块的一个名为C的对象，当B模块已经加载完毕时，还没有导出C对象，那么会调用A模块的此方法
     * @param moduleInfo 本模块的模块信息
     * @param requireModuleInfo 依赖的模块的模块信息
     * @param name 依赖名
     */
    default void onRequireMissed(
            ModuleInfo moduleInfo,
            ModuleInfo requireModuleInfo,
            String name
    ) {}

    /**
     * 模块依赖重新加载事件，A依赖B模块的一个名为C的对象，当B模块被重新加载时，如果重新加载的B模块导出了名为C的对象，那么此方法将会被调用
     * @param moduleInfo 本模块的模块信息
     * @param requireModuleInfo 依赖的模块的模块信息
     * @param name 依赖名
     */
    default void onReloadRequireResolved(
            ModuleInfo moduleInfo,
            ModuleInfo requireModuleInfo,
            String name
    ) {}

    /**
     * 模块依赖重新加载事件，A依赖B模块的一个名为C的对象，当B模块被重新加载时，如果重新加载的B模块没有导出名为C的对象，那么此方法将会被调用
     * @param moduleInfo 本模块的模块信息
     * @param requireModuleInfo 依赖的模块的模块信息
     * @param name 依赖名
     */
    default void onReloadRequireMissed(
            ModuleInfo moduleInfo,
            ModuleInfo requireModuleInfo,
            String name
    ) {}
}
