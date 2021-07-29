package com.contrastsecurity.maven.plugin.sdkx;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
    final LocalDateTimeObj obj =
        gson.fromJson("{\"time\": \"1955-11-12T22:04:00\"}", LocalDateTimeObj.class);

    final LocalDateTime expected = LocalDateTime.of(1955, 11, 12, 22, 4, 0);
    assertThat(obj.time).isEqualTo(expected);
  }

  @Test
  void parse_null_to_local_date_time() {
    // GIVEN a new GSON instance that should handle java.time.LocalDateTime
    final Gson gson = GsonFactory.create();

    // WHEN deserialize a type containing a LocalDateTime that is null
    final LocalDateTimeObj obj = gson.fromJson("{\"time\": null}", LocalDateTimeObj.class);

    assertThat(obj.time).isNull();
  }

  /**
   * Verifies that the {@code Gson} returned by the factor has requisite adapters for dealing with
   * {@link java.time.ZonedDateTime} types.
   */
  @Test
  void parse_iso8601_zoned_strings_to_zoned_date_time() {
    // GIVEN a new GSON instance that should handle java.time.ZonedDateTime
    final Gson gson = GsonFactory.create();

    // WHEN deserialize a type containing a ZonedDateTime
    final ZonedDateTimeObj obj =
        gson.fromJson("{\"time\": \"1955-11-12T22:04:00.000+00:00\"}", ZonedDateTimeObj.class);

    final ZonedDateTime expected = ZonedDateTime.of(1955, 11, 12, 22, 4, 0, 0, ZoneOffset.UTC);
    assertThat(obj.time).isEqualTo(expected);
  }

  @Test
  void parse_null_to_zoned_date_time() {
    // GIVEN a new GSON instance that should handle java.time.ZonedDateTime
    final Gson gson = GsonFactory.create();

    // WHEN deserialize a type containing a ZonedDateTime that is null
    final ZonedDateTimeObj obj = gson.fromJson("{\"time\": null}", ZonedDateTimeObj.class);

    assertThat(obj.time).isNull();
  }

  /** GSON cannot work with local classes */
  private static final class LocalDateTimeObj {
    LocalDateTime time;
  }

  /** GSON cannot work with local classes */
  private static final class ZonedDateTimeObj {
    ZonedDateTime time;
  }
}
