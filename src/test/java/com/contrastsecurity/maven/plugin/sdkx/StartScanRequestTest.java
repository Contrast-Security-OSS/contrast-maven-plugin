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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link StartScanRequest} */
final class StartScanRequestTest {

  @Test
  void gson_serialization_test() {
    // GIVEN a start scan request
    final StartScanRequest request =
        StartScanRequest.builder()
            .projectId("project-id")
            .codeArtifactId("code-artifact-id")
            .label("label")
            .build();

    // WHEN serialize with GSON
    final JsonElement jsonEl = GsonFactory.create().toJsonTree(request);

    // THEN JSON includes expected fields
    assertThat(jsonEl.isJsonObject()).isTrue();
    final JsonObject jsonObj = jsonEl.getAsJsonObject();
    assertThat(jsonObj.get("codeArtifactId").getAsString()).isEqualTo(request.getCodeArtifactId());
    assertThat(jsonObj.get("label").getAsString()).isEqualTo(request.getLabel());
    // AND does not include project ID because that is part of the HTTP path
    assertThat(jsonObj.has("projectId")).isFalse();
  }
}
