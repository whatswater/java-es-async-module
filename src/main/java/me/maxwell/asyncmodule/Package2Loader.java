package me.maxwell.asyncmodule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Package2Loader {
    public static Package2Loader of(String packageName, ModuleClassLoader moduleClassLoader, Package2Loader parent) {
        return (new Parser(packageName, moduleClassLoader)).parse(parent);
    }

    public static Package2Loader findParent(Package2Loader child) {
        Package2Loader config = child.getParent();
        while(config != null) {
            ModuleClassLoader classLoader = config.getModuleClassLoader();
            if(classLoader != null) {
                break;
            }
            config = config.getParent();
        }
        return config;
    }

    public static ModuleClassLoader findClassLoader(String packageName, Map<String, Package2Loader> package2Loaders) {
        String[] packageNames = packageName.split("\\.");
        int level = 0;
        Package2Loader package2Loader = package2Loaders.get(packageNames[level]);
        if(package2Loader == null) {
            return null;
        }

        String name = packageNames[++level];
        while(package2Loader.hasChild(name)) {
            package2Loader = package2Loader.getChild(name);
            name = packageNames[++level];
        }

        ModuleClassLoader ret = package2Loader.getModuleClassLoader();
        if(ret != null) {
            return ret;
        }

        Package2Loader parent = Package2Loader.findParent(package2Loader);
        return parent == null ? null : parent.getModuleClassLoader();
    }

    private String prefix;
    private ModuleClassLoader moduleClassLoader;
    private Map<String, Package2Loader> children;
    private Package2Loader parent;

    public String getPrefix() {
        return prefix;
    }

    public ModuleClassLoader getModuleClassLoader() {
        return moduleClassLoader;
    }
    public void setModuleClassLoader(ModuleClassLoader moduleClassLoader) {
        this.moduleClassLoader = moduleClassLoader;
    }

    public boolean hasChild(String name) {
        return children != null && children.containsKey(name);
    }

    public Package2Loader getChild(String name) {
        return children.get(name);
    }

    public void putChild(String name, Package2Loader config) {
        children.put(name, config);
    }

    public Map<String, Package2Loader> getChildren() {
        return children;
    }

    public Package2Loader getParent() {
        return this.parent;
    }

    public void setParent(Package2Loader parent) {
        this.parent = parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public static class Parser {
        private final String packageName;
        private int index;
        private ModuleClassLoader moduleClassLoader;

        public Parser(String packageName, ModuleClassLoader moduleClassLoader) {
            if(packageName == null || packageName.length() == 0) {
                throw new RuntimeException();
            }
            this.packageName = packageName;
            this.index = 0;
            this.moduleClassLoader = moduleClassLoader;
        }

        public Package2Loader parse(Package2Loader parent) {
            String token = getNextToken();
            Package2Loader nameGroup = new Package2Loader();
            nameGroup.prefix = token;
            nameGroup.parent = parent;

            if(!hasChar()) {
                nameGroup.moduleClassLoader = moduleClassLoader;
                return nameGroup;
            }

            char start = getCurrentChar();
            if(start == '|' || start == '}') {
                nameGroup.moduleClassLoader = moduleClassLoader;
                return nameGroup;
            }
            if(start == '.') {
                next();
                Package2Loader child = parse(nameGroup);
                nameGroup.children = new TreeMap<>();
                nameGroup.children.put(child.prefix, child);
                return nameGroup;
            }
            if(start == '{') {
                List<Package2Loader> childrenList = parseChildrenList(nameGroup);
                nameGroup.children = new TreeMap<>();
                for(Package2Loader name2ClassLoader: childrenList) {
                    nameGroup.children.put(name2ClassLoader.prefix, name2ClassLoader);
                }
                return nameGroup;
            }
            throw new RuntimeException();
        }

        private List<Package2Loader> parseChildrenList(Package2Loader parent) {
            char start = getCurrentCharAndMoveToNext();
            if(start != '{') {
                throw new RuntimeException();
            }

            List<Package2Loader> nameGroups = new ArrayList<>();
            while(hasChar()) {
                nameGroups.add(parse(parent));
                char c = getCurrentCharAndMoveToNext();
                if(c == '}') {
                    break;
                }
            }
            return nameGroups;
        }

        private String getNextToken() {
            StringBuilder stringLiteral = new StringBuilder();
            while(hasChar()) {
                char c = getCurrentCharAndMoveToNext();
                if(c == '.' || c == '|' || c == '{' || c == '}') {
                    back();
                    break;
                }
                stringLiteral.append(c);
            }
            return stringLiteral.toString();
        }

        private boolean hasChar() {
            return index < packageName.length();
        }
        private char getCurrentCharAndMoveToNext() {
            return packageName.charAt(index++);
        }
        private char getCurrentChar() {
            return packageName.charAt(index);
        }
        private void back() {
            index = index - 1;
        }
        private void next() {
            index = index + 1;
        }
    }
}
