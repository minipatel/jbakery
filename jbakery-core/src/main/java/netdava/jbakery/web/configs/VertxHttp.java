package netdava.jbakery.web.configs;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertxbeans.ContextRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Configuration
public class VertxHttp {

    @Autowired
    @Qualifier("mainRouter")
    Router router;

    @Autowired
    Environment env;

    @Autowired
    Vertx vertx;

    @Autowired

    ContextRunner contextRunner;
    @Value("${server.port:8080}")

    Integer port;
    @Value("${server.address:localhost}")
    String address;

    @PostConstruct
    public void start() throws InterruptedException, ExecutionException, TimeoutException {
        // Create two instances
        contextRunner.executeBlocking(2,
                (Handler<AsyncResult<HttpServer>> handler) ->
                        vertx.createHttpServer()
                                .requestHandler(router::accept)
                                .listen(port, address, handler),
                1, TimeUnit.MINUTES);
    }

}
