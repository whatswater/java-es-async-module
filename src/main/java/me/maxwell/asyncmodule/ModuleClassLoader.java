package me.maxwell.asyncmodule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ModuleClassLoader extends ClassLoader {
    private Map<String, String> versionMap;
    private ClassLoaderFinder finder;

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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len = -1;
        byte[] buff = new byte[1024*4];
        try {
            while((len = is.read(buff)) != -1) {
                baos.write(buff, 0, len);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(name);
        }
        byte[] data = baos.toByteArray();
        return defineClass(name, data, 0, data.length);
    }

    public void setFinder(ClassLoaderFinder finder) {
        this.finder = finder;
    }

    public void setVersionMap(Map<String, String> versionMap) {
        this.versionMap = versionMap;
    }
}
