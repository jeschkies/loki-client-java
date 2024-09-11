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
package io.trino.plugin.loki;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.airlift.http.client.HttpUriBuilder;
import io.trino.plugin.loki.model.Data;
import io.trino.plugin.loki.model.QueryResult;
import io.trino.spi.TrinoException;
import io.trino.spi.type.Type;
import io.trino.spi.type.TypeManager;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static io.trino.spi.StandardErrorCode.GENERIC_USER_ERROR;
import static io.trino.spi.type.TypeSignature.mapType;
import static io.trino.spi.type.VarcharType.VARCHAR;
import static java.util.Objects.requireNonNull;

public class LokiClient
{
    private final OkHttpClient httpClient;
    private final URI lokiEndpoint;

    private final Type varcharMapType;

    private static final MediaType JsonMediaType = MediaType.parse("application/json");

    public Type getVarcharMapType()
    {
        return varcharMapType;
    }

    @Inject
    public LokiClient(LokiConnectorConfig config, TypeManager typeManager)
    {
        this.lokiEndpoint = config.getLokiURI();
        requireNonNull(typeManager, "typeManager is null");

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder().readTimeout(Duration.ofMillis(config.getReadTimeout().toMillis()));
        setupBasicAuth(clientBuilder, config.getUser(), config.getPassword());
        this.httpClient = clientBuilder.build();
        varcharMapType = typeManager.getType(mapType(VARCHAR.getTypeSignature(), VARCHAR.getTypeSignature()));
    }

    private static void setupBasicAuth(OkHttpClient.Builder clientBuilder, Optional<String> user, Optional<String> password)
    {
        if (user.isPresent() && password.isPresent()) {
            clientBuilder.addInterceptor(basicAuth(user.get(), password.get()));
        }
    }

    private static Interceptor basicAuth(String user, String password)
    {
        requireNonNull(user, "user is null");
        requireNonNull(password, "password is null");
        if (user.contains(":")) {
            throw new TrinoException(GENERIC_USER_ERROR, "Illegal character ':' found in username");
        }

        String credential = Credentials.basic(user, password);
        return chain -> chain.proceed(chain.request().newBuilder()
                .header(AUTHORIZATION, credential)
                .build());
    }

    public QueryResult rangeQuery(String lokiQuery, Long start, Long end)
    {
        final URI uri =
                HttpUriBuilder.uriBuilderFrom(lokiEndpoint)
                        .appendPath("/loki/api/v1/query_range")
                        .addParameter("query", lokiQuery)
                        .addParameter("start", start.toString())
                        .addParameter("end", end.toString())
                        .addParameter("direction", "forward")
                        .build();

        try (Response response = requestUri(uri)) {
            if (response.isSuccessful() && response.body() != null) {
                return QueryResult.fromJSON(response.body().byteStream());
            }
            throw new TrinoException(LokiErrorCode.LOKI_UNKNOWN_ERROR, "Bad response " + response.code() + " " + response.message());
        }
        catch (IOException e) {
            throw new TrinoException(LokiErrorCode.LOKI_UNKNOWN_ERROR, "Error reading range query", e);
        }
    }

    public void pushLogLine(String log, Instant timestamp, Map<String, String> labels)
            throws IOException
    {
        final URI uri =
                HttpUriBuilder.uriBuilderFrom(lokiEndpoint)
                        .appendPath("/loki/api/v1/push")
                        .build();

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

        Request.Builder requestBuilder = new Request.Builder()
                .post(body)
                // TODO .header("X-Scope-OrgID", "1")
                .url(uri.toString());

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                String error = "";
                if (response.body() != null) {
                    error = ": " + response.body().string();
                }
                throw new TrinoException(LokiErrorCode.LOKI_UNKNOWN_ERROR, "Bad response " + response.code() + " " + response.message() + error);
            }
        }
    }

    public void flush()
            throws IOException
    {
        final URI uri =
                HttpUriBuilder.uriBuilderFrom(lokiEndpoint)
                        .appendPath("/flush")
                        .build();

        Request.Builder requestBuilder = new Request.Builder()
                .post(RequestBody.create("", JsonMediaType))
                .url(uri.toString());

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new TrinoException(LokiErrorCode.LOKI_UNKNOWN_ERROR, "Bad response " + response.code() + " " + response.message());
            }
        }
    }

    public Response requestUri(URI uri)
            throws IOException
    {
        Request.Builder requestBuilder = new Request.Builder().url(uri.toString());
        return httpClient.newCall(requestBuilder.build()).execute();
    }

    public Data.ResultType getExpectedResultType(String query)
    {
        // Execute instant query to determine whether the query is a log or metric expression.
        final URI uri =
                HttpUriBuilder.uriBuilderFrom(lokiEndpoint)
                        .appendPath("/loki/api/v1/query")
                        .addParameter("query", query)
                        .build();

        try (Response response = requestUri(uri)) {
            if (response.isSuccessful() && response.body() != null) {
                return deserializeResultType(response.body().byteStream());
            }
            throw new TrinoException(LokiErrorCode.LOKI_UNKNOWN_ERROR, "Bad response " + response.code() + " " + response.message());
        }
        catch (IOException e) {
            throw new TrinoException(LokiErrorCode.LOKI_UNKNOWN_ERROR, "Error reading instant query", e);
        }
    }

    private Data.ResultType deserializeResultType(InputStream input)
            throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        var node = mapper.readTree(input);
        if (Objects.equals(node.get("data").get("resultType").asText(), "streams")) {
            return Data.ResultType.Streams;
        }
        else {
            return Data.ResultType.Matrix;
        }
    }
}
