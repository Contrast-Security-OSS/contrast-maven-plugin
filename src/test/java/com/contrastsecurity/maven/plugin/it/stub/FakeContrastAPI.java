package com.contrastsecurity.maven.plugin.it.stub;

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
        authenticatedEndpoint(FakeContrastAPI::downloadAgent));

    final String projects =
        String.join("/", "", "api", "sast", "organizations", ORGANIZATION_ID, "projects");
    server.createContext(
        String.join("/", projects, PROJECT_ID, "code-artifacts"),
        authenticatedEndpoint(json(201, file("/scan-api/code-artifacts/code-artifact.json"))));
    server.createContext(
        String.join("/", projects, PROJECT_ID, "scans"),
        authenticatedEndpoint(json(201, file("/scan-api/scans/scan-waiting.json"))));
    server.createContext(
        String.join("/", projects, PROJECT_ID, "scans", SCAN_ID),
        authenticatedEndpoint(json(200, file("/scan-api/scans/scan-completed.json"))));
    server.createContext(
        String.join("/", projects, PROJECT_ID, "scans", SCAN_ID, "summary"),
        authenticatedEndpoint(json(200, file("/scan-api/scans/scan-summary.json"))));
    server.createContext(
        String.join("/", projects, PROJECT_ID, "scans", SCAN_ID, "raw-output"),
        authenticatedEndpoint(json(200, file("/scan-api/sarif/empty.sarif.json"))));

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

  private static HttpHandler status(final int status) {
    return exchange -> {
      discardRequest(exchange);
      // ideally we would use response length -1 according to the HttpServer JavaDoc, but it's not
      // working as expected
      exchange.sendResponseHeaders(status, 0);
      exchange.close();
    };
  }

  private static HttpHandler json(final int code, final Path path) {
    return exchange -> {
      discardRequest(exchange);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      try {
        exchange.sendResponseHeaders(code, Files.size(path));
        Files.copy(path, exchange.getResponseBody());
      } catch (final IOException e) {
        throw new UncheckedIOException("Failed to send response", e);
      } finally {
        exchange.close();
      }
    };
  }

  private static HttpHandler authenticatedEndpoint(HttpHandler handler) {
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

  private static void downloadAgent(final HttpExchange exchange) {
    discardRequest(exchange);
    exchange.getResponseHeaders().add("Content-Type", "application/java-archive");
    final Path path = file("/contrast-agent.jar");
    try {
      exchange.sendResponseHeaders(200, Files.size(path));
      Files.copy(path, exchange.getResponseBody());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to send response", e);
    }
    exchange.close();
  }

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
