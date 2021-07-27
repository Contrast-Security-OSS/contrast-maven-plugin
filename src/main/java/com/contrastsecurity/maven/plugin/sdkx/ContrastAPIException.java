package com.contrastsecurity.maven.plugin.sdkx;

/** Indicates an error occurred while using the Contrast HTTP API */
public class ContrastAPIException extends ContrastException {

  private final int status;

  /**
   * @param status HTTP status returned by Contrast API
   * @param message exception message
   */
  public ContrastAPIException(final int status, final String message) {
    this(status, message, null);
  }

  /**
   * @param status HTTP status returned by Contrast API
   * @param message exception message
   * @param inner inner exception
   */
  public ContrastAPIException(final int status, final String message, final Exception inner) {
    super(message, inner);
    this.status = status;
  }

  /** @return HTTP status returned by the Contrast API */
  public int getStatus() {
    return status;
  }
}
