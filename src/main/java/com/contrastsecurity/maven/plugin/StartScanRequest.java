package com.contrastsecurity.maven.plugin;

import com.google.gson.annotations.SerializedName;

// TODO[JG] Fill this out
public class StartScanRequest {

  @SerializedName("codeArtifactId")
  private final String codeArtifactID;

  private final String label;

  public StartScanRequest(final String codeArtifactID, final String label) {
    this.codeArtifactID = codeArtifactID;
    this.label = label;
  }

  public String getCodeArtifactID() {
    return codeArtifactID;
  }

  public String getLabel() {
    return label;
  }
}
