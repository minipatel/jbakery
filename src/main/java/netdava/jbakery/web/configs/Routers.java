package netdava.jbakery.web.configs;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseTimeHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Routers {

    private static final int KB = 1024;
    private static final int MB = 1024 * KB;

    @Autowired
    Vertx vertx;

    @Autowired
    ThymeleafTemplateEngine thymeleafTemplateEngine;

    @Bean
    public Router mainRouter() {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create().setBodyLimit(50 * MB));
        router.route().handler(FaviconHandler.create("webroot/favicon.ico"));
        router.route().handler(LoggerHandler.create());
        router.route().handler(ResponseTimeHandler.create());

        router.route("/static/*")
                .handler(StaticHandler.create().setIndexPage("index.html").setCachingEnabled(false));

        router.route().handler(TemplateHandler.create(thymeleafTemplateEngine, "", "text/html"));

        return router;
    }

}
