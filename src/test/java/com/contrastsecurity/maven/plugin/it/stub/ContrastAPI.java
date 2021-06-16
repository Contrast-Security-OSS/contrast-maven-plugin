package com.contrastsecurity.maven.plugin.it.stub;

public interface ContrastAPI {

  static ContrastAPI createFromEnvironment() {
    // TODO add the ability to make a ContrastAPI that uses a real Contrast API
    return new FakeContrastAPI();
  }

  void start();

  ConnectionParameters connection();

  void stop();
}
