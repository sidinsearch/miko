package com.example;

import io.vertx.core.Vertx;

/**
 * Application entrypoint for mvn exec:java.
 */
public final class Main {
  private Main() {
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}