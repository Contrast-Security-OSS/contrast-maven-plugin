package com.contrastsecurity.maven.plugin.it.stub;

/*-
 * #%L
 * Contrast Maven Plugin
 * %%
 * Copyright (C) 2021 Contrast Security, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static com.contrastsecurity.maven.plugin.Resources.file;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;

/** Starts a JDK HTTP server that serves as a stubbed {@link ContrastAPI} */
final class FakeContrastAPI implements ContrastAPI {

  private HttpServer server;

  @Override
  public void start() {
    if (server != null) {
      throw new IllegalStateException("server already started");
    }
    try {
      server = HttpServer.create();
    } catch (final IOException e) {
      throw new IllegalStateException("failed to create new server", e);
    }
    final InetSocketAddress address = new InetSocketAddress("localhost", 0);

    // register stub handlers
    server.createContext(
        "/api/ng/" + ORGANIZATION_ID + "/agents/default/java",
        authenticateAndCleanup(FakeContrastAPI::downloadAgent));

    final String projects =
        String.join("/", "", "api", "sast", "organizations", ORGANIZATION_ID, "projects");
    server.createContext(
        projects,
        authenticateAndCleanup(
            exchange -> {
              switch (exchange.getRequestMethod()) {
                case "GET":
                  final Path path;
                  final String query = exchange.getRequestURI().getQuery();
                  if (query.contains("unique=true")
                      && query.contains("name=spring-test-application")) {
                    // return one matching project
                    path = file("/scan-api/projects/paged-project.json");
                  } else {
                    // return no projects
                    path = file("/scan-api/projects/empty-projects-page.json");
                  }
                  json(exchange, 200, path);
                  return;
                case "POST":
                  json(exchange, 201, file("/scan-api/projects/project.json"));
                  return;
                default:
                  status(exchange, 415);
              }
            }));
    server.createContext(
        String.join("/", projects, PROJECT_ID, "code-artifacts"),
        authenticate(json(201, file("/scan-api/code-artifacts/code-artifact.json"))));
    server.createContext(
        String.join("/", projects, PROJECT_ID, "scans"),
        authenticate(json(201, file("/scan-api/scans/scan-waiting.json"))));
    server.createContext(
        String.join("/", projects, PROJECT_ID, "scans", SCAN_ID),
        authenticate(json(200, file("/scan-api/scans/scan-completed.json"))));
    server.createContext(
        String.join("/", projects, PROJECT_ID, "scans", SCAN_ID, "summary"),
        authenticate(json(200, file("/scan-api/scans/scan-summary.json"))));
    server.createContext(
        String.join("/", projects, PROJECT_ID, "scans", SCAN_ID, "raw-output"),
        authenticate(json(200, file("/scan-api/sarif/empty.sarif.json"))));

    server.setExecutor(Executors.newSingleThreadExecutor());
    try {
      server.bind(address, 0);
    } catch (final IOException e) {
      throw new IllegalStateException("failed to bind server to a port", e);
    }
    server.start();
    // spin wait for the operating system to assign a port before returning
    while (server.getAddress().getPort() <= 0) {
      Thread.yield();
    }
  }

  @Override
  public ConnectionParameters connection() {
    if (server == null) {
      throw new IllegalStateException("server not yet initialized, must call start() first");
    }
    final InetSocketAddress address = server.getAddress();
    final String url = "http://" + address.getHostName() + ":" + address.getPort() + "/api";
    return ConnectionParameters.builder()
        .url(url)
        .username(USER_NAME)
        .apiKey(API_KEY)
        .serviceKey(SERVICE_KEY)
        .organizationID(ORGANIZATION_ID)
        .build();
  }

  @Override
  public void stop() {
    if (server == null) {
      throw new IllegalStateException("server is not started");
    }
    server.stop(0);
  }

  /**
   * Creates a new {@code HttpHandler} that sends an empty response with the given status code.
   *
   * @param code HTTP status code to return
   * @return new {@code HttpHandler}
   */
  private static HttpHandler status(final int code) {
    return cleanup(exchange -> status(exchange, code));
  }

  /**
   * With the given {@code HttpExchange}, sends an empty response with the given status code.
   *
   * @param exchange the {@code HttpExchange} to use to send the response
   * @param code HTTP status code to return
   */
  private static void status(final HttpExchange exchange, final int code) throws IOException {
    // ideally we would use response length -1 according to the HttpServer JavaDoc, but it's not
    // working as expected
    exchange.sendResponseHeaders(code, 0);
  }

