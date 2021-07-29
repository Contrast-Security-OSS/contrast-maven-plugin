package com.contrastsecurity.maven.plugin.sdkx;

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

import com.contrastsecurity.maven.plugin.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.Month;
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
            .createdDate(LocalDateTime.of(2021, Month.JULY, 15, 20, 36, 48))
            .build();
    assertThat(summary).isEqualTo(expected);
  }
}
