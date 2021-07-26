package com.contrastsecurity.maven.plugin.sdkx;

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
