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
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Project} */
final class ProjectTest {

  @Test
  void gson_deserialization_configuration() throws IOException {
    // WHEN deserialize paged projects with GSON
    final ScanPagedResult<Project> projects;
    final Gson gson = GsonFactory.create();
    try (InputStream is = Resources.stream("/scan-api/projects/paged-project.json");
        Reader reader = new InputStreamReader(is)) {
      final TypeToken<ScanPagedResult<Project>> token =
          new TypeToken<ScanPagedResult<Project>>() {};
      projects = gson.fromJson(reader, token.getType());
    }

    // THEN deserialized object matches expected paged projects
    final Project project =
        Project.builder()
            .id("project-id")
            .organizationId("organization-id")
            .name("spring-test-application")
            .archived(false)
            .language("JAVA")
            .critical(1)
            .high(2)
            .medium(3)
            .low(4)
            .note(5)
            .lastScanTime(
                OffsetDateTime.of(2021, Month.JULY.getValue(), 27, 18, 38, 1, 0, ZoneOffset.UTC))
            .completedScans(6)
            .lastScanId("scan-id")
            .includeNamespaceFilters(Collections.emptyList())
            .excludeNamespaceFilters(Collections.emptyList())
            .build();
    final ScanPagedResult<Project> expected =
        new ScanPagedResult<>(Collections.singletonList(project), 1);
    assertThat(projects).isEqualTo(expected);
  }
}
