package com.admicro.vertx.core;

import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;

public class HttpVertxlet implements IHttpVertxlet {

    private Vertx vertx;
    private Verticle verticle;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void setContext(Vertx vertx, Verticle verticle) {
        this.vertx = vertx;
        this.verticle = verticle;
    }

    @Override
    public void init(Future<Void> future) {
        init();
        future.complete();
    }

    @Override
    public void destroy(Future<Void> future)  {
        destroy();
        future.complete();
    }

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/server_load";
    static final String USER = "root";
    static final String PASS = "root";

    @Override
    public void handle(RoutingContext routingContext) {
        if (getClass().getAnnotation(VertxServlet.class).usingDatabase()) {
            JsonObject config = new JsonObject()
                    .put("url", DB_URL)
                    .put("driver_class", JDBC_DRIVER)
                    .put("user", USER)
                    .put("password", PASS)
                    .put("max_pool_size", 50);

            JDBCClient client = JDBCClient.createShared(vertx, config);

            client.getConnection(result -> {
                if (result.failed()) {
                    routingContext.fail(result.cause());
                } else {
                    SQLConnection con = result.result();
                    routingContext.put("db", con);
                    routingContext.addHeadersEndHandler(future -> con.close(v -> {
                        if (v.failed()) {
                            future.fail(v.cause());
                        } else {
                            future.complete();
                        }
                    }));
                    routeByMethod(routingContext);
                }
            });
        } else {
            routeByMethod(routingContext);
        }
    }

    @Override
    public Vertx getVertx() {
        return vertx;
    }

    public Verticle getVerticle() {
        return verticle;
    }

    protected void init() {}

    protected void destroy() {}

    protected void doGet(RoutingContext routingContext) {
        routingContext.response().end();
    }

    protected void doPost(RoutingContext routingContext) {
        routingContext.response().end();
    }

    protected <T> void executingHeavyTask(AsyncTask<T> task, Handler<AsyncResult<T>> handler) {
        executingHeavyTask(task, handler, false);
    }

    protected <T> void executingHeavyTask(AsyncTask<T> task, Handler<AsyncResult<T>> handler, boolean ordered) {
        vertx.executeBlocking(future -> {
            try {
                T result = task.run();
                future.complete(result);
            } catch (Exception e) {
                future.fail(e);
            }
        }, ordered, handler);
    }

    protected void post(Runnable runnable) {
        postDelay(runnable, 0);
    }

    protected void postDelay(Runnable runnable, long delay) {
        vertx.setTimer(delay, id -> runnable.run());
    }

    protected SQLConnection getSqlConnection(RoutingContext routingContext) throws UnsupportedOperationException {
        SQLConnection con = routingContext.get("db");
        if (con == null) {
            UnsupportedOperationException e = new UnsupportedOperationException(
                    "Vertxlet was not declared using database");
            logger.error(e.getMessage(), e);
            throw e;
        }

        return con;
    }

    private void routeByMethod(RoutingContext routingContext) {
        if (routingContext.request().method() == HttpMethod.GET) {
            doGet(routingContext);
        } else if (routingContext.request().method() == HttpMethod.POST) {
            doPost(routingContext);
        } else {
            UnsupportedOperationException e = new UnsupportedOperationException("Method not support");
            logger.error(e.getMessage(), e);
            throw e;
        }
    }
}