package netdava.jbakery.web;

import io.vertx.core.Vertx;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;
import io.vertxbeans.VertxBeans;
import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.thymeleaf.resourceresolver.FileResourceResolver;
import org.thymeleaf.templatemode.StandardTemplateModeHandlers;
import org.thymeleaf.templateresolver.TemplateResolver;

@ComponentScan
@Configuration
@Import(VertxBeans.class)
public class Config {

    @Autowired
    Vertx vertx;

    @Autowired
    Environment env;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ThymeleafTemplateEngine createThymeleaf() {
        boolean cachingEnabled = env.getProperty("jbakery.thymeleaf.cachingEnabled", Boolean.class, false);

        TemplateResolver templateResolver = new TemplateResolver();
        templateResolver.setResourceResolver(new FileResourceResolver());
        templateResolver.setPrefix("templates/");
        templateResolver.setTemplateMode("HTML5");
        templateResolver.setSuffix(".html");
        templateResolver.setCacheable(cachingEnabled);

        templateResolver.addTemplateAlias("", "/index");
        templateResolver.addTemplateAlias("/", "/index");

        ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create()
                .setMode(StandardTemplateModeHandlers.HTML5.getTemplateModeName());
        // enable layout
        engine.getThymeleafTemplateEngine().addDialect(new LayoutDialect());
        engine.getThymeleafTemplateEngine().setTemplateResolver(templateResolver);

        return engine;
    }


}
