package com.contrastsecurity.maven.plugin.sdkx;

public class ContrastException extends RuntimeException {

  public ContrastException(final String message) {
    super(message);
  }

  public ContrastException(final String message, final Throwable inner) {
    super(message, inner);
  }
}
