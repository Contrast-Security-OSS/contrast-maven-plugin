package com.contrastsecurity.maven.plugin.it.stub;

import java.util.Objects;

final class RealContrastAPI implements ContrastAPI {

  private final ConnectionParameters connection;

  public RealContrastAPI(final ConnectionParameters connection) {
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
