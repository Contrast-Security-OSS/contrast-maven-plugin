package com.contrastsecurity.maven.plugin.sdkx.scan;

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

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.maven.plugin.sdkx.CodeArtifact;
import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.NewCodeArtifactRequest;
import com.contrastsecurity.maven.plugin.sdkx.Scan;
import com.contrastsecurity.maven.plugin.sdkx.StartScanRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Facilitates analyzing code artifacts with Contrast Scan. Uploads the code artifact to Contrast
 * Scan, starts a new scan, and provides means for retrieving the results of that scan when it has
 * completed.
 */
public final class ArtifactScanner {

  private final ScheduledExecutorService scheduler;
  private final ContrastScanSDK contrast;
  private final String organizationId;
  private final String projectId;
  private final Duration interval;

  /**
   * @param scheduler for scheduling scan status retrievals and all other retrievals communication
   *     with the Scan API
   * @param contrast for communicating with the Contrast Scan API
   * @param organizationId unique ID of the organization in which the project exists
   * @param projectId unique ID of the Contrast Scan project
   * @param interval the polling interval for retrieving the status of a not-yet-finished scan
   */
  public ArtifactScanner(
      final ScheduledExecutorService scheduler,
      final ContrastScanSDK contrast,
      final String organizationId,
      final String projectId,
      final Duration interval) {
    this.scheduler = Objects.requireNonNull(scheduler);
    this.contrast = Objects.requireNonNull(contrast);
    this.organizationId = Objects.requireNonNull(organizationId);
    this.projectId = Objects.requireNonNull(projectId);
    this.interval = Objects.requireNonNull(interval);
  }

  /**
   * Uploads the given code artifact to Contrast and starts a new scan.
   *
   * @param file code artifact to analyze
   * @param label distinguishes this scan from others in your project
   * @return new {@link ScanOperation} for retrieving scan results
   */
  public ScanOperation scanArtifact(final Path file, final String label) {
    final NewCodeArtifactRequest newCodeArtifactRequest =
        NewCodeArtifactRequest.of(projectId, file);
    final CodeArtifact artifact;
    try {
      artifact = contrast.createCodeArtifact(organizationId, newCodeArtifactRequest);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to upload code artifact", e);
    } catch (final UnauthorizedException e) {
      throw new IllegalStateException("Failed to authenticate to Contrast", e);
    }
    final StartScanRequest startScanRequest =
        StartScanRequest.builder()
            .label(label)
            .projectId(projectId)
            .codeArtifactId(artifact.getId())
            .build();
    final Scan scan;
    try {
      scan = contrast.startScan(organizationId, startScanRequest);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to start scan", e);
    } catch (final UnauthorizedException e) {
      throw new IllegalStateException("Failed to authenticate to Contrast", e);
    }
    return ScanOperation.create(scheduler, contrast, scan, interval);
  }
}
