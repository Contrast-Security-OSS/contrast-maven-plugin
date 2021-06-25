package com.contrastsecurity.maven.plugin;

/** Indicates an error occurred while using the Contrast HTTP API */
public class ContrastException extends RuntimeException {

  private final int status;

  /**
   * @param status HTTP status returned by Contrast API
   * @param message exception message
   */
  public ContrastException(final int status, final String message) {
    this(status, message, null);
  }

  /**
   * @param status HTTP status returned by Contrast API
   * @param message exception message
   * @param inner inner exception
   */
  public ContrastException(final int status, final String message, final Exception inner) {
    super(message, inner);
    this.status = status;
  }

  /** @return HTTP status returned by the Contrast API */
  public int getStatus() {
    return status;
  }
}
