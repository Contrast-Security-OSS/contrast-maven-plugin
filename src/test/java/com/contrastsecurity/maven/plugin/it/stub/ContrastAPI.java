package com.contrastsecurity.maven.plugin.it.stub;

/** Describes a test instance of Contrast API to which tests may requests */
public interface ContrastAPI {

  /** starts the Contrast API instance */
  void start();

  /**
   * @return connection configuration necessary for making requests to this Contrast API instance
   */
  ConnectionParameters connection();

  /** stops the Contrast API instance */
  void stop();
}
