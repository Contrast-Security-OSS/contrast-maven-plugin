package com.contrastsecurity.maven.plugin.it.stub;

public interface ContrastAPI {

  void start();

  ConnectionParameters connection();

  void stop();
}
