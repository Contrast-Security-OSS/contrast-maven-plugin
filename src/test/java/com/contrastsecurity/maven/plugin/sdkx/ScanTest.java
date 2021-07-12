package com.contrastsecurity.maven.plugin.sdkx;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Scan} */
final class ScanTest {

  @Test
  void gson_deserialization_configuration() {
    // GIVEN some JSON for a scan
    final String id = "scan-id";
    final String json = "{\"id\": \"" + id + "\"}";

    // WHEN deserialize with GSON
    final Scan scan = new Gson().fromJson(new StringReader(json), Scan.class);

    // THEN has expected ID
    final Scan expected = new Scan(id);
    assertThat(scan).isEqualTo(expected);
  }
}
