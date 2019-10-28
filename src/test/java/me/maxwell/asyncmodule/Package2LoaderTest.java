package me.maxwell.asyncmodule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

public class Package2LoaderTest {
    @Test
    public void test() {
        String packageName = "me.whatswater.document{common|util|command}";
        ClassLoaderFactory classLoaderFactory = new ClassLoaderFactory(null);
        ModuleClassLoader classLoader1 = new ModuleClassLoader("common", classLoaderFactory);
        ModuleClassLoader classLoader2 = new ModuleClassLoader("test", classLoaderFactory);

        Package2Loader me = Package2Loader.of(packageName, classLoader1, null);
        Assertions.assertEquals("me", me.getPrefix());
        Assertions.assertNull(me.getModuleClassLoader());
        Assertions.assertNull(me.getParent());

        Package2Loader whatswater = me.getChild("whatswater");
        Assertions.assertNotNull(whatswater);
        Assertions.assertEquals("whatswater", whatswater.getPrefix());
        Assertions.assertNull(whatswater.getModuleClassLoader());
        Assertions.assertSame(me, whatswater.getParent());

        Package2Loader document = whatswater.getChild("document");
        Assertions.assertNotNull(document);
        Assertions.assertEquals("document", document.getPrefix());
        Assertions.assertNull(document.getModuleClassLoader());
        Assertions.assertSame(whatswater, document.getParent());

        Package2Loader common = document.getChild("common");
        Assertions.assertNotNull(common);
        Assertions.assertEquals("common", common.getPrefix());
        Assertions.assertSame(classLoader1, common.getModuleClassLoader());
        Assertions.assertSame(document, common.getParent());

        Package2Loader util = document.getChild("util");
        Assertions.assertNotNull(util);
        Assertions.assertEquals("util", util.getPrefix());
        Assertions.assertSame(classLoader1, util.getModuleClassLoader());
        Assertions.assertSame(document, util.getParent());

        Package2Loader command = document.getChild("command");
        Assertions.assertNotNull(command);
        Assertions.assertEquals("command", command.getPrefix());
        Assertions.assertSame(classLoader1, command.getModuleClassLoader());
        Assertions.assertSame(document, command.getParent());

        me.setModuleClassLoader(classLoader2);
        Assertions.assertSame(me, Package2Loader.findParent(document));
        Assertions.assertSame(me, Package2Loader.findParent(command));

        Map<String, Package2Loader> package2Loaders = new TreeMap<>();
        package2Loaders.put("me", me);
        Assertions.assertSame(classLoader1, Package2Loader.findClassLoader("me.whatswater.document.util.StringUtils", package2Loaders));
        Assertions.assertSame(classLoader2, Package2Loader.findClassLoader("me.whatswater.document.xx1.it", package2Loaders));
    }
}
