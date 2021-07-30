package com.contrastsecurity.maven.plugin.sdkx;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link GsonFactory} */
final class GsonFactoryTest {

  /**
   * Verifies that the {@code Gson} returned by the factory has requisite adapters for serializing
   * {@code null} references of {@link java.time.OffsetDateTime}.
   */
  @Test
  void write_null_string() {
    // GIVEN a new GSON instance that should handle java.time.OffsetDateTime
    final Gson gson = GsonFactory.create();

    // WHEN serialize a type containing a null OffsetDateTime
    final OffsetDateTimeObj obj = new OffsetDateTimeObj();
    final String json = gson.toJson(obj);

    assertThat(json).isEqualTo("{}");
  }

  /**
   * Verifies that the {@code Gson} returned by the factory has requisite adapters for serializing
   * instances of {@link java.time.OffsetDateTime}.
   */
  @Test
  void write_iso8601_string() {
    // GIVEN a new GSON instance that should handle java.time.OffsetDateTime
    final Gson gson = GsonFactory.create();

    // WHEN serialize a type containing a OffsetDateTime
    final OffsetDateTimeObj obj = new OffsetDateTimeObj();
    obj.time = OffsetDateTime.of(1955, 11, 12, 22, 4, 0, 0, ZoneOffset.UTC);
    final String json = gson.toJson(obj);

    assertThat(json).isEqualTo("{\"time\":\"1955-11-12T22:04:00Z\"}");
  }

  /**
   * Verifies that the {@code Gson} returned by the factory has requisite adapters for deserializing
   * instances of {@link java.time.OffsetDateTime}.
   */
  @ValueSource(strings = {"1955-11-12T22:04:00.000+00:00", "1955-11-12T22:04:00Z"})
  @ParameterizedTest
  void parse_iso8601_strings_to_offset_date_time(final String time) {
    // GIVEN a new GSON instance that should handle java.time.OffsetDateTime
    final Gson gson = GsonFactory.create();

    // WHEN deserialize a type containing a OffsetDateTime
    final OffsetDateTimeObj obj =
        gson.fromJson("{\"time\": \"" + time + "\"}", OffsetDateTimeObj.class);

    final OffsetDateTime expected = OffsetDateTime.of(1955, 11, 12, 22, 4, 0, 0, ZoneOffset.UTC);
    assertThat(obj.time).isEqualTo(expected);
  }

  /**
   * Verifies that the {@code Gson} returned by the factory has requisite adapters for deserializing
   * null references of {@link java.time.OffsetDateTime}.
   */
  @Test
  void parse_null_to_offset_date_time() {
    // GIVEN a new GSON instance that should handle java.time.OffsetDateTime
    final Gson gson = GsonFactory.create();

    // WHEN deserialize a type containing a OffsetDateTime that is null
    final OffsetDateTimeObj obj = gson.fromJson("{\"time\": null}", OffsetDateTimeObj.class);

    assertThat(obj.time).isNull();
  }

  /** GSON cannot work with local classes */
  private static final class OffsetDateTimeObj {
    OffsetDateTime time;
  }
}
