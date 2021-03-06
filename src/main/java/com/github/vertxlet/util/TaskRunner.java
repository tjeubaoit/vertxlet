package com.github.vertxlet.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Deprecated
public class TaskRunner {

    private static final Logger logger = LoggerFactory.getLogger(TaskRunner.class);

    public static <T> void loopParallel(RunnableFuture<T> rf, int count, Handler<AsyncResult<T>> handler) {
        if (count <= 0) {
            logger.warn("No tasks to run");
            handler.handle(Future.succeededFuture());
            return;
        }

        List<RunnableFuture<T>> rfs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rfs.add(rf);
        }
        executeParallel(rfs, handler);
    }

    public static <T> void loopSequence(RunnableFuture<T> rf, int count, Handler<AsyncResult<T>> handler) {
        if (count <= 0) {
            logger.warn("No tasks to run");
            handler.handle(Future.succeededFuture());
            return;
        }

        Future<T> fut = Future.future();
        fut.setHandler(ar -> {
            if (ar.failed()) {
                handler.handle(Future.failedFuture(ar.cause()));
            } else {
                if (count == 1) {
                    handler.handle(Future.succeededFuture());
                } else {
                    TaskRunner.loopSequence(rf, count - 1, handler);
                }
            }
        });
        rf.run(fut);
    }

    public static <T> void executeParallel(List<RunnableFuture<T>> runnableFutures,
                                           Handler<AsyncResult<T>> handler) {
        if (runnableFutures.isEmpty()) {
            logger.warn("No tasks to run");
            handler.handle(Future.succeededFuture());
            return;
        }

        AtomicInteger remainTasks = new AtomicInteger(runnableFutures.size());

        Future<T> future = Future.future();
        future.setHandler(handler);

        for (RunnableFuture<T> rf : runnableFutures) {
            Future<T> fut = Future.future();
            fut.setHandler(ar -> {
                if (!future.isComplete()) {
                    if (ar.failed()) {
                        future.fail(ar.cause());
                    } else if (remainTasks.decrementAndGet() == 0) {
                        future.complete();
                    }
                }
            });
            rf.run(fut);
        }
    }
}
