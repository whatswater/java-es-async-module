package me.maxwell.asyncmodule;

import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleClassLoader extends ClassLoader {
    private final static String V = "what'swater";

    @Getter
    private final String name;
    private Map<String, String> versionMap;
    private ClassLoaderFactory finder;
    private List<Class<? extends Module>> moduleClassList;
    private Map<ModuleClassLoader, Object> listeners = new ConcurrentHashMap<>();
    private Map<ModuleClassLoader, Object> requires = new ConcurrentHashMap<>();

    public ModuleClassLoader(String name, ClassLoaderFactory factory) {
        this(name, factory, null);
    }

    public ModuleClassLoader(String name, ClassLoaderFactory factory, Map<String, String> versionMap) {
        super(Module.class.getClassLoader());
        if(name == null || name.length() == 0) {
            throw new RuntimeException("ModuleClassLoader's name must not null or empty");
        }
        this.name = name;
        this.finder = factory;
        this.versionMap = versionMap;
    }

    public ModuleClassLoader findClassLoader(String className) {
        String version = ModuleFactory.DEFAULT_VERSION;
        if(versionMap != null && versionMap.containsKey(className)) {
            version = versionMap.get(className);
        }
        return finder.find(className, version);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            ModuleClassLoader classLoader = findClassLoader(name);
            Class<?> cls;
            if(classLoader != null) {
                cls = classLoader.findClass(name);
                if(classLoader != this) {
                    classLoader.listeners.put(this, V);
                    requires.put(classLoader, V);
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

    @SuppressWarnings("unchecked")
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

    public void unListen(Set<ModuleClassLoader> except) {
        for(ModuleClassLoader moduleClassLoader: requires.keySet()) {
            if(except.contains(moduleClassLoader)) {
                continue;
            }
            moduleClassLoader.listeners.remove(this);
        }
    }

    public Set<ModuleClassLoader> getReloadModules() {
        Set<ModuleClassLoader> reloads = new HashSet<>();
        Stack<Iterator<ModuleClassLoader>> stack = new Stack<>();
        ModuleClassLoader loader = this;

        reloads.add(loader);
        if(!loader.listeners.isEmpty()) {
            stack.push(loader.listeners.keySet().iterator());
            while(true) {
                Iterator<ModuleClassLoader> it = stack.lastElement();
                if(it.hasNext()) {
                    ModuleClassLoader loader1 = it.next();
                    reloads.add(loader1);
                    if(loader1.listeners == null || loader1.listeners.isEmpty()) {
                        continue;
                    }
                    stack.push(loader1.listeners.keySet().iterator());
                }
                else if(stack.size() > 1) {
                    stack.pop();
                }
                else {
                    break;
                }
            }
        }
        return reloads;
    }

    public List<Class<? extends Module>> getModuleClassList() {
        return moduleClassList;
    }

    public void resetFactory(ClassLoaderFactory factory) {
        this.finder = factory;
    }
}
