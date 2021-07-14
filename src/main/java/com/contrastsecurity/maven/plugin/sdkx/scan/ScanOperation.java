package com.contrastsecurity.maven.plugin.sdkx.scan;

import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.Scan;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;

public final class ScanOperation {

  static ScanOperation create(
      final ScheduledExecutorService scheduler, final ContrastScanSDK contrast, final Scan scan) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  private ScanOperation(
      final CompletableFuture<Scan> operation,
      final CompletableFuture<Object> summary,
      final CompletableFuture<Object> results) {
    this.operation = operation;
    this.summary = summary;
    this.results = results;
  }

  private final CompletableFuture<Scan> operation;
  private final CompletableFuture<Object> summary;
  private final CompletableFuture<Object> results;

  public CompletionStage<Object> results() {
    return results;
  }

  public CompletionStage<Object> summary() {
    return summary;
  }

  public CompletionStage<Void> saveResultsToFile(final Path file) {
    throw new UnsupportedOperationException("not yet implemented");
  }

  public void cancel() {
    for (final CompletableFuture<?> future : Arrays.asList(operation, summary, results)) {
      future.cancel(true);
    }
  }
}
