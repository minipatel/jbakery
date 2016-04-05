package netdava.jbakery.web.configs;

import lombok.extern.slf4j.Slf4j;
import netdava.jbakery.web.Oven;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.jbake.app.ConfigUtil;
import org.jbake.app.JBakeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Slf4j
@Configuration
public class JbakeConfig {

    @Value("${source:.}")
    String source;

    @Value("${destination:output}")
    String destination;

    @Bean
    public Oven jbakeOven() throws Exception {
        log.info("Baking {} -> {}", source, destination);

        File sourceFile = new File(source);
        final CompositeConfiguration config;
        try {
            config = ConfigUtil.load(sourceFile);
        } catch (final ConfigurationException e) {
            throw new JBakeException("Configuration error: " + e.getMessage(), e);
        }

        System.out.println("JBake " + config.getString(ConfigUtil.Keys.VERSION) +
                " (" + config.getString(ConfigUtil.Keys.BUILD_TIMESTAMP) + ") [http://jbake.org]");

        System.out.println();

        Oven oven = new Oven(sourceFile, new File(destination), true);
        oven.setupPaths();

        return oven;
    }

}
