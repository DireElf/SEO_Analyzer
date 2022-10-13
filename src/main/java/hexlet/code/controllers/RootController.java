package hexlet.code.controllers;

import io.javalin.http.Handler;

public final class RootController {
    public static final Handler WELCOME = ctx -> ctx.render("index.html");
}
