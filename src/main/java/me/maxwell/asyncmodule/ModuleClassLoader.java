package me.maxwell.asyncmodule;

import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class ModuleClassLoader extends ClassLoader implements Comparable<ModuleClassLoader> {
    private final String name;
    private Map<String, String> versionMap;
    private ClassLoaderFactory finder;
    @Getter
    private List<Class<? extends Module>> moduleClassList;
    @Getter
    private Set<ModuleClassLoader> reloadListeners = new ConcurrentSkipListSet<>();

    public ModuleClassLoader(ClassLoader parent, String name) {
        super(parent);
        if(name == null || name.length() == 0) {
            throw new RuntimeException("ModuleClassLoader's name must not null or empty");
        }
        this.name = name;
    }

    public ModuleClassLoader findClassLoader(String name) {
        String version = ModuleFactory.DEFAULT_VERSION;
        if(versionMap != null && versionMap.containsKey(name)) {
            version = versionMap.get(name);
        }
        return finder.find(version + ModuleFactory.VERSION_SPLIT + name);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            ModuleClassLoader classLoader = findClassLoader(name);
            Class<?> cls;
            if(classLoader != null) {
                cls = classLoader.findClass(name);
                if(classLoader != this) {
                    classLoader.addReloadListener(this);
                }
            }
            else {
                cls = getParent().loadClass(name);
            }
            if(resolve) {
                resolveClass(cls);
            }
            return cls;
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> cls = findLoadedClass(name);
        if(cls != null) {
            return cls;
        }
        InputStream is = this.getClass().getResourceAsStream("/" + name.replace(".", "/") + ".class");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int len;
        byte[] buff = new byte[1024*4];
        try {
            while((len = is.read(buff)) != -1) {
                os.write(buff, 0, len);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(name);
        }
        byte[] data = os.toByteArray();

        Class<?> newCls = defineClass(name, data, 0, data.length);
        if(Module.class.isAssignableFrom(newCls)) {
            addModuleClass((Class<Module>) newCls);
        }
        return newCls;
    }

    public void addModuleClass(Class<? extends Module> moduleClass) {
        if(this.moduleClassList == null) {
            this.moduleClassList = new ArrayList<>();
        }
        this.moduleClassList.add(moduleClass);
    }

    public void addReloadListener(ModuleClassLoader moduleClassLoader) {
        this.reloadListeners.add(moduleClassLoader);
    }

    public void setFinder(ClassLoaderFactory finder) {
        this.finder = finder;
    }

    public void setVersionMap(Map<String, String> versionMap) {
        this.versionMap = versionMap;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public int compareTo(ModuleClassLoader o) {
        if(o == null) {
            return 1;
        }
        return this.name.compareTo(o.name);
    }
}
