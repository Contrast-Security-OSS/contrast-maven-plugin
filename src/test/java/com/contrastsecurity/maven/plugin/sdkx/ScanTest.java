package com.contrastsecurity.maven.plugin.sdkx;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.maven.plugin.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link Scan} */
final class ScanTest {

  private final Gson gson = GsonFactory.create();

  @Test
  void gson_deserialization_configuration() throws IOException {
    // WHEN deserialize with GSON
    final Scan scan = readScanResource("/scan-api/scans/scan-completed.json");

    // THEN has expected ID
    final Scan expected = Scan.createCompleted("scan-id", "project-id", "organization-id");
    assertThat(scan).isEqualTo(expected);
  }

  /** Verifies that completed scans and failed scans are finished */
  @ValueSource(
      strings = {
        "/scan-api/scans/scan-cancelled.json",
        "/scan-api/scans/scan-completed.json",
        "/scan-api/scans/scan-failed.json"
      })
  @ParameterizedTest
  void is_finished(final String path) throws IOException {
    // TODO scan-failed.json is a complete guess. We have not yet seen an example of a failed scan,
    // so we don't know exactly what the API will return
    final Scan scan = readScanResource(path);
    assertThat(scan.isFinished()).isTrue();
  }

  /** Verifies that waiting scans and running scans are not yet finished */
  @ValueSource(strings = {"/scan-api/scans/scan-waiting.json", "/scan-api/scans/scan-running.json"})
  @ParameterizedTest
  void is_not_yet_finished(final String path) throws IOException {
    final Scan scan = readScanResource(path);
    assertThat(scan.isFinished()).isFalse();
  }

  private Scan readScanResource(final String path) throws IOException {
    final Scan scan;
    try (InputStream is = Resources.stream(path)) {
      scan = gson.fromJson(new InputStreamReader(is), Scan.class);
    }
    return scan;
  }
}
