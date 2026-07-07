package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the /aggregate endpoint and translates service results into HTTP responses.
 */
public class AggregateHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LoggerFactory.getLogger(AggregateHandler.class);
  private static final String JSON_CONTENT_TYPE = "application/json";

  private final ApiService apiService;
  private final ObjectMapper objectMapper;

  public AggregateHandler(ApiService apiService) {
    this(apiService, new ObjectMapper());
  }

  AggregateHandler(ApiService apiService, ObjectMapper objectMapper) {
    this.apiService = apiService;
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(RoutingContext ctx) {
    HttpServerRequest request = ctx.request();
    apiService.fetchAggregate()
      .onSuccess(response -> writeSuccess(request, response))
      .onFailure(error -> writeError(request, error));
  }

  private void writeSuccess(HttpServerRequest request, AggregateResponse response) {
    try {
      request.response()
        .setStatusCode(200)
        .putHeader("content-type", JSON_CONTENT_TYPE)
        .end(objectMapper.writeValueAsString(response));
    } catch (Exception exception) {
      LOGGER.error("Failed to serialize aggregate response", exception);
      writeError(request, exception);
    }
  }

  private void writeError(HttpServerRequest request, Throwable error) {
    LOGGER.error("Aggregation request failed", error);

    try {
      request.response()
        .setStatusCode(500)
        .putHeader("content-type", JSON_CONTENT_TYPE)
        .end(objectMapper.writeValueAsString(new ErrorResponse("Failed to fetch external service")));
    } catch (Exception exception) {
      LOGGER.error("Failed to serialize error response", exception);
      request.response()
        .setStatusCode(500)
        .putHeader("content-type", JSON_CONTENT_TYPE)
        .end(new JsonObject().put("error", "Failed to fetch external service").encode());
    }
  }
}
