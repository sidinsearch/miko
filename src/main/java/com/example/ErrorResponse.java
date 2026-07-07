package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standardized error payload returned to clients.
 */
public record ErrorResponse(
  @JsonProperty("error") String error
) {
}
