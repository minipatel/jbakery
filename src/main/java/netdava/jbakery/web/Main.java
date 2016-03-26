package netdava.jbakery.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * Application bootstrap code.
 */
@Slf4j
public class Main {

    public static void main(String[] args) {
        // bootstrap application with Spring.
        log.info("Working directory {}", System.getProperty("user.dir"));
        AbstractApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
        context.registerShutdownHook();
    }

}
