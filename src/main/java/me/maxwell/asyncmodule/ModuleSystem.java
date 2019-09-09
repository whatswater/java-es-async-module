package me.maxwell.asyncmodule;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class ModuleSystem {
    public ModuleFactory load(String moduleClassName, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        return load(moduleClassName, ModuleFactory.DEFAULT_VERSION, config);
    }

    private ClassLoaderFactory classLoaderFactory;
    private ModuleFactory moduleFactory;

    public static void main(String[] args) throws IOException {
        ModuleSystem moduleSystem = new ModuleSystem();
        moduleSystem.startServer();
    }

    public void startServer() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel ss = ServerSocketChannel.open();
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 9527);
        ss.bind(hostAddress);
        ss.configureBlocking(false);
        ss.register(selector, ss.validOps());

        while(true) {
            int n = selector.select();
            if(n == 0) {
                continue;
            }
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while(it.hasNext()) {
                SelectionKey key = it.next();

                if(key.isAcceptable()) {
                    SocketChannel client = ss.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                }
                else if(key.isReadable()) {
                    SelectableChannel channel = key.channel();
                    if(channel instanceof SocketChannel) {
                        SocketChannel socketChannel = (SocketChannel) channel;
                        ByteBuffer buffer = ByteBuffer.allocate(256);
                        socketChannel.read(buffer);

                        System.out.print(new String(buffer.array()));
                        buffer.flip();
                        socketChannel.write(buffer);
                    }
                }
                it.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public ModuleFactory load(String moduleClassName, String version, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        ClassLoaderFactory classLoaderFactory = new ClassLoaderFactory(config);
        Class<?> cls = classLoaderFactory.loadClass(moduleClassName, version);
        if(!Module.class.isAssignableFrom(cls)) {
            System.out.println(cls.getClassLoader());
            System.out.println(cls.getClassLoader().getParent());
            System.out.println(Module.class.getClassLoader());

            throw new ModuleSystemException("The cls: " + cls.getName() + " must implements Module Interface when loading");
        }

        Class<? extends Module> moduleClass = (Class<? extends Module>) cls;
        ModuleFactory moduleFactory = new ModuleFactory(new ModuleLoadedListener() {
            @Override
            public void onModuleLoaded(ModuleInfo moduleInfo, ModuleFactory factory) {
                System.out.println(moduleInfo.getModuleName() + " loaded");
            }

            @Override
            public void onAllModuleLoaded(ModuleFactory factory) {
                System.out.println("moduleFactory loaded");
            }
        });
        moduleFactory.getModuleInfo(moduleClass);

        this.classLoaderFactory = classLoaderFactory;
        this.moduleFactory = moduleFactory;

        return moduleFactory;
    }

    @SuppressWarnings("unchecked")
    public void reloadModule(String moduleClassName, String version, Map<String, ClassLoaderBuilder> config) throws ClassNotFoundException {
        if(!moduleFactory.isLoaded()) {
            return;
        }

        ModuleClassLoader loader = classLoaderFactory.find(moduleClassName, version);
        Set<ModuleClassLoader> reloads = loader.getReloadModules();
        List<String> moduleNames = getModuleNames(reloads);

        ClassLoaderFactoryChain newFinder = new ClassLoaderFactoryChain(config, classLoaderFactory, reloads);
        ModuleFactoryChain newFactory = new ModuleFactoryChain(moduleFactory, newFinder, new ModuleLoadedListener() {
            @Override
            public void onModuleLoaded(ModuleInfo moduleInfo, ModuleFactory factory) {
                System.out.println(moduleInfo.getModuleName() + " loaded");
            }

            @Override
            public void onAllModuleLoaded(ModuleFactory factory) {
                System.out.println("moduleFactory loaded");
                newFinder.merge();
                for(ModuleClassLoader classLoader: reloads) {
                    classLoader.unListen(reloads);
                }
                if(factory instanceof ModuleFactoryChain) {
                    ((ModuleFactoryChain) factory).merge();
                }
                System.out.println("12312312");
            }
        });

        for(String moduleName: moduleNames) {
            String thisVersion = moduleName.split(ModuleFactory.VERSION_SPLIT)[0];
            String className = moduleName.split(ModuleFactory.VERSION_SPLIT)[1];
            Class<? extends Module> moduleClass = (Class<? extends Module>)newFinder.loadClass(className, thisVersion);
            newFactory.getModuleInfo(moduleClass);
        }
    }

    public ModuleFactory getModuleFactory() {
        return this.moduleFactory;
    }
    public ClassLoaderFactory getClassLoaderFactory() {
        return this.classLoaderFactory;
    }

    private List<String> getModuleNames(Set<ModuleClassLoader> reloads) {
        List<String> moduleNames = new ArrayList<>();
        for(ModuleClassLoader classLoader: reloads) {
            for(Class<? extends Module> moduleClass: classLoader.getModuleClassList()) {
                moduleNames.add(moduleFactory.getModuleName(moduleClass));
            }
        }
        return moduleNames;
    }
}
