package com.contrastsecurity.maven.plugin.it.stub;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
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

    // TODO register Contrast API stub handlers
    // /ng/org-uuid/agents/default/java?jvm=1_6
    server.createContext("/", status(204));

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
    final String url = "http://" + address.getHostName() + ":" + address.getPort();
    return ConnectionParameters.builder()
        .url(url)
        .username(USER_NAME)
        .apiKey(API_KEY)
        .serviceKey(SERVICE_KEY)
        .build();
  }

  @Override
  public void stop() {
    if (server == null) {
      throw new IllegalStateException("server is not started");
    }
    server.stop(0);
  }

  public static HttpHandler status(final int status) {
    return exchange -> {
      // ideally we would use response length -1 according to the HttpServer JavaDoc, but it's not
      // working as expected
      exchange.sendResponseHeaders(status, 0);
      exchange.close();
    };
  }

  private static final String USER_NAME = "test-user";
  private static final String API_KEY = "test-api-key";
  private static final String SERVICE_KEY = "test-service-key";
}
