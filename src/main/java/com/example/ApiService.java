package com.example;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles communication with external REST APIs using non-blocking asynchronous calls
 * protected by a Circuit Breaker.
 */
public class ApiService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApiService.class);

  private final WebClient webClient;
  private final CircuitBreaker circuitBreaker;
  private final String postUrl;
  private final String userUrl;

  public ApiService(Vertx vertx, JsonObject config) {
    long timeoutMs = config.getLong("request.timeout.ms", 5000L);
    this.postUrl = config.getString("post.url", "https://jsonplaceholder.typicode.com/posts/1");
    this.userUrl = config.getString("user.url", "https://jsonplaceholder.typicode.com/users/1");

    WebClientOptions clientOptions = new WebClientOptions()
      .setConnectTimeout((int) timeoutMs)
      .setIdleTimeout((int) (timeoutMs / 1000));
    this.webClient = WebClient.create(vertx, clientOptions);

    CircuitBreakerOptions breakerOptions = new CircuitBreakerOptions()
      .setMaxFailures(config.getInteger("circuit.breaker.max.failures", 3))
      .setTimeout(config.getLong("circuit.breaker.timeout.ms", timeoutMs))
      .setFallbackOnFailure(true)
      .setResetTimeout(config.getLong("circuit.breaker.reset.timeout.ms", 10000L));
    this.circuitBreaker = CircuitBreaker.create("external-api-breaker", vertx, breakerOptions);
  }

  /**
   * Fetches post and user data in parallel and combines them into an AggregateResponse.
   */
  public Future<AggregateResponse> fetchAggregate() {
    Future<PostResponse> postFuture = fetchPost();
    Future<UserResponse> userFuture = fetchUser();

    return Future.all(postFuture, userFuture)
      .map(composite -> {
        PostResponse post = composite.resultAt(0);
        UserResponse user = composite.resultAt(1);
        LOGGER.info("Successfully fetched upstream post {} by user {} in parallel", post.id(), user.id());
        return new AggregateResponse(
          "Vert.x 5 Asynchronous API Gateway Assessment",
          "Siddharth Shinde");
      })
      .onFailure(error -> LOGGER.error("Parallel aggregation failed: {}", error.getMessage()));
  }

  private Future<PostResponse> fetchPost() {
    return circuitBreaker.execute(promise -> webClient.getAbs(postUrl)
      .as(BodyCodec.jsonObject())
      .send()
      .onSuccess(response -> {
        if (response.statusCode() == 200 && response.body() != null) {
          try {
            promise.complete(new PostResponse(
              response.body().getInteger("userId"),
              response.body().getInteger("id"),
              response.body().getString("title"),
              response.body().getString("body")));
          } catch (Exception exception) {
            promise.fail("Malformed JSON response from posts API: " + exception.getMessage());
          }
        } else {
          promise.fail("Posts API returned status " + response.statusCode());
        }
      })
      .onFailure(error -> {
        LOGGER.warn("Failed to fetch post data: {}", error.getMessage());
        promise.fail(error);
      }));
  }

  private Future<UserResponse> fetchUser() {
    return circuitBreaker.execute(promise -> webClient.getAbs(userUrl)
      .as(BodyCodec.jsonObject())
      .send()
      .onSuccess(response -> {
        if (response.statusCode() == 200 && response.body() != null) {
          try {
            promise.complete(new UserResponse(
              response.body().getInteger("id"),
              response.body().getString("name"),
              response.body().getString("username"),
              response.body().getString("email")));
          } catch (Exception exception) {
            promise.fail("Malformed JSON response from users API: " + exception.getMessage());
          }
        } else {
          promise.fail("Users API returned status " + response.statusCode());
        }
      })
      .onFailure(error -> {
        LOGGER.warn("Failed to fetch user data: {}", error.getMessage());
        promise.fail(error);
      }));
  }

  public Future<Void> close() {
    circuitBreaker.close();
    webClient.close();
    return Future.succeededFuture();
  }
}
