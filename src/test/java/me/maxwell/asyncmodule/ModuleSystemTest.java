package me.maxwell.asyncmodule;

import org.jdeferred2.DeferredManager;
import org.jdeferred2.Promise;
import org.jdeferred2.impl.DefaultDeferredManager;
import org.jdeferred2.multiple.MasterProgress;
import org.jdeferred2.multiple.MultipleResults2;
import org.jdeferred2.multiple.OneReject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModuleSystemTest {
    public static class Counter {
        private int count = 1;

        public int increment() {
            return ++count;
        }

        public int decrement() {
            return --count;
        }
    }

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
        }
    }

    public static class DataSource implements Module {
        private String url;

        @Override
        public void register(ModuleFactory factory) {
            factory.require(Config.class, "url", String.class).then(url -> {
                this.url = url;
                factory.export(this, DataSource.class);
            });
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
            factory.require(DataSource.class).then(dataSource -> {
                Article article = new Article();
                article.id = "1";
                article.content = "4567";
                article.url = dataSource.getUrl();

                data = new TreeMap<>();
                data.put("1", article);

                factory.export(this, ArticleService.class);
                factory.export("MAX_INT_VALUE", Integer.MAX_VALUE, DataSource.class);
            });
        }

        public Article getArticle(String id) {
            return data.get(id);
        }
    }

    ModuleFactory factory;

    @BeforeEach
    public void beforeEach() {
        factory = new ModuleFactory();
        factory.export("USER_NAME_PREFIX", "USER_NAME_");
        factory.export("increment", new Counter());
    }

    @Test
    public void globalModuleTest() {
        Promise<Counter, ModuleSystemException, Void> promise = factory.require("increment", Counter.class);

        promise.done(counter -> {
            assertTrue(counter.increment() == 2);
            assertTrue(counter.decrement() == 1);
        });

        Promise<MultipleResults2<String, Counter>, OneReject<ModuleSystemException>, MasterProgress> promise1 = factory.require(
            Dependency.of("USER_NAME_PREFIX", String.class),
            Dependency.of("increment", Counter.class)
        );

        promise1.then(multi -> {
            assertTrue(multi.getSecond().getResult().increment() == 2);
        });
    }

    @Test
    public void defaultExportTest() {
        factory.require(ArticleService.class).then(articleService -> {
            Article article = articleService.getArticle("1");
            assertTrue(article != null);
            assertTrue("1".equals(article.id));
            assertTrue("jdbc:postgresql://localhost:5236/document".equals(article.url));
        });

        factory.require(ArticleService.class).then(articleService -> {
            Article article = articleService.getArticle("1");
            assertTrue(article != null);
            assertTrue("1".equals(article.id));
            assertTrue("jdbc:postgresql://localhost:5236/document".equals(article.url));
        });
    }

    @Test
    public void moduleSystemTest() {
        Promise<ArticleService, ModuleSystemException, Void> promise1 = factory.require(ArticleService.class);
        Promise<Integer, ModuleSystemException, Void> promise2 = factory.require(ArticleService.class, "MAX_INT_VALUE", Integer.class);

        DeferredManager deferredManager = new DefaultDeferredManager();
        deferredManager.when(promise1, promise2).then(multi -> {
            Article article = multi.getFirst().getValue().getArticle("1");
            assertTrue(article != null);
            assertTrue("1".equals(article.id));
            assertTrue("jdbc:postgresql://localhost:5236/document".equals(article.url));

            assertTrue(multi.getSecond().getValue() == Integer.MAX_VALUE);
        });
    }
}
