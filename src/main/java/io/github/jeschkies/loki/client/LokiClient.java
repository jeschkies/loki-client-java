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
package io.github.jeschkies.loki.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jeschkies.loki.client.model.Data;
import io.github.jeschkies.loki.client.model.QueryResult;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LokiClient {
  private final OkHttpClient httpClient;
  private final URI lokiEndpoint;

  private static final MediaType JsonMediaType = MediaType.parse("application/json");

  public LokiClient(LokiClientConfig config) {
    this.lokiEndpoint = config.uri();

    OkHttpClient.Builder clientBuilder =
        new OkHttpClient.Builder().readTimeout(Duration.ofMillis(config.readTimeout().toMillis()));
    this.httpClient = clientBuilder.build();
  }

  public QueryResult rangeQuery(String lokiQuery, Long start, Long end) throws LokiClientException {
    final URI uri =
        new HttpUrl.Builder()
            .scheme(this.lokiEndpoint.getScheme())
            .host(this.lokiEndpoint.getHost())
            .port(this.lokiEndpoint.getPort())
            .addQueryParameter("query", lokiQuery)
            .addQueryParameter("start", start.toString())
            .addQueryParameter("end", end.toString())
            .addQueryParameter("direction", "forward")
            .build()
            .uri();

    try (Response response = requestUri(uri)) {
      if (response.isSuccessful() && response.body() != null) {
        return QueryResult.fromJSON(response.body().byteStream());
      }
      throw new LokiClientException("Bad response " + response.code() + " " + response.message());
    } catch (IOException e) {
      throw new LokiClientException("Error reading range query", e);
    }
  }

  public void pushLogLine(String log, Instant timestamp, Map<String, String> labels)
      throws IOException, LokiClientException {
    final URI uri =
        new HttpUrl.Builder()
            .scheme(this.lokiEndpoint.getScheme())
            .host(this.lokiEndpoint.getHost())
            .port(this.lokiEndpoint.getPort())
            .addPathSegment("/loki/api/v1/push")
            .build()
            .uri();

    ObjectMapper mapper = new ObjectMapper();
    var root = mapper.createObjectNode();
    var streams = mapper.createArrayNode();
    var stream = mapper.createObjectNode();
    var lbls = mapper.createObjectNode();
    for (var pair : labels.entrySet()) {
      lbls.put(pair.getKey(), pair.getValue());
    }
    var values = mapper.createArrayNode();
    var line = mapper.createArrayNode();
    line.add(Time.nanosFromInstant(timestamp).toString());
    line.add(log);
    values.add(line);
    stream.set("stream", lbls);
    stream.set("values", values);
    streams.add(stream);
    root.set("streams", streams);

    String bodyStr = mapper.writeValueAsString(root);
    RequestBody body = RequestBody.create(bodyStr, JsonMediaType);

    Request.Builder requestBuilder =
        new Request.Builder()
            .post(body)
            // TODO .header("X-Scope-OrgID", "1")
            .url(uri.toString());

    try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
      if (!response.isSuccessful()) {
        String error = "";
        if (response.body() != null) {
          error = ": " + response.body().string();
        }
        throw new LokiClientException(
            "Bad response " + response.code() + " " + response.message() + error);
      }
    }
  }

  public void flush() throws IOException, LokiClientException {
    final URI uri =
        new HttpUrl.Builder()
            .scheme(this.lokiEndpoint.getScheme())
            .host(this.lokiEndpoint.getHost())
            .port(this.lokiEndpoint.getPort())
            .addPathSegment("/flush")
            .build()
            .uri();

    Request.Builder requestBuilder =
        new Request.Builder().post(RequestBody.create("", JsonMediaType)).url(uri.toString());

    try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
      if (!response.isSuccessful()) {
        throw new LokiClientException("Bad response " + response.code() + " " + response.message());
      }
    }
  }

  public Response requestUri(URI uri) throws IOException {
    Request.Builder requestBuilder = new Request.Builder().url(uri.toString());
    return httpClient.newCall(requestBuilder.build()).execute();
  }

  public Data.ResultType getExpectedResultType(String query) throws LokiClientException {
    // Execute instant query to determine whether the query is a log or metric expression.
    final URI uri =
        new HttpUrl.Builder()
            .scheme(this.lokiEndpoint.getScheme())
            .host(this.lokiEndpoint.getHost())
            .port(this.lokiEndpoint.getPort())
            .addPathSegment("/loki/api/v1/query")
            .addQueryParameter("query", query)
            .build()
            .uri();

    try (Response response = requestUri(uri)) {
      if (response.isSuccessful() && response.body() != null) {
        return deserializeResultType(response.body().byteStream());
      }
      throw new LokiClientException("Bad response " + response.code() + " " + response.message());
    } catch (IOException e) {
      throw new LokiClientException("Error reading instant query", e);
    }
  }

  private Data.ResultType deserializeResultType(InputStream input) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    var node = mapper.readTree(input);
    if (Objects.equals(node.get("data").get("resultType").asText(), "streams")) {
      return Data.ResultType.Streams;
    } else {
      return Data.ResultType.Matrix;
    }
  }
}
