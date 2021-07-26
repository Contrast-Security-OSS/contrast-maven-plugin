package com.contrastsecurity.maven.plugin.sdkx;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link GsonFactory} */
final class GsonFactoryTest {

  /**
   * Verifies that the {@code Gson} returned by the factor has requisite adapters for dealing with
   * {@link java.time.LocalDateTime} types
   */
  @Test
  void parse_iso8601_strings_to_local_date_time() {
    // GIVEN a new GSON instance that should handle java.time.LocalDateTime
    final Gson gson = GsonFactory.create();

    // WHEN deserialize a type containing a LocalDateTime
    final Obj obj = gson.fromJson("{\"time\": \"1955-11-12T22:04:00\"}", Obj.class);

    final LocalDateTime expected = LocalDateTime.of(1955, 11, 12, 22, 4, 0);
    assertThat(obj.time).isEqualTo(expected);
  }

  /** GSON cannot work with local classes */
  private static final class Obj {
    LocalDateTime time;
  }
}
