package com.contrastsecurity.maven.plugin.sdkx;

import com.contrastsecurity.maven.plugin.sdkx.Scan.Status;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class AwaitScan {

  private final ContrastScanSDK contrast;
  private final ScheduledExecutorService scheduler;
  private final String organizationId;
  private final String projectId;
  private final String scanId;
  private final int delay;
  private final TimeUnit unit;

  AwaitScan(
      final ContrastScanSDK contrast,
      final ScheduledExecutorService scheduler,
      final String organizationId,
      final String projectId,
      final String scanId,
      final int delay,
      final TimeUnit unit) {
    this.contrast = contrast;
    this.scheduler = scheduler;
    this.organizationId = organizationId;
    this.projectId = projectId;
    this.scanId = scanId;
    this.delay = delay;
    this.unit = unit;
  }

  CompletableFuture<Scan> await() {
    return await(scheduler);
  }

  CompletableFuture<Scan> await(final Executor executor) {
    return CompletableFuture.supplyAsync(
            () -> contrast.getScanById(organizationId, projectId, scanId), executor)
        .thenCompose(
            scan -> {
              if (scan.getStatus() == Status.FAILED) {
                throw new ScanFailedException(scan.getErrorMessage());
              }
              return scan.isFinished()
                  ? CompletableFuture.completedFuture(scan)
                  : await(delayedExecutor(delay, unit, scheduler));
            });
  }

  private Executor delayedExecutor(long delay, TimeUnit unit, Executor executor) {
    return r -> scheduler.schedule(() -> executor.execute(r), delay, unit);
  }
}
