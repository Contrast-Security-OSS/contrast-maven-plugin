package com.contrastsecurity.maven.plugin.it.stub;

import java.util.Objects;

/**
 * {@link ContrastAPI} implementation that represents an external system. Methods that affect the
 * system such as {@code start()} and {@code stop()} are no-ops.
 */
final class ExternalContrastAPI implements ContrastAPI {

  private final ConnectionParameters connection;

  /** @param connection the connection parameters constant to provide to users */
  public ExternalContrastAPI(final ConnectionParameters connection) {
    this.connection = Objects.requireNonNull(connection);
  }

  /** nop */
  @Override
  public void start() {}

  @Override
  public ConnectionParameters connection() {
    return connection;
  }

  /** nop */
  @Override
  public void stop() {}
}
