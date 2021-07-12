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

import java.util.Objects;

/**
 * Describes a code artifact uploaded to Contrast Scan
 *
 * <p>TODO[JG] JAVA-3298 move this to the Contrast Java SDK and flesh it out
 */
public final class CodeArtifact {

  /**
   * @param id unique ID for this code artifact
   * @return new code artifact
   */
  public static CodeArtifact of(final String id) {
    return new CodeArtifact(id);
  }

  private final String id;

  CodeArtifact(final String id) {
    this.id = Objects.requireNonNull(id);
  }

  /** @return unique ID of this code artifact */
  public String getId() {
    return id;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final CodeArtifact artifact = (CodeArtifact) o;
    return id.equals(artifact.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "CodeArtifact{" + "id='" + id + '\'' + '}';
  }
}
