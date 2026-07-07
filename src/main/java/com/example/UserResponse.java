package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the user service response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserResponse(
  @JsonProperty("id") Integer id,
  @JsonProperty("name") String name,
  @JsonProperty("username") String username,
  @JsonProperty("email") String email
) {
}
