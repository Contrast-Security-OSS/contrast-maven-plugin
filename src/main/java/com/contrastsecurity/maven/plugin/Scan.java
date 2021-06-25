package com.contrastsecurity.maven.plugin;

import java.util.Objects;

/** TODO[JG] JAVA-3298 move this to the Contrast Java SDK and flesh it out */
public final class Scan {

  private final String id;

  public Scan(final String id) {
    this.id = Objects.requireNonNull(id);
  }

  public String getID() {
    return id;
  }
}
