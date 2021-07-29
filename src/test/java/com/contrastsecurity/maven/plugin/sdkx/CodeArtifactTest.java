package com.contrastsecurity.maven.plugin.sdkx;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.maven.plugin.Resources;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CodeArtifact} */
final class CodeArtifactTest {

  @Test
  void gson_deserialization_configuration() throws IOException {
    // WHEN deserialize code-artifact JSON with GSON
    final Gson gson = GsonFactory.create();
    final CodeArtifact artifact;
    try (InputStream is = Resources.stream("/scan-api/code-artifacts/code-artifact.json")) {
      artifact = gson.fromJson(new InputStreamReader(is), CodeArtifact.class);
    }

    // THEN has expected ID
    final CodeArtifact expected = new CodeArtifact("code-artifact-id");
    assertThat(artifact).isEqualTo(expected);
  }
}