  /**
   * Creates a new {@code HttpHandler} that sends a JSON response with the given status code.
   *
   * @param code HTTP status code to return
   * @param path JSON file to send in the response
   * @return new {@code HttpHandler}
   */
  private static HttpHandler json(final int code, final Path path) {
    return cleanup(exchange -> json(exchange, code, path));
  }

  /**
   * With the given {@code HttpExchange}, sends a JSON response with the given status code and JSON
   * file.
   *
   * @param exchange the {@code HttpExchange} to use to send the response
   * @param code HTTP status code to return
   * @param path JSON file to send in the response
   */
  private static void json(final HttpExchange exchange, final int code, final Path path) {
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    try {
      exchange.sendResponseHeaders(code, Files.size(path));
      Files.copy(path, exchange.getResponseBody());
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to send response", e);
    }
  }

  /**
   * Decorates the given handler with authentication logic.
   *
   * @param handler handler to decorate
   * @return new {@code HttpHandler}
   */
  private static HttpHandler authenticate(HttpHandler handler) {
    // local pair class for iterating over common header verification logic
    class ExpectedHeader {
      final String name;
      final String value;

      ExpectedHeader(final String name, final String value) {
        this.name = name;
        this.value = value;
      }
    }
    return exchange -> {
      final Headers headers = exchange.getRequestHeaders();
      final List<ExpectedHeader> expectedHeaders =
          Arrays.asList(
              new ExpectedHeader("API-Key", API_KEY),
              new ExpectedHeader("Authorization", AUTHORIZATION));
      for (final ExpectedHeader expected : expectedHeaders) {
        final String value = headers.getFirst(expected.name);
        if (value == null) {
          exchange.sendResponseHeaders(401, 0);
          return;
        }
        if (!expected.value.equals(value)) {
          exchange.sendResponseHeaders(403, 0);
          return;
        }
      }
      handler.handle(exchange);
    };
  }

  /**
   * Decorates an {@code HttpHandler} with clean-up semantics including discarding the entire
   * request body (because these endpoints don't consider the contents of the request bodies).
   *
   * @param handler handler to decorate
   * @return decorated {@code HttpHandler}
   */
  private static HttpHandler cleanup(final HttpHandler handler) {
    return exchange -> {
      try {
        discardRequest(exchange);
        handler.handle(exchange);
      } finally {
        exchange.close();
      }
    };
  }

  /**
   * Applies both the {@link #authenticate(HttpHandler)} and {@link #cleanup(HttpHandler)}
   * decorators
   *
   * @param handler handler to decorate
   * @return decorated {@code HttpHandler}
   */
  private static HttpHandler authenticateAndCleanup(final HttpHandler handler) {
    return cleanup(authenticate(handler));
  }

  /**
   * Sends the Contrast agent jar to the response in the given {@code HttpExchange}.
   *
   * @param exchange exchange to which the response will be sent
   */
  private static void downloadAgent(final HttpExchange exchange) {
    exchange.getResponseHeaders().add("Content-Type", "application/java-archive");
    final Path path = file("/contrast-agent.jar");
    try {
      exchange.sendResponseHeaders(200, Files.size(path));
      Files.copy(path, exchange.getResponseBody());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to send response", e);
    }
  }

  /**
   * Reads the request in its entirety. The Sun HTTP server requires that the entire request be read
   * before sending the response. Note, the {@link HttpExchange#close()} method may not fully read
   * the request.
   *
   * @param exchange the exchange with the request to discard
   */
  private static void discardRequest(final HttpExchange exchange) {
    final byte[] buffer = new byte[4096];
    try (InputStream is = exchange.getRequestBody()) {
      //noinspection StatementWithEmptyBody
      while (is.read(buffer) >= 0) {}
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read request", e);
    }
  }

  private static final String USER_NAME = "test-user";
  private static final String API_KEY = "test-api-key";
  private static final String SERVICE_KEY = "test-service-key";
  private static final String ORGANIZATION_ID = "organization-id";
  private static final String PROJECT_ID = "project-id";
  private static final String SCAN_ID = "scan-id";
  public static final String AUTHORIZATION =
      Base64.getEncoder()
          .encodeToString((USER_NAME + ":" + SERVICE_KEY).getBytes(StandardCharsets.US_ASCII));
}
