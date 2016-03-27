package netdava.jbakery.web.configs;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseTimeHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class Routers {

    public static final String PREFIX = "/_jbakery";
    private static final int KB = 1024;
    private static final int MB = 1024 * KB;
    @Autowired
    Vertx vertx;

    @Autowired
    ThymeleafTemplateEngine thymeleafTemplateEngine;

    @Value("${JBAKERY_HOME}")
    String jbakeryHomeProp;

    @Bean
    public Router mainRouter() {
        Router router = Router.router(vertx);

        Path jbakeryHome = Paths.get(jbakeryHomeProp);

        router.route().handler(BodyHandler.create().setBodyLimit(50 * MB));
        router.route().handler(LoggerHandler.create());
        router.route().handler(ResponseTimeHandler.create());

        router.mountSubRouter(PREFIX, jbakery(Vertx.vertx()));

        router.route().handler(StaticHandler.create("output")
                .setIndexPage("index.html")
                .setCachingEnabled(false));

        return router;
    }


    public Router jbakery(Vertx vertx) {
        Router router = Router.router(vertx);

        Path jbakeryHome = Paths.get(jbakeryHomeProp);

        router.route("/static/*")
                .handler(StaticHandler.create()
                        .setAllowRootFileSystemAccess(true)
                        .setWebRoot(jbakeryHome.resolve("webroot").toString())
                        .setIndexPage("index.html")
                        .setCachingEnabled(false));

        router.route().handler(TemplateHandler.create(thymeleafTemplateEngine, "", "text/html"));

        return router;
    }


}
