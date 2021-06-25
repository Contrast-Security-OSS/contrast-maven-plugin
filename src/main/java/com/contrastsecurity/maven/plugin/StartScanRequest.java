package com.contrastsecurity.maven.plugin;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

/** TODO[JG] JAVA-3298 move this to the Contrast Java SDK and flesh it out */
public final class StartScanRequest {

  @SerializedName("codeArtifactId")
  private final String codeArtifactID;

  private final String label;

  public StartScanRequest(final String codeArtifactID, final String label) {
    this.codeArtifactID = Objects.requireNonNull(codeArtifactID);
    this.label = Objects.requireNonNull(label);
  }

  public String getCodeArtifactID() {
    return codeArtifactID;
  }

  public String getLabel() {
    return label;
  }
}
