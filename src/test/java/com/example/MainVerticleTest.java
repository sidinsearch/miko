package com.example;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit and integration tests evaluating the API Gateway requirements and Circuit Breaker resilience.
 */
@ExtendWith(VertxExtension.class)
class MainVerticleTest {
  @Test
  void returnsAggregatedResponse(Vertx vertx, VertxTestContext testContext) {
    StubServers stubs = new StubServers(vertx)
      .withPostResponse(new JsonObject().put("userId", 1).put("id", 1).put("title", "Post title").put("body", "Body"))
      .withUserResponse(new JsonObject().put("id", 1).put("name", "Author Name").put("username", "username").put("email", "user@example.com"));

    stubs.start()
      .compose(ignored -> deployGateway(vertx, stubs, 2500))
      .compose(mainVerticle -> invokeGateway(vertx, mainVerticle.actualPort(), "/aggregate"))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(200, response.statusCode());
        assertEquals("Post title", response.body().getString("post_title"));
        assertEquals("Author Name", response.body().getString("author_name"));
        testContext.completeNow();
      })));
  }

  @Test
  void returns500WhenOneApiFails(Vertx vertx, VertxTestContext testContext) {
    StubServers stubs = new StubServers(vertx)
      .withPostResponse(new JsonObject().put("userId", 1).put("id", 1).put("title", "Post title").put("body", "Body"))
      .withUserFailure();

    stubs.start()
      .compose(ignored -> deployGateway(vertx, stubs, 2500))
      .compose(mainVerticle -> invokeGateway(vertx, mainVerticle.actualPort(), "/aggregate"))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(500, response.statusCode());
        assertEquals("Failed to fetch external service", response.body().getString("error"));
        testContext.completeNow();
      })));
  }

  @Test
  void returns500ForMalformedJson(Vertx vertx, VertxTestContext testContext) {
    StubServers stubs = new StubServers(vertx)
      .withPostRawBody("{not-valid-json")
      .withUserResponse(new JsonObject().put("id", 1).put("name", "Author Name").put("username", "username").put("email", "user@example.com"));

    stubs.start()
      .compose(ignored -> deployGateway(vertx, stubs, 2500))
      .compose(mainVerticle -> invokeGateway(vertx, mainVerticle.actualPort(), "/aggregate"))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(500, response.statusCode());
        assertEquals("Failed to fetch external service", response.body().getString("error"));
        testContext.completeNow();
      })));
  }

  @Test
  void returns500OnTimeout(Vertx vertx, VertxTestContext testContext) {
    StubServers stubs = new StubServers(vertx)
      .withPostResponse(new JsonObject().put("userId", 1).put("id", 1).put("title", "Post title").put("body", "Body"))
      .withUserDelayedResponse(new JsonObject().put("id", 1).put("name", "Author Name").put("username", "username").put("email", "user@example.com"), 300L);

    stubs.start()
      .compose(ignored -> deployGateway(vertx, stubs, 50))
      .compose(mainVerticle -> invokeGateway(vertx, mainVerticle.actualPort(), "/aggregate"))
      .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(500, response.statusCode());
        assertEquals("Failed to fetch external service", response.body().getString("error"));
        testContext.completeNow();
      })));
  }

  private Future<MainVerticle> deployGateway(Vertx vertx, StubServers stubs, long timeoutMs) {
    MainVerticle mainVerticle = new MainVerticle();
    JsonObject config = new JsonObject()
      .put("server.port", 0)
      .put("request.timeout.ms", timeoutMs)
      .put("post.url", stubs.postUrl())
      .put("user.url", stubs.userUrl());

    return vertx.deployVerticle(mainVerticle, new DeploymentOptions().setConfig(config))
      .map(ignored -> mainVerticle);
  }

  private Future<io.vertx.ext.web.client.HttpResponse<JsonObject>> invokeGateway(Vertx vertx, int port, String path) {
    return WebClient.create(vertx)
      .get(port, "localhost", path)
      .as(BodyCodec.jsonObject())
      .send();
  }

  private static final class StubServers {
    private final Vertx vertx;
    private String postRawBody = null;
    private JsonObject postResponse;
    private JsonObject userResponse;
    private boolean userFailure;
    private long userDelayMs;
    private HttpServer postServer;
    private HttpServer userServer;

    private StubServers(Vertx vertx) {
      this.vertx = vertx;
    }

    StubServers withPostResponse(JsonObject response) {
      this.postResponse = response;
      return this;
    }

    StubServers withPostRawBody(String rawBody) {
      this.postRawBody = rawBody;
      return this;
    }

    StubServers withUserResponse(JsonObject response) {
      this.userResponse = response;
      return this;
    }

    StubServers withUserFailure() {
      this.userFailure = true;
      return this;
    }

    StubServers withUserDelayedResponse(JsonObject response, long delayMs) {
      this.userResponse = response;
      this.userDelayMs = delayMs;
      return this;
    }

    Future<Void> start() {
      postServer = vertx.createHttpServer().requestHandler(request -> {
        request.response().putHeader("content-type", "application/json");
        if (postRawBody != null) {
          request.response().end(postRawBody);
        } else {
          request.response().end(postResponse != null ? postResponse.encode() : "{}");
        }
      });

      userServer = vertx.createHttpServer().requestHandler(request -> {
        if (userFailure) {
          request.response().setStatusCode(500).end("boom");
          return;
        }

        if (userDelayMs > 0) {
          vertx.setTimer(userDelayMs, ignored -> request.response()
            .putHeader("content-type", "application/json")
            .end(userResponse != null ? userResponse.encode() : "{}"));
          return;
        }

        request.response().putHeader("content-type", "application/json").end(userResponse != null ? userResponse.encode() : "{}");
      });

      return Future.all(postServer.listen(0), userServer.listen(0)).mapEmpty();
    }

    String postUrl() {
      return "http://localhost:" + postServer.actualPort() + "/posts/1";
    }

    String userUrl() {
      return "http://localhost:" + userServer.actualPort() + "/users/1";
    }
  }
}
