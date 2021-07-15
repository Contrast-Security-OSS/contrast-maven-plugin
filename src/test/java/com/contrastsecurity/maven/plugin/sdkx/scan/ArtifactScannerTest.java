package com.contrastsecurity.maven.plugin.sdkx.scan;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

final class ArtifactScannerTest {

  @Test
  void retrieves_results_after_scan_completes(@TempDir final Path tmp)
      throws UnauthorizedException, IOException {
    // GIVEN an ArtifactScanner with a stubbed ContrastScanSDK that successfully completes the scan
    final Path artifact = tmp.resolve("my-app.jar");
    final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    final ContrastScanSDK contrast = mock(ContrastScanSDK.class);
    final ArtifactScanner scanner =
        new ArtifactScanner(executor, contrast, ORGANIZATION_ID, PROJECT_ID, Duration.ofMillis(1));
    final NewCodeArtifactRequest newCodeArtifactRequest =
        NewCodeArtifactRequest.of(PROJECT_ID, artifact);
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
    final ScanSummary summary = new ScanSummary();
    when(contrast.getScanSummary(ORGANIZATION_ID, PROJECT_ID, SCAN_ID)).thenReturn(summary);

    // WHEN scan code artifact and retrieve results
    final ScanOperation operation = scanner.scanArtifact(artifact, "Artifact Scanner Test");

    // THEN requests summary after scan has completed
    assertThat(operation.summary()).succeedsWithin(Duration.ofMillis(100)).isEqualTo(summary);
    final InOrder inOrder = inOrder(contrast);
    inOrder.verify(contrast).createCodeArtifact(ORGANIZATION_ID, newCodeArtifactRequest);
    inOrder.verify(contrast).startScan(ORGANIZATION_ID, startScanRequest);
    inOrder.verify(contrast, times(2)).getScanById(ORGANIZATION_ID, PROJECT_ID, SCAN_ID);
    inOrder.verify(contrast).getScanSummary(ORGANIZATION_ID, PROJECT_ID, SCAN_ID);

    // AND saves sarif to file system
    final Path results = tmp.resolve("results.sarif");
    final CompletionStage<Void> save = operation.saveResultsToFile(results);
    assertThat(save).succeedsWithin(Duration.ofMillis(100));
    assertThat(results).hasContent("results");
  }

  private static final String ORGANIZATION_ID = "organization-id";
  private static final String PROJECT_ID = "project-id";
  private static final String CODE_ARTIFACT_ID = "code-artifact-id";
  private static final String SCAN_ID = "scan-id";
}
