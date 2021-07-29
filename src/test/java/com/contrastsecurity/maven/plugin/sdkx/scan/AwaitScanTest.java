package com.contrastsecurity.maven.plugin.sdkx.scan;

/*-
 * #%L
 * Contrast Maven Plugin
 * %%
 * Copyright (C) 2021 Contrast Security, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.Scan;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    assertThat(future).succeedsWithin(TEST_TIMEOUT).isEqualTo(completed);
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
    assertThat(future).succeedsWithin(TEST_TIMEOUT).isEqualTo(completed);
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
    assertThat(future).failsWithin(TEST_TIMEOUT);
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

    // THEN completes exceptionally
    assertThat(future)
        .failsWithin(TEST_TIMEOUT)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(ScanException.class)
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

    // THEN completes exceptionally
    assertThat(future)
        .failsWithin(TEST_TIMEOUT)
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
  private static final Duration TEST_TIMEOUT = Duration.ofMillis(100);
}
