package me.maxwell.asyncmodule;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class ModuleSystemTest {
    public static ClassLoader configLoader;
    public static ClassLoader serviceLoader;
    public static ClassLoader testModuleLoader;

    public static class Config implements Module {
        @Override
        public void register(ModuleInfo moduleInfo) {
            configLoader = getClass().getClassLoader();
            assertTrue(configLoader instanceof ModuleClassLoader);
            String moduleName = moduleInfo.getModuleName();
            assertEquals("default:me.maxwell.asyncmodule.ModuleSystemTest$Config", moduleName);

            moduleInfo.export("url", "jdbc:postgresql://localhost:5236/document");
            moduleInfo.export("username", "document");
            moduleInfo.export("password", "document");
            moduleInfo.export("driverClassName", "org.postgresql.Driver");

            moduleInfo.export("autoCommit", false);
            moduleInfo.export("connectionTimeout", 60000);
            moduleInfo.export("idleTimeout", 600000);
            moduleInfo.export("maxLifetime", 1200000);
            moduleInfo.export("maximumPoolSize", 100);
            moduleInfo.export("minimumIdle", 1);
            moduleInfo.export("poolName", "document-pool");
            moduleInfo.export("allowPoolSuspension", true);
            moduleInfo.setModuleLoaded();
        }
    }

    public static class DataSource implements Module {
        private String url;
        private static final Symbol<String> NAME_URL = new Symbol<>("url");

        @Override
        public void register(ModuleInfo moduleInfo) {
            assertTrue(getClass().getClassLoader() instanceof ModuleClassLoader);
            String moduleName = moduleInfo.getModuleName();
            assertEquals("default:me.maxwell.asyncmodule.ModuleSystemTest$DataSource", moduleName);
            moduleInfo.require(Config.class, NAME_URL.getKey());
        }

        @Override
        public void onRequireResolved(ModuleInfo moduleInfo, ModuleInfo rModuleInfo, String name) {
            String moduleName = moduleInfo.getModuleName();
            assertEquals("default:me.maxwell.asyncmodule.ModuleSystemTest$DataSource", moduleName);

            url = rModuleInfo.getExport(NAME_URL);
            moduleInfo.export(this);
            moduleInfo.setModuleLoaded();
        }

        public String getUrl() {
            return url;
        }
    }

    public static class Article {
        public String id;
        public String content;
        public String url;
    }

    public static class ArticleService implements Module {
        private Map<String, Article> data;
        private static final Symbol<DataSource> NAME_DataSource = new Symbol<>(ModuleFactory.DEFAULT_NAME);

        @Override
        public void register(ModuleInfo moduleInfo) {
            serviceLoader = getClass().getClassLoader();
            assertTrue(serviceLoader instanceof ModuleClassLoader);

            String moduleName = moduleInfo.getModuleName();
            assertEquals("default:me.maxwell.asyncmodule.ModuleSystemTest$ArticleService", moduleName);
            moduleInfo.require(DataSource.class);
        }

        @Override
        public void onRequireResolved(ModuleInfo moduleInfo, ModuleInfo rModuleInfo, String name) {
            String moduleName = moduleInfo.getModuleName();
            assertEquals("default:me.maxwell.asyncmodule.ModuleSystemTest$ArticleService", moduleName);

            DataSource dataSource = rModuleInfo.getExport(NAME_DataSource);
            Article article = new Article();
            article.id = "1";
            article.content = "4567";
            article.url = dataSource.getUrl();

            data = new TreeMap<>();
            data.put("1", article);

            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
                moduleInfo.export(this);
                moduleInfo.export("MAX_INT_VALUE", Integer.MAX_VALUE);
                moduleInfo.setModuleLoaded();
            }).start();
        }

        public Article getArticle(String id) {
            return data.get(id);
        }
    }

    public static class TestModule implements Module {
        ArticleService articleService;
        private static final Symbol<ArticleService> NAME_ArticleService = new Symbol<>(ModuleFactory.DEFAULT_NAME);
        private static final Symbol<Integer> NAME_MAX_INTEGER = new Symbol<>("MAX_INT_VALUE");

        @Override
        public void register(ModuleInfo moduleInfo) {
            testModuleLoader = getClass().getClassLoader();
            assertTrue(testModuleLoader instanceof ModuleClassLoader);

            String moduleName = moduleInfo.getModuleName();
            assertEquals("default:me.maxwell.asyncmodule.ModuleSystemTest$TestModule", moduleName);
            moduleInfo.require(ArticleService.class);
            moduleInfo.require(ArticleService.class, "MissRequire");
        }

        @Override
        public void onRequireResolved(ModuleInfo moduleInfo, ModuleInfo rModuleInfo, String name) {
            String moduleName = moduleInfo.getModuleName();
            assertEquals("default:me.maxwell.asyncmodule.ModuleSystemTest$TestModule", moduleName);

            if("default".equals(name)) {
                articleService = rModuleInfo.getExport(NAME_ArticleService);
                Article article = articleService.getArticle("1");
                assertTrue(article != null);
                assertTrue("1".equals(article.id));
                assertTrue("jdbc:postgresql://localhost:5236/document".equals(article.url));
            }
            else if("MAX_INT_VALUE".equals(name)) {
                Integer maxIntValue = rModuleInfo.getExport(NAME_MAX_INTEGER);
                assertTrue(Integer.MAX_VALUE == maxIntValue.intValue());
            }

            if(articleService != null) {
                moduleInfo.setModuleLoaded();
            }
        }

        @Override
        public void onRequireMissed(ModuleInfo moduleInfo, ModuleInfo rModuleInfo, String name) {
            String moduleName = moduleInfo.getModuleName();
            assertEquals("default:me.maxwell.asyncmodule.ModuleSystemTest$TestModule", moduleName);
            assertEquals("default:me.maxwell.asyncmodule.ModuleSystemTest$ArticleService", rModuleInfo.getModuleName());
            assertEquals("MissRequire", name);
        }
    }

    public static class InnerLoader implements ClassLoaderBuilder {
        @Override
        public String getCacheKey(String configName) {
            int idx = configName.lastIndexOf('$');
            if(idx > 0) {
                return configName.substring(0, idx);
            }
            return configName;
        }

        @Override
        public ModuleClassLoader createClassLoader(
                ClassLoaderFactory finder,
                ClassLoader parent,
                String cacheKey
        ) {
            ModuleClassLoader classLoader = new ModuleClassLoader(parent, cacheKey);
            classLoader.setFinder(finder);
            return classLoader;
        }
    }

    @Test
    public void moduleSystemTest() throws ClassNotFoundException {
        Map<String, ClassLoaderBuilder> config = new TreeMap<>();
        ClassLoaderBuilder builder = ClassLoaderBuilder.builder();
        ClassLoaderBuilder innerClassBuilder = new InnerLoader();

        ModuleSystem moduleSystem = new ModuleSystem();

        config.put("default:me.maxwell.asyncmodule.ModuleSystemTest$Config", innerClassBuilder);
        config.put("default:me.maxwell.asyncmodule.ModuleSystemTest$DataSource", innerClassBuilder);
        config.put("default:me.maxwell.asyncmodule.ModuleSystemTest$ArticleService", innerClassBuilder);
        config.put("default:me.maxwell.asyncmodule.ModuleSystemTest$TestModule", builder);
        moduleSystem.load("me.maxwell.asyncmodule.ModuleSystemTest$TestModule", config);

        assertTrue(serviceLoader == configLoader);
        assertTrue(serviceLoader != testModuleLoader);

        int count = 0;
        while(true) {
            count++;
            if(moduleSystem.getModuleFactory().isLoaded()) {
                moduleSystem.reloadModule(
                        "me.maxwell.asyncmodule.ModuleSystemTest$ArticleService",
                        "default",
                        new TreeMap<>()
                );
                break;
            }
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
            }

            if(count > 20) {
                throw new RuntimeException("???");
            }
        }
    }
}
