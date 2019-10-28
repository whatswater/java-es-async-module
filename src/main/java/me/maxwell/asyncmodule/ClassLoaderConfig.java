package me.maxwell.asyncmodule;

import java.util.*;

public class ClassLoaderConfig {
    private Map<String, Map<String, Package2Loader>> versionList = new TreeMap<>();

    public ModuleClassLoader find(String packageName, String version) {
        Map<String, Package2Loader> package2Loaders = versionList.get(version);
        if(package2Loaders == null) {
            return null;
        }

        return Package2Loader.findClassLoader(packageName, package2Loaders);
    }

    /**
     * 添加或者替换类加载器配置，此方法不能删除类加载器配置，应当调用其他方法
     * @param packageName 包路径
     * @param version 版本
     * @param moduleClassLoader 类加载器路径
     */
    public void addConfig(String packageName, String version, ModuleClassLoader moduleClassLoader) {
        Package2Loader newConfig = Package2Loader.of(packageName, moduleClassLoader, null);
        String prefix = newConfig.getPrefix();

        Map<String, Package2Loader> configs = versionList.get(version);
        if(configs == null) {
            configs = new TreeMap<>();
            configs.put(prefix, newConfig);
            versionList.put(version, configs);
            return;
        }

        Package2Loader oldConfig = configs.get(prefix);
        if(oldConfig == null) {
            configs.put(prefix, newConfig);
            return;
        }

        Set<ModuleClassLoader> reloads = merge(oldConfig, newConfig);
        // TODO 获取依赖这些ModuleClassLoader的所有classLoader和ModuleInfo
        // 方法主体写在ModuleClassLoader的static方法中
    }

    // 此方法逻辑比较复杂，需要较好的测试和注释论证为什么结果是正确的
    public Set<ModuleClassLoader> merge(Package2Loader oldConfig, Package2Loader newConfig) {
        Set<ModuleClassLoader> reloads = new HashSet<>();
        List<Package2Loader> nodeList = new ArrayList<>();
        nodeList.add(oldConfig);
        nodeList.add(newConfig);

        List<Package2Loader> tmp = new ArrayList<>();
        while(!nodeList.isEmpty()) {
            for(int i = 0; i < nodeList.size(); i = i + 2) {
                Package2Loader package2Loader1 = nodeList.get(i);
                Package2Loader package2Loader2 = nodeList.get(i + 1);

                ModuleClassLoader loader2 = package2Loader2.getModuleClassLoader();
                final boolean needReplace = loader2 != null && loader2 != package2Loader1.getModuleClassLoader();
                if(needReplace) {
                    if(package2Loader1.getModuleClassLoader() == null) {
                        // 此处可以加上判断，判断parent的ModuleClassLoader是否加载过package2Loader1的packageName的类
                        // 如果没有，就不需要执行add方法
                        Package2Loader parent = Package2Loader.findParent(package2Loader1);
                        if(parent != null && parent.getModuleClassLoader() != null) {
                            reloads.add(parent.getModuleClassLoader());
                        }
                    }
                    else {
                        reloads.add(package2Loader1.getModuleClassLoader());
                    }
                }

                Map<String, Package2Loader> newChildren = package2Loader2.getChildren();
                for(Map.Entry<String, Package2Loader> entry: newChildren.entrySet()) {
                    String name = entry.getKey();
                    if(package2Loader1.hasChild(name)) {
                        tmp.add(package2Loader1.getChild(name));
                        tmp.add(entry.getValue());
                    }
                    else {
                        package2Loader1.putChild(name, entry.getValue());
                    }
                    entry.getValue().setParent(package2Loader1);
                }
            }
            nodeList = tmp;
            tmp = new ArrayList<>();
        }

        return reloads;
    }
}
