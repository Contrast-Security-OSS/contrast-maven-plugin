package com.contrastsecurity.maven.plugin.sdkx.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.Scan;
import com.contrastsecurity.maven.plugin.sdkx.ScanFailedException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AwaitScan} */
final class AwaitScanTest {

  private ScheduledExecutorService scheduler;
  private ContrastScanSDK contrast;
  private AwaitScan as;

  @BeforeEach
  void before() {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    contrast = mock(ContrastScanSDK.class);
    as =
        new AwaitScan(
            contrast, scheduler, ORGANIZATION_ID, PROJECT_ID, SCAN_ID, Duration.ofMillis(1));
  }

  @AfterEach
  void after() {
    scheduler.shutdownNow();
  }

  @Test
  void completes_successfully_when_scan_is_completed() throws UnauthorizedException, IOException {
    // GIVEN the scan has already been completed
    final Scan completed = Scan.createCompleted(SCAN_ID, PROJECT_ID, ORGANIZATION_ID);
    when(contrast.getScanById(ORGANIZATION_ID, PROJECT_ID, SCAN_ID)).thenReturn(completed);

    // WHEN await scan to finish
    final CompletionStage<Scan> future = as.await();

    // THEN succeeds
    assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(completed);
  }

  @Test
  void completes_successfully_when_scan_eventually_completes()
      throws UnauthorizedException, IOException {
    // GIVEN the scan has not yet started, then starts, then completes
    final Scan waiting = Scan.createWaiting(SCAN_ID, PROJECT_ID, ORGANIZATION_ID);
    final Scan running = Scan.createRunning(SCAN_ID, PROJECT_ID, ORGANIZATION_ID);
    final Scan completed = Scan.createCompleted(SCAN_ID, PROJECT_ID, ORGANIZATION_ID);
    when(contrast.getScanById(ORGANIZATION_ID, PROJECT_ID, SCAN_ID))
        .thenReturn(waiting, running, completed);

    // WHEN await scan to finish
    final CompletionStage<Scan> future = as.await(scheduler);

    // THEN completes successfully
    assertThat(future).succeedsWithin(1, TimeUnit.SECONDS).isEqualTo(completed);
  }

  @Test
  void completes_exceptionally_when_scan_has_failed() throws UnauthorizedException, IOException {
    // GIVEN the scan has already failed
    final Scan failed =
        Scan.createFailed(SCAN_ID, PROJECT_ID, ORGANIZATION_ID, "DNS exploded again");
    when(contrast.getScanById(ORGANIZATION_ID, PROJECT_ID, SCAN_ID)).thenReturn(failed);

    // WHEN await scan to finish
    final CompletionStage<Scan> future = as.await();

    // THEN fails
    assertThat(future).failsWithin(1, TimeUnit.SECONDS);
  }

  @Test
  void completes_exceptionally_when_scan_eventually_fails()
      throws UnauthorizedException, IOException {
    // GIVEN the scan has not yet started, then starts, then fails
    final Scan waiting = Scan.createWaiting(SCAN_ID, PROJECT_ID, ORGANIZATION_ID);
    final Scan running = Scan.createRunning(SCAN_ID, PROJECT_ID, ORGANIZATION_ID);
    final Scan failed =
        Scan.createFailed(SCAN_ID, PROJECT_ID, ORGANIZATION_ID, "DNS exploded again");
    when(contrast.getScanById(ORGANIZATION_ID, PROJECT_ID, SCAN_ID))
        .thenReturn(waiting, running, failed);

    // WHEN await scan to finish
    final CompletionStage<Scan> future = as.await(scheduler);

    // THEN completes successfully
    assertThat(future)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ScanFailedException.class)
        .havingCause()
        .withMessage("DNS exploded again");
  }

  @Test
  void completes_exceptionally_when_get_scan_throws() throws UnauthorizedException, IOException {
    // GIVEN the scan has not yet started, then starts, then fails
    final Scan waiting = Scan.createWaiting(SCAN_ID, PROJECT_ID, ORGANIZATION_ID);
    final Scan running = Scan.createRunning(SCAN_ID, PROJECT_ID, ORGANIZATION_ID);
    when(contrast.getScanById(ORGANIZATION_ID, PROJECT_ID, SCAN_ID))
        .thenReturn(waiting, running, running)
        .thenThrow(new IOException("NIC melted"));

    // WHEN await scan to finish
    final CompletionStage<Scan> future = as.await(scheduler);

    // THEN completes successfully
    assertThat(future)
        .failsWithin(1, TimeUnit.SECONDS)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(UncheckedIOException.class)
        .havingCause()
        .withCauseExactlyInstanceOf(IOException.class)
        .havingCause()
        .withMessage("NIC melted");
  }

  private static final String ORGANIZATION_ID = "organization-id";
  private static final String PROJECT_ID = "project-id";
  private static final String SCAN_ID = "scan-id";
}
