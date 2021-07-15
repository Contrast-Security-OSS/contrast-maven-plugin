package com.contrastsecurity.maven.plugin.sdkx.scan;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.Scan;
import com.contrastsecurity.maven.plugin.sdkx.ScanSummary;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

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
    return new ScanOperation(scheduler, contrast, scan.getId(), operation);
  }

  private final Executor executor;
  private final ContrastScanSDK contrast;
  private final String id;
  private final CompletableFuture<Scan> operation;

  private CompletableFuture<ScanSummary> summary;

  private ScanOperation(
      final Executor executor,
      final ContrastScanSDK contrast,
      final String id,
      final CompletableFuture<Scan> operation) {
    this.executor = executor;
    this.contrast = contrast;
    this.id = id;
    this.operation = operation;
  }

  public String id() {
    return id;
  }

  public CompletionStage<InputStream> sarif() {
    return operation.thenCompose(
        scan ->
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    return contrast.getSarif(
                        scan.getOrganizationId(), scan.getProjectId(), scan.getId());
                  } catch (final IOException e) {
                    throw new UncheckedIOException("Failed to retrieve SARIF", e);
                  } catch (final UnauthorizedException e) {
                    throw new IllegalStateException("Failed to authenticate to Contrast", e);
                  }
                },
                executor));
  }

  public synchronized CompletionStage<ScanSummary> summary() {
    if (summary == null || summary.isCompletedExceptionally()) {
      summary =
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
                      executor));
    }
    return summary;
  }

  public CompletionStage<Void> saveSarifToFile(final Path file) {
    return sarif()
        .thenCompose(
            is ->
                CompletableFuture.supplyAsync(
                    () -> {
                      try {
                        Files.copy(is, file);
                      } catch (final IOException e) {
                        throw new UncheckedIOException("Failed to save SARIF to file", e);
                      } finally {
                        try {
                          is.close();
                        } catch (final IOException ignored) {
                        }
                      }
                      return null;
                    },
                    executor));
  }

  public synchronized void hangup() {
    for (final CompletableFuture<?> stage : Arrays.asList(operation, summary)) {
      if (stage == null) {
        continue;
      }
      stage.toCompletableFuture().cancel(true);
      stage.cancel(true);
    }
  }
}
