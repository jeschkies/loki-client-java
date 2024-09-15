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
package io.github.jeschkies.loki;

import com.google.common.io.Resources;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class LokiTestServer implements Closeable {
  private static final int LOKI_PORT = 3100;
  private static final String DEFAULT_VERSION = "3.1.0";

  public static  final String USER = "admin";
  public static final String PASSWORD = "password";
  public static final String LOKI_QUERY_API = "/loki/api/v1/query";

  private final DockerComposeContainer dockerCompose;

  public LokiTestServer() throws IOException {
    this(DEFAULT_VERSION, false);
  }

  public LokiTestServer(String version, boolean enableBasicAuth) throws IOException {
    File composeFile = File.createTempFile("loki-compose-", ".yaml");
    composeFile.deleteOnExit();
    Files.copy(
        Resources.asByteSource(Resources.getResource("compose.yaml")).openStream(),
        composeFile.toPath(),
        StandardCopyOption.REPLACE_EXISTING);
    this.dockerCompose =
        new DockerComposeContainer(composeFile)
            .withExposedService("loki", 3100)
            .waitingFor(
                "loki",
                Wait.forHttp("/ready").forResponsePredicate(response -> response.contains("ready")))
            .withStartupTimeout(Duration.ofSeconds(360));

    this.dockerCompose.withServices("loki").start();
  }

  public URI getUri() {
    return URI.create(
        "http://" + dockerCompose.getServiceHost("loki", LOKI_PORT) + ":" + LOKI_PORT + "/");
  }

  @Override
  public void close() {
    dockerCompose.close();
  }
}
