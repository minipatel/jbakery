package netdava.jbakery.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * Application bootstrap code.
 */
@Slf4j
public class Main {

    public static final String JBAKERY_HOME = "JBAKERY_HOME";

    public static void main(String[] args) throws Exception {
        // bootstrap application with Spring.
        log.info("Working directory {}", System.getProperty("user.dir"));
        ensureJbakeryHomeIsSet();

        AbstractApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
        context.registerShutdownHook();
    }

    private static void ensureJbakeryHomeIsSet() {
        String jbakeryHome = System.getenv(JBAKERY_HOME);
        log.info(JBAKERY_HOME + " is {}", jbakeryHome);

        if (jbakeryHome == null) {
            log.error("Missing JBAKERY_HOME environment variable.\n" +
                    "Please make sure you set JABEKERY_HOME to where you installed jbakery");
            System.exit(-1);
        }
    }

}
