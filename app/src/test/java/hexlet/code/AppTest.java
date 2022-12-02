package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.assertj.core.api.Assertions.assertThat;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import io.javalin.Javalin;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    private final int entriesPerPage = 10;
    private static final String FIXTURES_DIRECTORY = "src/test/resources/fixtures";

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        database.script().run("/truncate.sql");
        app.stop();
    }

    @Nested
    class RootTest {
        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(response.getBody()).contains("Анализатор страниц");
        }
    }

    @Nested
    class UrlTest {
        private final String testLink = "https://www.example.com";

        @BeforeEach
        void addTestLink() {
            database.script().run("/truncate.sql");
            Url testUrl = new Url(testLink);
            testUrl.save();
        }

        @Test
        void urlsList() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(body).contains(testLink);
        }

        @Test
        void showUrl() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/" + 1)
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(body).contains(testLink);
        }

        @Test
        void addUrl() {
            String url = "https://www.google.com";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", url)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(code302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(code200);
            assertThat(body).contains(url);
            assertThat(body).contains("Страница успешно добавлена");

            Url actualUrl = new QUrl()
                    .name.equalTo(url)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(url);
        }

        @Test
        void addInvalidUrl() {
            String url = "test";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", url)
                    .asString();

            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/")
                    .asString();
            String body = response.getBody();

            assertThat(body).contains("Некорректный URL");
        }

        @Test
        void addExistingUrl() {
            HttpResponse<String> responsePost1 = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", testLink)
                    .asString();

            assertThat(responsePost1.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(body).contains(testLink);
            assertThat(body).contains("Страница уже существует");
        }

        @Test
        void checkUrl() throws IOException {
            String samplePage = Files.readString(Paths.get(FIXTURES_DIRECTORY, "sample.html"));

            MockWebServer mockServer = new MockWebServer();
            String samplePageUrl = mockServer.url("/").toString();
            mockServer.enqueue(new MockResponse().setBody(samplePage));

            HttpResponse<String> response = Unirest
                    .post(baseUrl + "/urls/")
                    .field("url", samplePageUrl)
                    .asEmpty();

            Url url = new QUrl()
                    .name.equalTo(samplePageUrl.substring(0, samplePageUrl.length() - 1))
                    .findOne();

            assertThat(url).isNotNull();

            HttpResponse<String> response1 = Unirest
                    .post(baseUrl + "/urls/" + url.getId() + "/checks")
                    .asEmpty();

            HttpResponse<String> response2 = Unirest
                    .get(baseUrl + "/urls/" + url.getId())
                    .asString();

            assertThat(response2.getStatus()).isEqualTo(code200);

            UrlCheck check = new QUrlCheck()
                    .findList().get(0);

            assertThat(check).isNotNull();
            assertThat(check.getUrl().getId()).isEqualTo(url.getId());

            assertThat(response2.getBody()).contains("Sample title");
            assertThat(response2.getBody()).contains("Sample description");
            assertThat(response2.getBody()).contains("Sample header");

            mockServer.shutdown();
        }
    }

    @Nested
    class PaginationTest {
        @Test
        void testPagination() {
            for (int i = 1; i <= entriesPerPage + 1; i++) {
                String testLink = String.format("http://localhost:%d", i);
                Url url = new Url(testLink);
                url.save();
            }
            HttpResponse<String> response1 = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body1 = response1.getBody();
            assertThat(body1).contains("http://localhost:1");
            assertThat(body1).contains("http://localhost:10");
            assertThat(body1.contains("http://localhost:11")).isFalse();
            HttpResponse<String> response2 = Unirest
                    .get(baseUrl + "/urls?page=2")
                    .asString();
            String body2 = response2.getBody();
            assertThat(body2).contains("http://localhost:11");
        }
    }
}
