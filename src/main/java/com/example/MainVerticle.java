package com.example;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Vert.x API Gateway.
 * Configures HTTP routing, request logging, and service initialization.
 */
public class MainVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

  private HttpServer server;
  private ApiService apiService;

  @Override
  public void start(Promise<Void> startPromise) {
    ConfigRetriever retriever = ConfigRetriever.create(vertx);

    retriever.getConfig()
      .compose(fileConfig -> {
        JsonObject mergedConfig = fileConfig.mergeIn(config());
        int port = mergedConfig.getInteger("server.port", 8080);
        this.apiService = new ApiService(vertx, mergedConfig);

        Router router = setupRouter();
        this.server = vertx.createHttpServer();

        return server.requestHandler(router).listen(port);
      })
      .onSuccess(httpServer -> {
        LOGGER.info("API Gateway started successfully on port {}", httpServer.actualPort());
        startPromise.complete();
      })
      .onFailure(error -> {
        LOGGER.error("Failed to start API Gateway", error);
        startPromise.fail(error);
      });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    Future<Void> serverClose = server == null ? Future.<Void>succeededFuture() : server.close();
    Future<Void> serviceClose = apiService == null ? Future.<Void>succeededFuture() : apiService.close();
    Future.all(serverClose, serviceClose)
      .onComplete(ar -> {
        if (ar.succeeded()) {
          stopPromise.complete();
        } else {
          stopPromise.fail(ar.cause());
        }
      });
  }

  public int actualPort() {
    return server != null ? server.actualPort() : -1;
  }

  private Router setupRouter() {
    Router router = Router.router(vertx);

    // Request logging middleware
    router.route().handler(this::logRequest);

    // Root welcome handler directing to primary endpoint
    router.get("/").handler(ctx -> ctx.response()
      .putHeader("content-type", "application/json")
      .end(new JsonObject()
        .put("message", "Welcome to the Vert.x Asynchronous API Gateway!")
        .put("project", "Junior / Senior Java Backend Assessment")
        .put("author", "Siddharth Shinde")
        .put("status", "running")
        .put("endpoint", "/aggregate")
        .encode()));

    // Primary assessment endpoint
    AggregateHandler aggregateHandler = new AggregateHandler(apiService);
    router.get("/aggregate").handler(aggregateHandler);

    return router;
  }

  private void logRequest(RoutingContext ctx) {
    long startTime = System.currentTimeMillis();
    ctx.addBodyEndHandler(ignored -> {
      long duration = System.currentTimeMillis() - startTime;
      LOGGER.info("{} {} -> Status {} ({} ms)",
        ctx.request().method(),
        ctx.request().uri(),
        ctx.response().getStatusCode(),
        duration);
    });
    ctx.next();
  }
}
