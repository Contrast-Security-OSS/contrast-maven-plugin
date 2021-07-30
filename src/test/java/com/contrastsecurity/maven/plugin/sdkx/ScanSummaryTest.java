package com.contrastsecurity.maven.plugin.sdkx;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.maven.plugin.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ScanSummary}. */
final class ScanSummaryTest {

  @Test
  void gson_deserialization_configuration() throws IOException {
    // WHEN deserialize scan summary with GSON
    final ScanSummary summary;
    final Gson gson = GsonFactory.create();
    try (InputStream is = Resources.stream("/scan-api/scans/scan-summary.json");
        Reader reader = new InputStreamReader(is)) {
      summary = gson.fromJson(reader, ScanSummary.class);
    }

    // THEN deserialized object matches expected summary
    final ScanSummary expected =
        ScanSummary.builder()
            .id("summary-id")
            .scanId("scan-id")
            .projectId("project-id")
            .organizationId("organization-id")
            .totalResults(10)
            .totalNewResults(8)
            .totalFixedResults(1)
            .createdDate(
                OffsetDateTime.of(2021, Month.JULY.getValue(), 15, 20, 36, 48, 0, ZoneOffset.UTC))
            .build();
    assertThat(summary).isEqualTo(expected);
  }
}
