package com.contrastsecurity.maven.plugin.sdkx.scan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.maven.plugin.sdkx.CodeArtifact;
import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.NewCodeArtifactRequest;
import com.contrastsecurity.maven.plugin.sdkx.Scan;
import com.contrastsecurity.maven.plugin.sdkx.ScanSummary;
import com.contrastsecurity.maven.plugin.sdkx.StartScanRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

/**
 * Unit tests for {@link ArtifactScanner}. Mockist style tests mock the {@link ContrastScanSDK} and
 * verify that the {@code ArtifactScanner} interacts with it as expected.
 *
 * <p>The complexity of monitoring a running scan and retrieving results is encapsulated in {@link
 * ScanOperation} and not this class. Therefore, these tests cover the happy path and some failure
 * cases for starting the scan. The more interesting test cases are in {@link ScanOperationTest}.
 */
final class ArtifactScannerTest {

  private ScheduledExecutorService executor;
  private ContrastScanSDK contrast;
  private ArtifactScanner scanner;
  private Path artifact;
  private NewCodeArtifactRequest newCodeArtifactRequest;

  @TempDir Path tmp;

  @BeforeEach
  void before() {
    executor = Executors.newSingleThreadScheduledExecutor();
    contrast = mock(ContrastScanSDK.class);
    artifact = tmp.resolve("my-app.jar");
    scanner =
        new ArtifactScanner(executor, contrast, ORGANIZATION_ID, PROJECT_ID, Duration.ofMillis(1));
    newCodeArtifactRequest = NewCodeArtifactRequest.of(PROJECT_ID, artifact);
  }

  @AfterEach
  void after() {
    executor.shutdownNow();
  }

  /** Happy path test: verifies the full, complete scan operation */
  @Test
  void retrieves_results_after_scan_completes() throws UnauthorizedException, IOException {
    // GIVEN an ArtifactScanner with a stubbed ContrastScanSDK that successfully completes the scan
    when(contrast.createCodeArtifact(ORGANIZATION_ID, newCodeArtifactRequest))
        .thenReturn(CodeArtifact.of(CODE_ARTIFACT_ID));
    final Scan waiting = Scan.createWaiting(SCAN_ID, PROJECT_ID, ORGANIZATION_ID);
    final Scan running = waiting.toRunning();
    final Scan completed = running.toCompleted();
    final StartScanRequest startScanRequest =
        StartScanRequest.builder()
            .projectId(PROJECT_ID)
            .codeArtifactId(CODE_ARTIFACT_ID)
            .label("Artifact Scanner Test")
            .build();
    when(contrast.startScan(ORGANIZATION_ID, startScanRequest)).thenReturn(waiting);
    when(contrast.getScanById(ORGANIZATION_ID, PROJECT_ID, SCAN_ID)).thenReturn(running, completed);
    when(contrast.getSarif(ORGANIZATION_ID, PROJECT_ID, SCAN_ID))
        .thenReturn(new ByteArrayInputStream("results".getBytes(StandardCharsets.UTF_8)));
    final ScanSummary summary =
        ScanSummary.builder()
            .id("summary-id")
            .scanId(SCAN_ID)
            .projectId(PROJECT_ID)
            .organizationId(ORGANIZATION_ID)
            .createdDate(OffsetDateTime.now())
            .totalResults(10)
            .totalFixedResults(2)
            .totalNewResults(8)
            .build();
    when(contrast.getScanSummary(ORGANIZATION_ID, PROJECT_ID, SCAN_ID)).thenReturn(summary);

    // WHEN scan code artifact and retrieve results
    final ScanOperation operation = scanner.scanArtifact(artifact, "Artifact Scanner Test");

    // THEN requests summary after scan has completed
    assertThat(operation.summary()).succeedsWithin(TEST_TIMEOUT).isEqualTo(summary);
    final InOrder inOrder = inOrder(contrast);
    inOrder.verify(contrast).createCodeArtifact(ORGANIZATION_ID, newCodeArtifactRequest);
    inOrder.verify(contrast).startScan(ORGANIZATION_ID, startScanRequest);
    inOrder.verify(contrast, times(2)).getScanById(ORGANIZATION_ID, PROJECT_ID, SCAN_ID);
    inOrder.verify(contrast).getScanSummary(ORGANIZATION_ID, PROJECT_ID, SCAN_ID);

    // AND saves sarif to file system
    final Path results = tmp.resolve("results.sarif");
    final CompletionStage<Void> save = operation.saveSarifToFile(results);
    assertThat(save).succeedsWithin(Duration.ofMillis(100));
    assertThat(results).hasContent("results");
  }

  @Test
  void throws_when_code_artifact_upload_fails() throws UnauthorizedException, IOException {
    // WHEN the SDK encounters an error when uploading the code artifact
    when(contrast.createCodeArtifact(ORGANIZATION_ID, newCodeArtifactRequest))
        .thenThrow(new IOException("💥"));

    // THEN scan artifact throws
    assertThatThrownBy(() -> scanner.scanArtifact(artifact, "Test Label"))
        .isInstanceOf(UncheckedIOException.class)
        .hasCauseInstanceOf(IOException.class)
        .hasRootCauseMessage("💥");
  }

  @Test
  void throws_when_start_scan_fails() throws UnauthorizedException, IOException {
    // WHEN the SDK encounters an error when uploading the code artifact
    when(contrast.createCodeArtifact(ORGANIZATION_ID, newCodeArtifactRequest))
        .thenReturn(CodeArtifact.of(CODE_ARTIFACT_ID));
    when(contrast.startScan(
            ORGANIZATION_ID,
            StartScanRequest.builder()
                .projectId(PROJECT_ID)
                .codeArtifactId(CODE_ARTIFACT_ID)
                .label("Test Label")
                .build()))
        .thenThrow(new IOException("💥"));

    // THEN scan artifact throws
    assertThatThrownBy(() -> scanner.scanArtifact(artifact, "Test Label"))
        .isInstanceOf(UncheckedIOException.class)
        .hasCauseInstanceOf(IOException.class)
        .hasRootCauseMessage("💥");
  }

  private static final String ORGANIZATION_ID = "organization-id";
  private static final String PROJECT_ID = "project-id";
  private static final String CODE_ARTIFACT_ID = "code-artifact-id";
  private static final String SCAN_ID = "scan-id";

  /**
   * Reasonable amount of time to wait for a test future to resolve given that no actual IO is
   * happening
   */
  private static final Duration TEST_TIMEOUT = Duration.ofMillis(100);
}
