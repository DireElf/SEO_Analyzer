package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;
import io.ebean.DB;
import io.ebean.Database;

class AppTest {
    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Database database;
    private final int code200 = 200;
    private final int code302 = 302;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        database.script().run("/truncate.sql");
        database.script().run("/seed-test-db.sql");
    }

    @Nested
    class RootTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(response.getBody()).contains("<title>Анализатор страниц</title>");
            assertThat(response.getBody()).contains("<a class=\"navbar-brand\" href=\"/\">Анализатор страниц</a>");
            assertThat(response.getBody()).contains("<a class=\"nav-link\" href=\"/\">Главная</a>");
            assertThat(response.getBody()).contains("<a class=\"nav-link\" href=\"/urls\">Сайты</a>");
            assertThat(response.getBody()).contains("Бесплатно проверяйте сайты на SEO пригодность");
            assertThat(response.getBody()).contains("Пример: https://www.example.com");
            assertThat(response.getBody()).contains("Проверить");
            assertThat(response.getBody())
                    .contains("<a href=\"https://github.com/DireElf\" target=\"_blank\">DireElf</a>");
        }
    }

    @Nested
    class UrlTest {

        @Test
        void urlsList() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(response.getBody()).contains("<title>Анализатор страниц</title>");
            assertThat(response.getBody()).contains("<a class=\"navbar-brand\" href=\"/\">Анализатор страниц</a>");
            assertThat(response.getBody()).contains("<a class=\"nav-link\" href=\"/\">Главная</a>");
            assertThat(response.getBody()).contains("<a class=\"nav-link\" href=\"/urls\">Сайты</a>");
            assertThat(body).contains("<a href=\"/urls/1\">https://www.google.com</a>");
            assertThat(body).contains("<a href=\"/urls/2\">http://localhost:5000</a>");
            assertThat(response.getBody())
                    .contains("<a href=\"https://github.com/DireElf\" target=\"_blank\">DireElf</a>");
        }

        @Test
        void showUrl() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/1")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(body).contains("Сайт https://www.google.com");
        }

        @Test
        void addUrl() {
            String inputUrl = "https://ru.hexlet.io";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputUrl)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(code302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(body).contains(inputUrl);
            assertThat(body).contains("Страница успешно добавлена");

            Url actualUrl = new QUrl()
                    .name.equalTo(inputUrl)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(inputUrl);
        }

        @Test
        void addInvalidUrl() {
            String inputName = "test";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputName)
                    .asEmpty();

            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/")
                    .asString();
            String body = response.getBody();

            assertThat(body).contains("Некорректный URL");
        }

        @Test
        void testCreateExistingUrl() {
            String inputName = "https://www.google.com";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputName)
                    .asEmpty();

            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(body).contains(inputName);
            assertThat(body).contains("Страница уже существует");
        }
    }
}
