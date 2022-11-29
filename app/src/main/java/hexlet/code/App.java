package hexlet.code;

import hexlet.code.controllers.RootController;
import hexlet.code.controllers.UrlController;

import io.javalin.Javalin;
import io.javalin.plugin.rendering.template.JavalinThymeleaf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "5000");
        int portNumber = Integer.valueOf(port);
        LOGGER.info("Received port {}", portNumber);
        return portNumber;
    }

    private static String getMode() {
        String mode = System.getenv().getOrDefault("APP_ENV", "development");
        LOGGER.info("Mode: {}", mode);
        return mode;
    }

    private static boolean isProduction() {
        return getMode().equals("production");
    }

    private static void addRoutes(Javalin app) {
        app.get("/", RootController.WELCOME);

        app.routes(() -> {
            path("urls", () -> {
                get(UrlController.URLS_LIST);
                post(UrlController.NEW_URL);
                path("{id}", () -> {
                    get(UrlController.SHOW_URL);
                    post("/checks", UrlController.CHECK_URL);
                });
            });
        });
    }

    private static TemplateEngine getTemplateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new Java8TimeDialect());
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateResolver.setCharacterEncoding("UTF-8");
        templateEngine.addTemplateResolver(templateResolver);
        return templateEngine;
    }

    public static Javalin getApp() {
        Javalin app = Javalin.create(config -> {
            if (!isProduction()) {
                config.enableDevLogging();
            }
            config.enableWebjars();
            JavalinThymeleaf.configure(getTemplateEngine());
        });
        addRoutes(app);
        app.before(ctx -> ctx.attribute("ctx", ctx));
        return app;
    }

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }
}
