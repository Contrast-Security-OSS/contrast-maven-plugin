package com.contrastsecurity.maven.plugin.sdkx.scan;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.Scan;
import com.contrastsecurity.maven.plugin.sdkx.ScanSummary;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.maven.wagon.authentication.AuthenticationException;

public final class ScanOperation {

  static ScanOperation create(
      final ScheduledExecutorService scheduler,
      final ContrastScanSDK contrast,
      final Scan scan,
      final Duration interval) {
    // poll Scan API until the scan is completed, failed, or canceled
    final CompletableFuture<Scan> operation =
        new AwaitScan(
                contrast,
                scheduler,
                scan.getOrganizationId(),
                scan.getProjectId(),
                scan.getId(),
                interval)
            .await()
            .toCompletableFuture();

    // when scan is complete, fetch summary
    final CompletableFuture<ScanSummary> summary =
        operation.thenCompose(
            completed ->
                CompletableFuture.supplyAsync(
                    () -> {
                      try {
                        return contrast.getScanSummary(
                            completed.getOrganizationId(),
                            completed.getProjectId(),
                            completed.getId());
                      } catch (final IOException e) {
                        throw new UncheckedIOException("Failed to retrieve scan status", e);
                      } catch (final UnauthorizedException e) {
                        throw new IllegalStateException("Failed to authenticate to Contrast", e);
                      }
                    },
                    scheduler));
    return new ScanOperation(scan.getId(), operation, summary);
  }

  private final String id;
  private final CompletableFuture<Scan> operation;
  private final CompletableFuture<ScanSummary> summary;

  private ScanOperation(
      final String id,
      final CompletableFuture<Scan> operation,
      final CompletableFuture<ScanSummary> summary) {
    this.id = id;
    this.operation = operation;
    this.summary = summary;
  }

  public String id() {
    return id;
  }

  public CompletionStage<InputStream> sarif() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public CompletionStage<ScanSummary> summary() {
    return summary;
  }

  public CompletionStage<Void> saveResultsToFile(final Path file) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  public void hangup() {
    for (final CompletableFuture<?> stage : Arrays.asList(operation, summary)) {
      stage.toCompletableFuture().cancel(true);
      stage.cancel(true);
    }
  }
}
