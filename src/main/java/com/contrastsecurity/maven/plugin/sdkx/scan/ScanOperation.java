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

/**
 * Provides methods for asynchronously waiting for a scan to complete and retrieving its results
 * when it has.
 */
public final class ScanOperation {

  /**
   * Static factory for creating a new {@code ScanOperation}
   *
   * @param scheduler for scheduling scan status retrievals and all other retrievals communication
   *     with the Scan API
   * @param contrast for communicating with the Contrast Scan API
   * @param scan the scan on which the new {@code ScanOperation} operates
   * @param interval the polling interval for retrieving the status of a not-yet-finished scan
   * @return new {@code ScanOperation}
   */
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

  /** @return scan ID */
  public String id() {
    return id;
  }

  /**
   * Retrieves a summary of the scan results.
   *
   * @return {@link CompletionStage} that completes successfully when the scan has completed and the
   *     summary has been retrieved
   */
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

  /**
   * Retrieves and scan's results in <a href="https://sarifweb.azurewebsites.net">SARIF</a>
   *
   * @return {@link CompletionStage} that completes successfully when the scan has completed and the
   *     results stream is available to consume. The caller is expected to close the {@code
   *     InputStream}
   */
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

  /**
   * Retrieves and saves the scan's results (in <a
   * href="https://sarifweb.azurewebsites.net">SARIF</a>) to the specified file
   *
   * @param file the file to which to save the results
   * @return {@link CompletionStage} that completes successfully when the scan has completed and the
   *     results file has been saved
   */
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

  /**
   * Disconnects from the Contrast Scan API. Stops polling for the latest scan status.
   *
   * <p>This method is deliberately not named {@code cancel}, because the Contrast Scan API supports
   * a "cancel scan" operation and this class will likely add a corresponding {@code cancel} method
   * in the future as it is needed.
   */
  public synchronized void disconnect() {
    for (final CompletableFuture<?> stage : Arrays.asList(operation, summary)) {
      if (stage == null) {
        continue;
      }
      stage.toCompletableFuture().cancel(true);
      stage.cancel(true);
    }
  }
}
