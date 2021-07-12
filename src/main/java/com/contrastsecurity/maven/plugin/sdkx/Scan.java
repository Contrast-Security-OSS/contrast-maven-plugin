package com.contrastsecurity.maven.plugin.sdkx;

import java.util.Objects;

/** TODO[JG] JAVA-3298 move this to the Contrast Java SDK and flesh it out */
public final class Scan {

  private final String id;

  Scan(final String id) {
    this.id = Objects.requireNonNull(id);
  }

  /** @return unique ID of this scan */
  public String getId() {
    return id;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Scan scan = (Scan) o;
    return id.equals(scan.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Scan{" + "id='" + id + '\'' + '}';
  }
}
