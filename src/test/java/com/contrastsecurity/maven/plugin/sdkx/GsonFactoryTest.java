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
