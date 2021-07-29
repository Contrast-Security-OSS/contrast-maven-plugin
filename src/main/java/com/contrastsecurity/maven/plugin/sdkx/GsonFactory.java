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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Factory for configuring an instance of GSON that is compatible with the Scan API */
final class GsonFactory {

  static Gson create() {
    return new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
        .create();
  }

  /** static members only */
  private GsonFactory() {}

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
