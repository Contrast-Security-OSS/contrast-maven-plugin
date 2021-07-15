package com.contrastsecurity.maven.plugin.sdkx.scan;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.Scan;
import com.contrastsecurity.maven.plugin.sdkx.Scan.Status;
import com.contrastsecurity.maven.plugin.sdkx.ScanFailedException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
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

  AwaitScan(
      final ContrastScanSDK contrast,
      final ScheduledExecutorService scheduler,
      final String organizationId,
      final String projectId,
      final String scanId,
      final Duration delay) {
    this.contrast = contrast;
    this.scheduler = scheduler;
    this.organizationId = organizationId;
    this.projectId = projectId;
    this.scanId = scanId;
    this.delay = delay;
  }

  /** @return {@link CompletableFuture} */
  CompletionStage<Scan> await() {
    return await(scheduler);
  }

  /**
   * Visible for testing
   *
   * @param executor
   * @return
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
              if (scan.getStatus() == Status.FAILED) {
                throw new ScanFailedException(scan.getErrorMessage());
              }
              return scan.isFinished()
                  ? CompletableFuture.completedFuture(scan)
                  : await(delayedExecutor(delay, scheduler));
            });
  }

  private Executor delayedExecutor(Duration delay, Executor executor) {
    return r ->
        scheduler.schedule(() -> executor.execute(r), delay.toMillis(), TimeUnit.MILLISECONDS);
  }
}
