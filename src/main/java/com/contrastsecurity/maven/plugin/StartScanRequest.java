package com.contrastsecurity.maven.plugin;

import java.util.Objects;

/** TODO[JG] JAVA-3298 move this to the Contrast Java SDK and flesh it out */
public final class StartScanRequest {

  private final String codeArtifactId;

  private final String label;

  public StartScanRequest(final String codeArtifactId, final String label) {
    this.codeArtifactId = Objects.requireNonNull(codeArtifactId);
    this.label = Objects.requireNonNull(label);
  }

  public String getCodeArtifactId() {
    return codeArtifactId;
  }

  public String getLabel() {
    return label;
  }
}
