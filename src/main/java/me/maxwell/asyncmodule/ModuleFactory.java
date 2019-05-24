package me.maxwell.asyncmodule;


import org.jdeferred2.Deferred;
import org.jdeferred2.DeferredManager;
import org.jdeferred2.Promise;
import org.jdeferred2.impl.DefaultDeferredManager;
import org.jdeferred2.impl.DeferredObject;
import org.jdeferred2.multiple.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleFactory {
    private Map<Class<? extends Module>, ModuleInfo> moduleInfoMap = new ConcurrentHashMap<>();

    private ModuleInfo getModuleInfo(Class<? extends Module> moduleClass) {
        ModuleInfo moduleInfo = moduleInfoMap.get(moduleClass);
        if(moduleInfo == null) {
            moduleInfo = new ModuleInfo(moduleClass);
            moduleInfoMap.put(moduleClass, moduleInfo);
            moduleInfo.getModuleInstance().register(this);
        }

        return moduleInfo;
    }

    public final <M> Promise<M, ModuleSystemException, Void> require(
        Class<? extends Module> moduleClass,
        Class<M> moduleType
    ) {
        return require(moduleClass, "default", moduleType);
    }

    public final <M extends Module> Promise<M, ModuleSystemException, Void> require(
        Class<M> moduleClass
    ) {
        return require(moduleClass, "default", moduleClass);
    }

    public final <M> Promise<M, ModuleSystemException, Void> require(
        String moduleName,
        Class<M> moduleType
    ) {
        return require(GlobalModule.class, moduleName, moduleType);
    }

    public final <M> Promise<M, ModuleSystemException, Void> require(
        Class<? extends Module> moduleClass,
        String moduleName,
        Class<M> moduleType
    ) {
        Dependency<M> dependency = Dependency.of(moduleName, moduleType);
        return require(moduleClass, dependency);
    }

    public final <M> Promise<M, ModuleSystemException, Void> require(
        Dependency<M> dependency
    ) {
        return require(GlobalModule.class, dependency);
    }

    public final <M> Promise<M, ModuleSystemException, Void> require(
        Class<? extends Module> moduleClass,
        Dependency<M> dependency
    ) {
        Deferred<M, ModuleSystemException, Void> deferred = new DeferredObject<>();
        this.registerDependency(dependency, deferred, moduleClass);

        return deferred.promise();
    }

    public final <M, N> Promise<MultipleResults2<M,N>, OneReject<ModuleSystemException>, MasterProgress> require(
        Dependency<M> dependency1,
        Dependency<N> dependency2
    ) {
        return require(GlobalModule.class, dependency1, dependency2);
    }

    public final <M, N> Promise<MultipleResults2<M,N>, OneReject<ModuleSystemException>, MasterProgress> require(
        Class<? extends Module> moduleClass,
        Dependency<M> dependency1,
        Dependency<N> dependency2
    ) {
        Promise<M, ModuleSystemException, Void> promise1 = require(moduleClass, dependency1);
        Promise<N, ModuleSystemException, Void> promise2 = require(moduleClass, dependency2);

        DeferredManager dm = new DefaultDeferredManager();
        return dm.when(promise1, promise2);
    }

    public final <M, N, L> Promise<MultipleResults3<M,N,L>, OneReject<ModuleSystemException>, MasterProgress> require(
        Dependency<M> dependency1,
        Dependency<N> dependency2,
        Dependency<L> dependency3
    ) {
        return require(GlobalModule.class, dependency1, dependency2, dependency3);
    }

    public final <M, N, L> Promise<MultipleResults3<M,N,L>, OneReject<ModuleSystemException>, MasterProgress> require(
        Class<? extends Module> moduleClass,
        Dependency<M> dependency1,
        Dependency<N> dependency2,
        Dependency<L> dependency3
    ) {
        Promise<M, ModuleSystemException, Void> promise1 = require(moduleClass, dependency1);
        Promise<N, ModuleSystemException, Void> promise2 = require(moduleClass, dependency2);
        Promise<L, ModuleSystemException, Void> promise3 = require(moduleClass, dependency3);

        DeferredManager dm = new DefaultDeferredManager();
        return dm.when(promise1, promise2, promise3);
    }

    public final <M, N, L, K> Promise<MultipleResults4<M,N,L, K>, OneReject<ModuleSystemException>, MasterProgress> require(
        Dependency<M> dependency1,
        Dependency<N> dependency2,
        Dependency<L> dependency3,
        Dependency<K> dependency4
    ) {
        return require(GlobalModule.class, dependency1, dependency2, dependency3, dependency4);
    }

    public final <M, N, L, K> Promise<MultipleResults4<M,N,L, K>, OneReject<ModuleSystemException>, MasterProgress> require(
        Class<? extends Module> moduleClass,
        Dependency<M> dependency1,
        Dependency<N> dependency2,
        Dependency<L> dependency3,
        Dependency<K> dependency4
    ) {
        Promise<M, ModuleSystemException, Void> promise1 = require(moduleClass, dependency1);
        Promise<N, ModuleSystemException, Void> promise2 = require(moduleClass, dependency2);
        Promise<L, ModuleSystemException, Void> promise3 = require(moduleClass, dependency3);
        Promise<K, ModuleSystemException, Void> promise4 = require(moduleClass, dependency4);

        DeferredManager dm = new DefaultDeferredManager();
        return dm.when(promise1, promise2, promise3, promise4);
    }

    public void export(String name, Object obj, Class<? extends Module> moduleClass) {
        ModuleInfo moduleInfo = getModuleInfo(moduleClass);
        moduleInfo.addExport(name, obj);
    }

    public void export(Object obj, Class<? extends Module> moduleClass) {
        ModuleInfo moduleInfo = getModuleInfo(moduleClass);
        moduleInfo.addExport("default", obj);
    }

    public void export(String name, Object obj) {
        export(name, obj, GlobalModule.class);
    }

    <M> void registerDependency(
        Dependency<M> dependency,
        Deferred<M, ModuleSystemException, Void> deferred,
        Class<? extends Module> from) {

        ModuleInfo moduleInfo = getModuleInfo(from);

        if(moduleInfo.hasExport(dependency.getName())) {
            deferred.resolve((M)moduleInfo.getExport(dependency.getName()));
        }
        else {
            moduleInfo.addDependency(
                dependency.getName(),
                (Deferred<Object, ModuleSystemException, Void>) deferred
            );
        }
    }
}
