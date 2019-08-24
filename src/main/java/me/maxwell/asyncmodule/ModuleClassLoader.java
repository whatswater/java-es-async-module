package me.maxwell.asyncmodule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ModuleClassLoader extends ClassLoader {
    private Map<String, String> versionMap;
    private ClassLoaderFinder finder;
    private List<Class<? extends Module>> moduleClassList;
    private Set<ModuleClassLoader> dependencys;

    public ModuleClassLoader(ClassLoader parent) {
        super(parent);
    }

    public ModuleClassLoader findClassLoader(String name) {
        String version = ModuleFactory.DEFAULT_VERSION;
        if(versionMap != null && versionMap.containsKey(name)) {
            version = versionMap.get(name);
        }
        return finder.find(name, version);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            ModuleClassLoader classLoader = findClassLoader(name);
            Class<?> cls;
            if(classLoader != null) {
                cls = classLoader.findClass(name);
                classLoader.addDependency(this);
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

    @SuppressWarnings("unchecked")
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> cls = findLoadedClass(name);
        if(cls != null) {
            return cls;
        }
        InputStream is = this.getClass().getResourceAsStream("/" + name.replace(".", "/") + ".class");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len;
        byte[] buff = new byte[1024*4];
        try {
            while((len = is.read(buff)) != -1) {
                baos.write(buff, 0, len);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(name);
        }
        byte[] data = baos.toByteArray();

        Class<?> newCls = defineClass(name, data, 0, data.length);
        if(Module.class.isAssignableFrom(newCls)) {
            addModuleClass((Class<? extends Module>) newCls);
        }
        return newCls;
    }

    public void addModuleClass(Class<? extends Module> moduleClass) {
        if(this.moduleClassList == null) {
            this.moduleClassList = new ArrayList<>();
        }
        this.moduleClassList.add(moduleClass);
    }

    public void addDependency(ModuleClassLoader moduleClassLoader) {
        if(this.dependencys == null) {
            this.dependencys = new HashSet<>();
        }
        this.dependencys.add(moduleClassLoader);
    }

    public void setFinder(ClassLoaderFinder finder) {
        this.finder = finder;
    }

    public void setVersionMap(Map<String, String> versionMap) {
        this.versionMap = versionMap;
    }
}
