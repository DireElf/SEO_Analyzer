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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    // Prometheus registry
    private static final PrometheusMeterRegistry REGISTRY =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    // RED метрики
    private static final Counter REQUEST_COUNTER = Counter.builder("http_requests_total")
            .description("Total HTTP requests")
            .register(REGISTRY);

    private static final Counter ERROR_COUNTER = Counter.builder("http_requests_errors_total")
            .description("Total HTTP errors")
            .register(REGISTRY);

    private static final Timer REQUEST_TIMER = Timer.builder("http_request_duration_seconds")
            .description("Request latency")
            .publishPercentileHistogram()
            .register(REGISTRY);

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

        // endpoint для Prometheus
        app.get("/metrics", ctx -> ctx.result(REGISTRY.scrape()));
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

        // старт таймера + считаем запросы
        app.before(ctx -> {
            ctx.attribute("timerSample", Timer.start(REGISTRY));
            REQUEST_COUNTER.increment();
            ctx.attribute("ctx", ctx);
        });

        // фиксируем latency
        app.after(ctx -> {
            Timer.Sample sample = ctx.attribute("timerSample");
            if (sample != null) {
                sample.stop(REQUEST_TIMER);
            }
        });

        // считаем ошибки
        final int errorStatus = 500;

        app.exception(Exception.class, (e, ctx) -> {
            ERROR_COUNTER.increment();
            LOGGER.error("Unhandled exception", e);
            ctx.status(errorStatus).result("Internal Server Error");
        });

        addRoutes(app);

        return app;
    }

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }
}
