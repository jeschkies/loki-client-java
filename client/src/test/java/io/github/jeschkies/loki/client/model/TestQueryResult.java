/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.jeschkies.loki.client.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.io.Resources;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestQueryResult {
  @Test
  void testDeserializeStreams() throws IOException {
    final InputStream input =
        Resources.asByteSource(Resources.getResource("streams.json")).openStream();
    QueryResult result = QueryResult.fromJSON(input);

    assertThat(result.getData().getResultType()).isEqualTo(Data.ResultType.Streams);
    assertThat(result.getData().getResult()).isInstanceOf(Streams.class);
    var streams = ((Streams) result.getData().getResult()).getStreams();
    assertThat(streams).hasSize(3);
    Assertions.assertThat(streams.getFirst().values()).hasSize(89);
  }

  @Test
  void testDeserializeEmptyStreams() throws IOException {
    String json = "{\"status\":\"success\",\"data\":{\"resultType\":\"streams\",\"result\":[]}}";
    final InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    QueryResult result = QueryResult.fromJSON(input);

    assertThat(result.getData().getResultType()).isEqualTo(Data.ResultType.Streams);
    assertThat(result.getData().getResult()).isInstanceOf(Streams.class);
    var streams = ((Streams) result.getData().getResult()).getStreams();
    assertThat(streams).hasSize(0);
  }

  @Test
  void testDeserializeEmptyMatrix() throws IOException {
    String json = "{\"status\":\"success\",\"data\":{\"resultType\":\"matrix\",\"result\":[]}}";
    final InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    QueryResult result = QueryResult.fromJSON(input);

    assertThat(result.getData().getResultType()).isEqualTo(Data.ResultType.Matrix);
    assertThat(result.getData().getResult()).isInstanceOf(Matrix.class);
    var metrics = ((Matrix) result.getData().getResult()).getMetrics();
    assertThat(metrics).hasSize(0);
  }

  @Test
  void testDeserializeMatrix() throws IOException {
    final InputStream input =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("matrix.json");
    QueryResult result = QueryResult.fromJSON(input);

    assertThat(result.getData().getResultType()).isEqualTo(Data.ResultType.Matrix);
    assertThat(result.getData().getResult()).isInstanceOf(Matrix.class);
    var metrics = ((Matrix) result.getData().getResult()).getMetrics();
    assertThat(metrics).hasSize(4);
    assertThat(metrics.getFirst().values()).hasSize(22);
  }
}
