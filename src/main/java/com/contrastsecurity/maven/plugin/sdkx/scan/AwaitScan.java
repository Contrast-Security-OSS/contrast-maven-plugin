package com.contrastsecurity.maven.plugin.sdkx.scan;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.Scan;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Method object that encapsulates a "wait for a Scan to complete" operation. */
final class AwaitScan {

  private final ContrastScanSDK contrast;
  private final ScheduledExecutorService scheduler;
  private final String organizationId;
  private final String projectId;
  private final String scanId;
  private final Duration delay;

  /**
   * @param contrast {@code ContrastScanSDK} for requesting the latest status of the scan
   * @param scheduler {@code ScheduledExecutorService} for scheduling tasks such as polling for the
   *     latest scan status
   * @param organizationId user's unique organization ID
   * @param projectId unique ID of the project in which the scan has been created
   * @param scanId unique ID of the scan to await
   * @param delay delay between status checks
   */
  AwaitScan(
      final ContrastScanSDK contrast,
      final ScheduledExecutorService scheduler,
      final String organizationId,
      final String projectId,
      final String scanId,
      final Duration delay) {
    this.contrast = Objects.requireNonNull(contrast);
    this.scheduler = Objects.requireNonNull(scheduler);
    this.organizationId = Objects.requireNonNull(organizationId);
    this.projectId = Objects.requireNonNull(projectId);
    this.scanId = Objects.requireNonNull(scanId);
    this.delay = Objects.requireNonNull(delay);
  }

  /**
   * @return {@code CompletionStage} that resolves successfully with a {@code Scan} record when the
   *     scan has completed, or resolves exceptionally with a {@link ScanException} when the scan
   *     has failed or there was a problem communicating with the Contrast Scan API.
   */
  CompletionStage<Scan> await() {
    return await(scheduler);
  }

  /**
   * Visible for testing.
   *
   * @param executor for executing blocking calls to the Contrast Scan API
   * @return {@code CompletionStage} that resolves successfully with a {@code Scan} record when the
   *     scan has completed, or resolves exceptionally with a {@link ScanException} when the scan
   *     has failed or there was a problem communicating with the Contrast Scan API.
   */
  CompletionStage<Scan> await(final Executor executor) {
    return CompletableFuture.supplyAsync(
            () -> {
              try {
                return contrast.getScanById(organizationId, projectId, scanId);
              } catch (final IOException e) {
                throw new UncheckedIOException(e);
              } catch (final UnauthorizedException e) {
                throw new IllegalStateException("Failed to authenticate to Contrast", e);
              }
            },
            executor)
        .thenCompose(
            scan -> {
              switch (scan.getStatus()) {
                case FAILED:
                  throw new ScanException(scan, scan.getErrorMessage());
                case CANCELLED:
                  throw new ScanException(scan, "Canceled");
                case COMPLETED:
                  return CompletableFuture.completedFuture(scan);
                default:
                  return await(delayedExecutor(delay, scheduler));
              }
            });
  }

  /**
   * @param delay the delay after which to execute runnables
   * @param executor the executor with which to execute runnables
   * @return new {@link Executor} that schedules runnables to execute in the future using the {@link
   *     #scheduler}.
   */
  private Executor delayedExecutor(Duration delay, Executor executor) {
    return r ->
        scheduler.schedule(() -> executor.execute(r), delay.toMillis(), TimeUnit.MILLISECONDS);
  }
}
