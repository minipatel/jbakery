package netdava.jbakery.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

/**
 * Application bootstrap code.
 */
@Slf4j
public class Main {

    public static final String JBAKERY_HOME = "JBAKERY_HOME";

    public static void main(String[] args) throws Exception {
        String jbakeryHome = ensureJbakeryHomeIsSet();
        System.setProperty("log4j.configurationFile", log4jConfigPath(jbakeryHome));
        log.info("Working directory {}", System.getProperty("user.dir"));
        log.info("JBAKERY_HOME is {}", jbakeryHome);

        // bootstrap application with Spring.
        String jbakeryConfigPath = jbakeryConfigPath(jbakeryHome);
        log.info("Using config file {}", jbakeryConfigPath);
        System.setProperty("spring.config.name", jbakeryConfigPath);

        CommandLinePropertySource clps = new SimpleCommandLinePropertySource(args);

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(clps);
        context.registerShutdownHook();
        context.register(Config.class);
        context.refresh();

    }

    private static String ensureJbakeryHomeIsSet() {
        String jbakeryHome = System.getenv(JBAKERY_HOME);

        if (jbakeryHome == null) {
            System.err.println("Missing JBAKERY_HOME environment variable.\n" +
                    "Please make sure you set JABEKERY_HOME to where you installed jbakery");
            System.exit(-1);
        }
        return jbakeryHome;
    }

    private static String jbakeryConfigPath(String jbakeryHome) {
        return String.format("file:%s/conf/jbakery.properties", jbakeryHome);
    }

    private static String log4jConfigPath(String jbakeryHome) {
        return String.format("%s/conf/log4j2.xml", jbakeryHome);
    }

}
