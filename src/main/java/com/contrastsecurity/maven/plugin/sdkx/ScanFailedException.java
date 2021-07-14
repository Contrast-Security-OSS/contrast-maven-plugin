package com.contrastsecurity.maven.plugin.sdkx;

public final class ScanFailedException extends RuntimeException {

  public ScanFailedException(final String message) {
    super(message);
  }
}
