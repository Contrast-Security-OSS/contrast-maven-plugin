package com.contrastsecurity.maven.plugin.sdkx.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.Scan;
import com.contrastsecurity.maven.plugin.sdkx.ScanFailedException;
import com.contrastsecurity.maven.plugin.sdkx.ScanSummary;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link ScanOperation}.
 *
 * <p>Uses a mocked {@link ContrastScanSDK} to simulate various failure conditions.
 */
final class ScanOperationTest {

  private ScheduledExecutorService scheduler;
  private ScanOperation operation;
  private ContrastScanSDK contrast;
  private Scan scan;

  @BeforeEach
  void before() {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    contrast = mock(ContrastScanSDK.class);
    scan = Scan.createWaiting("scan-id", "project-id", "organization-id");
  }

  @AfterEach
  void after() {
    operation.hangup();
    scheduler.shutdownNow();
  }

  @Test
  void results_available_once_scan_completes(@TempDir final Path tmp)
      throws UnauthorizedException, IOException {
    // GIVEN the scan eventually completes
    final Scan waiting = this.scan;
    final Scan running = waiting.toRunning();
    final Scan completed = running.toCompleted();
    when(contrast.getScanById(waiting.getOrganizationId(), waiting.getProjectId(), waiting.getId()))
        .thenReturn(waiting, running, running, completed);
    final ScanSummary summary = new ScanSummary();
    when(contrast.getScanSummary(scan.getOrganizationId(), scan.getProjectId(), scan.getId()))
        .thenReturn(summary);
    when(contrast.getSarif(scan.getOrganizationId(), scan.getProjectId(), scan.getId()))
        .thenReturn(new ByteArrayInputStream("sarif".getBytes(StandardCharsets.UTF_8)));
    startScanOperation();

    // EXPECT summary available
    assertThat(operation.summary()).succeedsWithin(Duration.ofMillis(10)).isEqualTo(summary);

    // AND may save SARIF to file
    final Path results = tmp.resolve("results.sarif");
    final CompletionStage<Void> save = operation.saveSarifToFile(results);
    assertThat(save).succeedsWithin(Duration.ofMillis(100));
    assertThat(results).hasContent("sarif");
  }

  @Test
  void results_not_yet_available_when_scan_has_not_finish()
      throws UnauthorizedException, IOException {
    // GIVEN the Scan is stuck in the waiting state
    when(contrast.getScanById(scan.getOrganizationId(), scan.getProjectId(), scan.getId()))
        .thenReturn(scan);
    startScanOperation();

    // WHEN request results
    final CompletionStage<ScanSummary> summary = operation.summary();
    final CompletionStage<InputStream> sarif = operation.sarif();

    // THEN futures will not complete
    try {
      Thread.sleep(10);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted", e);
    }
    assertThat(summary).isNotCompleted();
    assertThat(sarif).isNotCompleted();
  }

  @Test
  void fails_to_retrieve_sarif_when_request_fails(@TempDir final Path tmp)
      throws UnauthorizedException, IOException {
    // GIVEN the scan eventually completes
    final Scan waiting = this.scan;
    final Scan running = waiting.toRunning();
    final Scan completed = running.toCompleted();
    when(contrast.getScanById(waiting.getOrganizationId(), waiting.getProjectId(), waiting.getId()))
        .thenReturn(waiting, running, running, completed);
    final ScanSummary summary = new ScanSummary();
    when(contrast.getScanSummary(scan.getOrganizationId(), scan.getProjectId(), scan.getId()))
        .thenReturn(summary);
    // AND the request for the sarif fails
    when(contrast.getSarif(scan.getOrganizationId(), scan.getProjectId(), scan.getId()))
        .thenThrow(new IOException("💥"));
    startScanOperation();

    // EXPECT summary available
    assertThat(operation.summary()).succeedsWithin(Duration.ofMillis(10)).isEqualTo(summary);

    // AND SARIF fails
    final Path results = tmp.resolve("results.sarif");
    final CompletionStage<Void> save = operation.saveSarifToFile(results);
    assertThat(save).failsWithin(Duration.ofMillis(100));
    assertThat(results).doesNotExist();
  }

  @Test
  void fails_to_retrieve_results_when_scan_fails() throws UnauthorizedException, IOException {
    // GIVEN the scan eventually fails
    final Scan waiting = this.scan;
    final Scan running = waiting.toRunning();
    final Scan failed = running.toFailed("DNS again");
    when(contrast.getScanById(waiting.getOrganizationId(), waiting.getProjectId(), waiting.getId()))
        .thenReturn(waiting, running, running, failed);
    startScanOperation();

    // WHEN request results
    final CompletionStage<ScanSummary> summary = operation.summary();
    final CompletionStage<InputStream> sarif = operation.sarif();

    // THEN futures complete exceptionally
    for (final CompletionStage<?> result : Arrays.asList(summary, sarif)) {
      assertThat(result)
          .failsWithin(Duration.ofMillis(10))
          .withThrowableOfType(ExecutionException.class)
          .havingCause()
          .isInstanceOf(ScanFailedException.class)
          .withMessage("DNS again");
    }
  }

  @Test
  void ceases_to_poll_for_scan_updates_after_hangup() throws UnauthorizedException, IOException {
    // GIVEN the Scan is stuck in the waiting state
    when(contrast.getScanById(scan.getOrganizationId(), scan.getProjectId(), scan.getId()))
        .thenReturn(scan);
    startScanOperation();

    // WHEN hangup operation
    operation.hangup();

    // THEN operation ceases to poll for updates
    // sleep a little to allow executor's queue to clear out
    try {
      Thread.sleep(10);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted", e);
    }
    final List<Runnable> runnables = scheduler.shutdownNow();
    assertThat(runnables).isEmpty();

    // AND results complete exceptionally
    for (final CompletionStage<?> result : Arrays.asList(operation.summary(), operation.sarif())) {
      assertThat(result).failsWithin(Duration.ofMillis(10));
    }
  }

  private void startScanOperation() {
    final Duration interval = Duration.ofMillis(1);
    operation = ScanOperation.create(scheduler, contrast, scan, interval);
    assertThat(operation.id()).isEqualTo(scan.getId());
  }
}
