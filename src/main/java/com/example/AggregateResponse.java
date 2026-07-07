package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response returned by the gateway endpoint.
 */
public record AggregateResponse(
  @JsonProperty("post_title") String postTitle,
  @JsonProperty("author_name") String authorName
) {
}
