package com.contrastsecurity.maven.plugin.sdkx;

/*-
 * #%L
 * Contrast Maven Plugin
 * %%
 * Copyright (C) 2021 Contrast Security, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
}
