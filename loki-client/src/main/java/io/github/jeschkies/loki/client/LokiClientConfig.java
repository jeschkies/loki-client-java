package io.github.jeschkies.loki.client;

import java.net.URI;
import java.time.Duration;

public record LokiClientConfig(URI uri, Duration readTimeout, String tenantID) {

  public LokiClientConfig(URI uri, Duration readTimeout) {
    this(uri, readTimeout, null);
  }

  public LokiClientConfig {
    if (uri == null) {
      uri = URI.create("http://localhost:3100");
    }
    if (readTimeout == null) {
      readTimeout = Duration.ofSeconds(10);
    }
  }
}
