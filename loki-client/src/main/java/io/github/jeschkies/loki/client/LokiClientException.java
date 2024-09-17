package io.github.jeschkies.loki.client;

public class LokiClientException extends Exception {
  public LokiClientException(String message) {
    super(message);
  }

  public LokiClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
