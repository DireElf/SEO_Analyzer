package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;

class AppTest {
    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Database database;
    private static Url existingUrl;
    private final int code200 = 200;
    private final int code302 = 302;
    private static Transaction transaction;
    private static MockWebServer mockWebServer;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
        existingUrl = new Url("https://ru.hexlet.io");
        existingUrl.save();

    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
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
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", "https://www.google.com")
                    .asString();
            HttpResponse<String> responsePost1 = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", "http://localhost:5000")
                    .asString();
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(response.getBody()).contains("<title>Анализатор страниц</title>");
            assertThat(response.getBody()).contains("<a class=\"navbar-brand\" href=\"/\">Анализатор страниц</a>");
            assertThat(response.getBody()).contains("<a class=\"nav-link\" href=\"/\">Главная</a>");
            assertThat(response.getBody()).contains("<a class=\"nav-link\" href=\"/urls\">Сайты</a>");
            assertThat(body).contains("<a href=\"/urls/3\">https://www.google.com</a>");
            assertThat(body).contains("<a href=\"/urls/4\">http://localhost:5000</a>");
            assertThat(response.getBody())
                    .contains("<a href=\"https://github.com/DireElf\" target=\"_blank\">DireElf</a>");
        }

        @Test
        void showUrl() {
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", "https://www.google.com")
                    .asString();
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/3")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(body).contains("Сайт https://www.google.com");
        }

        @Test
        void addUrl() {
            String inputUrl = "https://dzen.ru";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputUrl)
                    .asString();

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
                    .asString();

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
                    .asString();
            HttpResponse<String> responsePost1 = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputName)
                    .asString();

            assertThat(responsePost1.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(body).contains(inputName);
            assertThat(body).contains("Страница уже существует");
        }
    }
}
