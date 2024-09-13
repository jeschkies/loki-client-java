package io.github.jeschkies.loki.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import io.github.jeschkies.loki.LokiTestServer;
import io.github.jeschkies.loki.client.model.Data;
import io.github.jeschkies.loki.client.model.QueryResult;
import io.github.jeschkies.loki.client.model.Streams;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestIntegration {
  private static LokiClient client;
  private static LokiTestServer server;

  // TODO: maybe make this a resource
  @BeforeAll
  public static void setup() throws IOException {
    server = new LokiTestServer();
    client = new LokiClient(new LokiClientConfig(server.getUri(), Duration.ofSeconds(10)));
  }

  @Test
  void TestRoundTrip() throws IOException, LokiClientException {
    Instant start = Instant.now().minus(Duration.ofHours(3));
    Instant end = start.plus(Duration.ofHours(2));

    client.pushLogLine(
        "line foo", end.minus(Duration.ofMinutes(5)), ImmutableMap.of("test", "roundtrip"));
    client.flush();
    QueryResult result = client.rangeQuery("{test=\"roundtrip\"}", start, end);
    assertThat(result.getData().getResultType()).isEqualTo(Data.ResultType.Streams);
    assertThat(result.getData().getResult()).isInstanceOf(Streams.class);
    var streams = ((Streams) result.getData().getResult()).getStreams();
    assertThat(streams).hasSize(1);
    assertThat(streams.getFirst().values().getFirst().getLine()).isEqualTo("line foo");
  }

  @AfterAll
  public static void teardown() {
    server.close();
  }
}
