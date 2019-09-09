package me.maxwell.asyncmodule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class OldVersionClassLoader extends ModuleClassLoader {
    public OldVersionClassLoader(String name, ClassLoaderFactory factory) {
        super(name, factory);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> cls = findLoadedClass(name);
        if(cls != null) {
            return cls;
        }

        InputStream is = this.getClass().getResourceAsStream("/old_version_classes/" + name.substring(name.lastIndexOf('.') + 1) + ".class");
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
}
