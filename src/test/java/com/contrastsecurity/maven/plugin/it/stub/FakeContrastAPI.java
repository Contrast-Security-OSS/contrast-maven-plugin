package com.contrastsecurity.maven.plugin.it.stub;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
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
    server.createContext(
        "/api/sast/organizations/" + ORGANIZATION_ID + "/projects",
        authenticatedEndpoint(FakeContrastAPI::projects));
    server.createContext(
        "/api/sast/organizations/"
            + ORGANIZATION_ID
            + "/projects/"
            + PROJECT_ID
            + "/code-artifacts",
        authenticatedEndpoint(FakeContrastAPI::uploadCodeArtifact));
    server.createContext(
        "/api/sast/organizations/" + ORGANIZATION_ID + "/projects/" + PROJECT_ID + "/scans",
        authenticatedEndpoint(FakeContrastAPI::startScan));

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

  /**
   * This code is extracted to its own method so that we can break up this code into multiple
   * statements which we cannot do when it is in a try-with-resources in the {@link
   * #downloadAgent(HttpExchange)} method.
   *
   * @return new {@link BufferedInputStream} for reading the Contrast agent JAR from the file system
   */
  private static BufferedInputStream readContrastAgentJAR() throws FileNotFoundException {
    final InputStream is = FakeContrastAPI.class.getResourceAsStream("/contrast-agent.jar");
    if (is == null) {
      throw new FileNotFoundException("Failed to find contrast-agent.jar testing resource");
    }
    return new BufferedInputStream(is);
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
    // read the contrast-agent.jar into memory, because we need to know the size of the content that
    // we'll send over the wire since we are not using chunked responses with this simple JDK HTTP
    // server
    final byte[] jar;
    try (BufferedInputStream bis = readContrastAgentJAR();
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      final byte[] buffer = new byte[4096];
      int read;
      while ((read = bis.read(buffer)) != -1) {
        bos.write(buffer, 0, read);
      }
      jar = bos.toByteArray();
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read contrast-agent from file system", e);
    }
    // send jar in the response
    try {
      exchange.getResponseHeaders().add("Content-Type", "application/java-archive");
      exchange.sendResponseHeaders(200, jar.length);
      exchange.getResponseBody().write(jar);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to send response", e);
    }
    exchange.close();
  }

  private static void projects(final HttpExchange exchange) {
    final String json =
        "{\n"
            + "    \"id\": \"2f35cd90-b73e-44c5-8bb0-533afdbb07d5\",\n"
            + "    \"organizationId\": \"d89d9103-89c7-4a74-968e-a8b93ea8f7bb\",\n"
            + "    \"projectId\": \"02faf17f-0db6-4d0f-b6dc-0d9c00473ff2\",\n"
            + "    \"filename\": \"spring-async.war\",\n"
            + "    \"createdTime\": \"2021-06-08T15:46:03.748+00:00\"\n"
            + "}";
    final byte[] body = json.getBytes(StandardCharsets.UTF_8);
    try {
      exchange.sendResponseHeaders(201, body.length);
      exchange.getResponseBody().write(body);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to send response", e);
    }
    exchange.close();
  }

  private static void uploadCodeArtifact(final HttpExchange exchange) {
    discardRequest(exchange);
    final String json =
        "{\n"
            + "    \"id\": \""
            + CODE_ARTIFACT_ID
            + "\",\n"
            + "    \"organizationId\": \""
            + ORGANIZATION_ID
            + "\",\n"
            + "    \"projectId\": \""
            + PROJECT_ID
            + "\",\n"
            + "    \"filename\": \"spring-async.war\",\n"
            + "    \"createdTime\": \"2021-06-08T15:46:03.748+00:00\"\n"
            + "}";
    final byte[] body = json.getBytes(StandardCharsets.UTF_8);
    try {
      exchange.sendResponseHeaders(201, body.length);
      exchange.getResponseBody().write(body);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to send response", e);
    }
    exchange.close();
  }

  private static void startScan(final HttpExchange exchange) {
    discardRequest(exchange);
    final String json = "{\"id\": \"scan-id\"}";
    final byte[] body = json.getBytes(StandardCharsets.UTF_8);
    try {
      exchange.sendResponseHeaders(201, body.length);
      exchange.getResponseBody().write(body);
    } catch (final IOException e) {
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
  private static final String CODE_ARTIFACT_ID = "code-artifact-id";
  public static final String AUTHORIZATION =
      Base64.getEncoder()
          .encodeToString((USER_NAME + ":" + SERVICE_KEY).getBytes(StandardCharsets.US_ASCII));
}
