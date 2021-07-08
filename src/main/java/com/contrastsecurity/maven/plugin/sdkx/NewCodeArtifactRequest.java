package com.contrastsecurity.maven.plugin.sdkx;

import java.nio.file.Path;
import java.util.Objects;

/** Describes a request to create a new Contrast Scan code artifact */
public final class NewCodeArtifactRequest {

  /**
   * @param projectId unique ID of the Scan project in which to start a new scan
   * @param file the file to upload and save as a new code artifact
   * @return new {@code NewCodeArtifactRequest}
   */
  public static NewCodeArtifactRequest of(final String projectId, final Path file) {
    return new NewCodeArtifactRequest(projectId, file);
  }

  private final String projectId;
  private final Path file;

  /**
   * @param projectId unique ID of the Scan project in which to start a new scan
   * @param file the file to upload and save as a new code artifact
   */
  private NewCodeArtifactRequest(final String projectId, final Path file) {
    this.projectId = Objects.requireNonNull(projectId);
    this.file = Objects.requireNonNull(file);
  }

  /** @return unique ID of the Scan project in which to start a new scan */
  public String getProjectId() {
    return projectId;
  }

  /** @return the file to upload and save as a new code artifact */
  public Path getFile() {
    return file;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final NewCodeArtifactRequest that = (NewCodeArtifactRequest) o;
    return projectId.equals(that.projectId) && file.equals(that.file);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, file);
  }
}
