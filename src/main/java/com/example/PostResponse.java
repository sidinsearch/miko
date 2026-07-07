package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the post service response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PostResponse(
  @JsonProperty("userId") Integer userId,
  @JsonProperty("id") Integer id,
  @JsonProperty("title") String title,
  @JsonProperty("body") String body
) {
}
