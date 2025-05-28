package io.github.jeschkies.loki.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.google.common.collect.ImmutableMap;
import io.github.jeschkies.loki.LokiTestServer;
import io.github.jeschkies.loki.client.model.Data;
import io.github.jeschkies.loki.client.model.Matrix;
import io.github.jeschkies.loki.client.model.QueryResult;
import io.github.jeschkies.loki.client.model.Streams;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestIntegration {
  private static LokiClient client;
  private static LokiTestServer server;

  // TODO: maybe make this a resource
  @BeforeAll
  public static void setup() throws IOException {
    server = new LokiTestServer("3.2.0", false);
    client = new LokiClient(new LokiClientConfig(server.getUri(), Duration.ofSeconds(10)));
  }

  @Test
  void TestRoundTrip() throws IOException, LokiClientException {
    Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS).minus(Duration.ofHours(3));
    Instant end = start.plus(Duration.ofHours(2));

    client.pushLogLine(
        "line foo", start.plus(Duration.ofMinutes(4)), ImmutableMap.of("test", "roundtrip"));
    client.pushLogLine(
        "line bar", start.plus(Duration.ofMinutes(5)), ImmutableMap.of("test", "roundtrip"));
    client.flush();
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              QueryResult result = client.rangeQuery("{test=\"roundtrip\"}", start, end);
              assertThat(result.getData().getResultType()).isEqualTo(Data.ResultType.Streams);
              assertThat(result.getData().getResult()).isInstanceOf(Streams.class);
              var streams = ((Streams) result.getData().getResult()).getStreams();
              assertThat(streams).hasSize(1);
              assertThat(streams.getFirst().values()).hasSize(2);
              assertThat(streams.getFirst().values().getFirst().getLine()).isEqualTo("line foo");
            });

    QueryResult result = client.rangeQuery("{test=\"roundtrip\"}", start, end, 300, 1);
    assertThat(result.getData().getResultType()).isEqualTo(Data.ResultType.Streams);
    assertThat(result.getData().getResult()).isInstanceOf(Streams.class);
    var streams = ((Streams) result.getData().getResult()).getStreams();
    assertThat(streams).hasSize(1);
    assertThat(streams.getFirst().values()).hasSize(1);

    result = client.rangeQuery("count_over_time({test=\"roundtrip\"}[5m])", start, end, 300, 1);
    assertThat(result.getData().getResultType()).isEqualTo(Data.ResultType.Matrix);
    assertThat(result.getData().getResult()).isInstanceOf(Matrix.class);
    var metrics = ((Matrix) result.getData().getResult()).getMetrics();
    assertThat(metrics).hasSize(1);
    var values = metrics.getFirst().values();
    assertThat(values).hasSize(1);
    assertThat(values.getFirst().getValue()).isEqualTo(2.0);
    assertThat(Instant.ofEpochSecond(values.getFirst().getTs()))
        .isEqualTo(start.plus(Duration.ofMinutes(5)));
  }

  @Test
  void TestExpectedResultType() throws LokiClientException {
    var type = client.getExpectedResultType("{test=\"type\"}");
    assertThat(type).isEqualTo(Data.ResultType.Streams);

    type = client.getExpectedResultType("count_over_time({test=\"type\"}[5m])");
    assertThat(type).isEqualTo(Data.ResultType.Matrix);
  }

  @AfterAll
  public static void teardown() {
    server.close();
  }
}
