package hexlet.code.controllers;

import hexlet.code.service.UrlDetails;
import hexlet.code.service.Utils;
import io.ebean.PagedList;

import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static hexlet.code.service.Utils.getNormalizedUrl;
import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;

import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.HttpResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UrlController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlController.class);

    public static final Handler URLS_LIST = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        final int entriesPerPage = 10;

        LOGGER.info("Request URLs list");
        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(page * entriesPerPage)
                .setMaxRows(entriesPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();
        Map<String, UrlDetails> urlDetails = Utils.getUrlDetails(urls);

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;

        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .toList();

        ctx.attribute("urls", urls);
        ctx.attribute("details", urlDetails);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls.html");
    };

    public static final Handler NEW_URL = ctx -> {
        String checkedUrl = getNormalizedUrl(ctx.formParam("url"));

        if (checkedUrl.isEmpty()) {
            LOGGER.info("Invalid URL is found");
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
            return;
        }

        LOGGER.info("Looking for {} in database", checkedUrl);
        Url existentUrl = new QUrl()
                .name.equalTo(checkedUrl)
                .findOne();
        if (existentUrl != null) {
            LOGGER.info("Existent URL {} is found", existentUrl.getName());
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/urls");
            return;
        }

        LOGGER.info("Valid URL {} is received", checkedUrl);
        Url url = new Url(checkedUrl);
        url.save();

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");
        ctx.redirect("/urls");
    };

    public static final Handler SHOW_URL = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        LOGGER.info("ID {} was found, looking for corresponding URL in database", id);
        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();
        if (url == null) {
            throw new NotFoundResponse();
        }

        LOGGER.info("Request URL checks list");
        List<UrlCheck> urlChecks = new QUrlCheck()
                .url.equalTo(url)
                .orderBy().id.desc()
                .findList();

        ctx.attribute("urlChecks", urlChecks);
        ctx.attribute("url", url);
        ctx.render("show.html");
    };

    public static final Handler CHECK_URL = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        LOGGER.info("ID {} was found, looking for corresponding URL in database", id);
        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();
        if (url == null) {
            throw new NotFoundResponse();
        }

        LOGGER.info("Try to check URL {}", url.getName());
        try {
            LOGGER.info("GET request to {}", url.getName());
            HttpResponse<String> response = Unirest
                    .get(url.getName())
                    .asString();

            LOGGER.info("Parse received page");
            Document body = Jsoup.parse(response.getBody());

            int statusCode = response.getStatus();
            String title = body.title();
            String h1 = body.selectFirst("h1") != null ? body.selectFirst("h1").text() : null;
            String description = body.selectFirst("meta[name=description]") != null
                    ? body.selectFirst("meta[name=description]").attr("content") : null;

            UrlCheck check = new UrlCheck(statusCode, title, h1, description, url);

            LOGGER.info("URL {} was checked, save to database", check.getUrl().getName());
            check.save();

            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");
        } catch (UnirestException e) {
            LOGGER.info("Failed to check URL");
            ctx.sessionAttribute("flash", "Не удалось проверить страницу");
            ctx.sessionAttribute("flash-type", "danger");
        }
        ctx.redirect("/urls/" + id);
    };
}
