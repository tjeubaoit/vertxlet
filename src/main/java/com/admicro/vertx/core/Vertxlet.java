package com.admicro.vertx.core;

import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public interface Vertxlet {

    void setContext(Vertx vertx, Verticle verticle);

    default <T> void init(Future<T> future) {
        future.complete();
    }

    default <T> void destroy(Future<T> future) {
        future.complete();
    }

    void handle(RoutingContext routingContext);

    Vertx getVertx();

    Verticle getVerticle();
}
