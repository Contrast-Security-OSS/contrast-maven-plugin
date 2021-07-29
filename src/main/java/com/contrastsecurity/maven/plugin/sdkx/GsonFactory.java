package com.contrastsecurity.maven.plugin.sdkx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Factory for configuring an instance of GSON that is compatible with the Scan API */
final class GsonFactory {

  static Gson create() {
    return new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter().nullSafe())
        .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeAdapter().nullSafe())
        .create();
  }

  /** static members only */
  private GsonFactory() {}

  private static final class ZonedDateTimeTypeAdapter extends TypeAdapter<ZonedDateTime> {

    @Override
    public void write(final JsonWriter writer, final ZonedDateTime value) throws IOException {
      final String formatted = value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
      writer.value(formatted);
    }

    @Override
    public ZonedDateTime read(final JsonReader reader) throws IOException {
      final String iso8601 = reader.nextString();
      return ZonedDateTime.parse(iso8601, DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }
  }

  private static final class LocalDateTimeTypeAdapter extends TypeAdapter<LocalDateTime> {

    @Override
    public void write(final JsonWriter writer, final LocalDateTime value) throws IOException {
      final String formatted = value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      writer.value(formatted);
    }

    @Override
    public LocalDateTime read(final JsonReader reader) throws IOException {
      final String iso8601 = reader.nextString();
      return LocalDateTime.parse(iso8601, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
  }
}
