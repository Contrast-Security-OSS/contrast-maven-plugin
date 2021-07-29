package com.contrastsecurity.maven.plugin.sdkx;

import java.util.Objects;

/**
 * Describes a request to start a new scan for a known code artifact and Contrast Scan project
 *
 * <p>TODO[JG] JAVA-3298 move this to the Contrast Java SDK and flesh it out
 */
public final class StartScanRequest {

  /** @return new builder */
  public static Builder builder() {
    return new StartScanRequest.Builder();
  }

  /**
   * transient because this is an HTTP path param, but this is an implementation detail we want to
   * abstract from the user
   */
  private final transient String projectId;

  private final String codeArtifactId;
  private final String label;

  private StartScanRequest(
      final String projectId, final String codeArtifactId, final String label) {
    this.projectId = Objects.requireNonNull(projectId);
    this.codeArtifactId = Objects.requireNonNull(codeArtifactId);
    this.label = Objects.requireNonNull(label);
  }

  /** @return unique ID of the Scan project in which to start a new scan */
  public String getProjectId() {
    return projectId;
  }

  /** @return unique ID of the code artifact to scan */
  public String getCodeArtifactId() {
    return codeArtifactId;
  }

  /** @return label that distinguishes this scan from others in the project */
  public String getLabel() {
    return label;
  }

  /** Builder for {@link StartScanRequest} */
  public static final class Builder {

    private String projectId;
    private String codeArtifactId;
    private String label;

    /**
     * @param projectId unique ID of the Scan project in which to start a new scan
     * @return this
     */
    public Builder projectId(final String projectId) {
      this.projectId = projectId;
      return this;
    }

    /**
     * @param codeArtifactId unique ID of the code artifact to scan
     * @return this
     */
    public Builder codeArtifactId(final String codeArtifactId) {
      this.codeArtifactId = codeArtifactId;
      return this;
    }

    /**
     * @param label distinguishes this scan from others in the project
     * @return this
     */
    public Builder label(final String label) {
      this.label = label;
      return this;
    }

    /** @return new {@link StartScanRequest} */
    public StartScanRequest build() {
      return new StartScanRequest(projectId, codeArtifactId, label);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final StartScanRequest that = (StartScanRequest) o;
    return projectId.equals(that.projectId)
        && codeArtifactId.equals(that.codeArtifactId)
        && label.equals(that.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, codeArtifactId, label);
  }
}
