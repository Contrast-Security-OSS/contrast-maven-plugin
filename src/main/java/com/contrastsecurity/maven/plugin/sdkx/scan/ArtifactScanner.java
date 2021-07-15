package com.contrastsecurity.maven.plugin.sdkx.scan;

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

public final class ArtifactScanner {

  private final ScheduledExecutorService scheduler;
  private final ContrastScanSDK contrast;
  private final String organizationId;
  private final String projectId;

  public ArtifactScanner(
      final ScheduledExecutorService scheduler,
      final ContrastScanSDK contrast,
      final String organizationId,
      final String projectId) {
    this.scheduler = Objects.requireNonNull(scheduler);
    this.contrast = Objects.requireNonNull(contrast);
    this.organizationId = Objects.requireNonNull(organizationId);
    this.projectId = Objects.requireNonNull(projectId);
  }

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
      throw new UncheckedIOException("Failed to upload code artifact", e);
    } catch (final UnauthorizedException e) {
      throw new IllegalStateException("Failed to authenticate to Contrast", e);
    }
    return ScanOperation.create(scheduler, contrast, scan, Duration.ofSeconds(30));
  }
}
