package com.contrastsecurity.maven.plugin.sdkx;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CodeArtifact} */
final class CodeArtifactTest {

  @Test
  void gson_deserialization_configuration() {
    // GIVEN some JSON for a Code Artifact
    final String id = "code-artifact-id";
    final String json = "{\"id\": \"" + id + "\"}";

    // WHEN deserialize with GSON
    final CodeArtifact artifact = new Gson().fromJson(new StringReader(json), CodeArtifact.class);

    // THEN has expected ID
    final CodeArtifact expected = new CodeArtifact(id);
    assertThat(artifact).isEqualTo(expected);
  }
}
