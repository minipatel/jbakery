package netdava.jbakery;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import lombok.extern.slf4j.Slf4j;
import netdava.jbakery.fixes.ThymeleafTemplateEngineImpl;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import java.util.Set;

@Slf4j
public class JBakeryVerticle extends AbstractVerticle {

    public static final int KB = 1024;
    public static final int MB = 1024 * KB;

    @Override
    public void start() throws Exception {
        final Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create().setBodyLimit(50 * MB));

        router.route().handler(LoggerHandler.create());
        router.route().handler(ResponseTimeHandler.create());

        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx))
                .setNagHttps(false).setSessionCookieName("SESSION"));

        JsonObject config = new JsonObject().put("properties_path", "conf/auth.properties");
        AuthProvider authProvider = ShiroAuth.create(vertx, ShiroAuthRealmType.PROPERTIES, config);
        router.route().handler(UserSessionHandler.create(authProvider));


        router.route("/static/*")
                .handler(StaticHandler.create().setIndexPage("index.html").setCachingEnabled(false));

        ThymeleafTemplateEngineImpl engine = createAndConfigureTemplateEngine();
        router.route().handler(TemplateHandler.create(engine, "templates/public", "text/html"));

        HttpServer server = vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080, "localhost");
    }

    private ThymeleafTemplateEngineImpl createAndConfigureTemplateEngine() {
        ThymeleafTemplateEngineImpl engine = new ThymeleafTemplateEngineImpl();
        engine.setMode("HTML5");

        // enable Thymeleaf layouts
        engine.getThymeleafTemplateEngine().addDialect(new LayoutDialect());

        // disable caching - useful for development
        Set<ITemplateResolver> trs = engine.getThymeleafTemplateEngine().getTemplateResolvers();
        for (ITemplateResolver tr : trs) {
            ((TemplateResolver) tr).setCacheable(false);
        }
        return engine;
    }

}
