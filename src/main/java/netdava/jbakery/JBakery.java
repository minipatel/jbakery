package netdava.jbakery;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;

@Slf4j
public class JBakery {

    public static void main(String[] args) {
        System.setProperty("user.timezone", "UTC");
        setVertxLogger();
        conventionBasedLoggingConfig();

        log.info("Logging from JBakery");

        DeploymentOptions options = new DeploymentOptions();

        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new JBakeryVerticle(), options);
    }

    private static void conventionBasedLoggingConfig() {
        //TODO: move path in config
        LoggerContext log4jContext = (LoggerContext) LogManager.getContext(false);
        File file = new File("conf/log4j2.xml");
        // this will force a reconfiguration
        log4jContext.setConfigLocation(file.toURI());
    }

    private static void setVertxLogger() {
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        log.info("Logging with " + System.getProperty("vertx.logger-delegate-factory-class-name"));
    }

}
