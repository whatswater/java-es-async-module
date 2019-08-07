package me.maxwell.asyncmodule;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModuleSystemTest {
    public static class Config implements Module {
        @Override
        public void register(ModuleFactory factory) {
            factory.export("url", "jdbc:postgresql://localhost:5236/document", Config.class);
            factory.export("username", "document", Config.class);
            factory.export("password", "document", Config.class);
            factory.export("driverClassName", "org.postgresql.Driver", Config.class);

            factory.export("autoCommit", false, Config.class);
            factory.export("connectionTimeout", 60000, Config.class);
            factory.export("idleTimeout", 600000, Config.class);
            factory.export("maxLifetime", 1200000, Config.class);
            factory.export("maximumPoolSize", 100, Config.class);
            factory.export("minimumIdle", 1, Config.class);
            factory.export("poolName", "document-pool", Config.class);
            factory.export("allowPoolSuspension", true, Config.class);
            factory.setModudleLoaded(this);
        }
    }

    public static class DataSource implements Module {
        private String url;

        @Override
        public void register(ModuleFactory factory) {
            factory.require(this, Config.class, "url");
        }

        @Override
        public void onDependencyResolved(ModuleFactory factory, Class<? extends Module> moduleClass, String name) {
            url = factory.getExport(moduleClass, name);
            factory.export(this, DataSource.class);
            factory.setModudleLoaded(this);
        }

        public String getUrl() {
            return url;
        }
    }

    public static class Article {
        String id;
        String content;
        String url;
    }

    public static class ArticleService implements Module {
        private Map<String, Article> data;

        @Override
        public void register(ModuleFactory factory) {
            factory.require(this, DataSource.class);
        }

        @Override
        public void onDependencyResolved(ModuleFactory factory, Class<? extends Module> moduleClass, String name) {
            DataSource dataSource = factory.getExport(moduleClass, name);

            Article article = new Article();
            article.id = "1";
            article.content = "4567";
            article.url = dataSource.getUrl();

            data = new TreeMap<>();
            data.put("1", article);

            factory.export(this, ArticleService.class);
            factory.export("MAX_INT_VALUE", Integer.MAX_VALUE, DataSource.class);
            factory.setModudleLoaded(this);
        }

        public Article getArticle(String id) {
            return data.get(id);
        }
    }

    public static class TestModule implements Module {
        ArticleService articleService;

        @Override
        public void register(ModuleFactory factory) {
            factory.require(this, ArticleService.class);
        }

        @Override
        public void onDependencyResolved(ModuleFactory factory, Class<? extends Module> moduleClass, String name) {
            if("default".equals(name)) {
                articleService = factory.getExport(moduleClass, name);
                Article article = articleService.getArticle("1");
                assertTrue(article != null);
                assertTrue("1".equals(article.id));
                assertTrue("jdbc:postgresql://localhost:5236/document".equals(article.url));
            }
            else if("MAX_INT_VALUE".equals(name)) {
                Integer maxIntValue = factory.getExport(moduleClass, name);
                assertTrue(Integer.MAX_VALUE == maxIntValue.intValue());
            }

            if(articleService != null) {
                factory.setModudleLoaded(this);
            }
        }
    }

    @Test
    public void moduleSystemTest() throws ClassNotFoundException {
        ModuleSystem.load("me.maxwell.asyncmodule.ModuleSystemTest$TestModule");
    }
}
