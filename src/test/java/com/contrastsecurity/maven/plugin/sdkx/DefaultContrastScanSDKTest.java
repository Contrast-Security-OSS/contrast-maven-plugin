package com.contrastsecurity.maven.plugin.sdkx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.HttpMethod;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link DefaultContrastScanSDK} */
final class DefaultContrastScanSDKTest {

  /**
   * Verifies that all well-known Java archive types are identified as a Java archive mime type
   *
   * <ul>
   *   <li>https://en.wikipedia.org/wiki/JAR_(file_format)
   *   <li>https://en.wikipedia.org/wiki/WAR_(file_format)
   *   <li>https://en.wikipedia.org/wiki/EAR_(file_format)
   * </ul>
   *
   * @param name file name
   */
  @ValueSource(strings = {"foo.jar", "foo.war", "foo.ear"})
  @ParameterizedTest
  void determine_content_type_java_archive(final String name) throws IOException {
    final Path file = Paths.get(name);
    final String mime = DefaultContrastScanSDK.determineMime(file);
    assertThat(mime).isEqualTo("application/java-archive");
  }

  /** Verifies that unknown file extensions use the generic application/octet-stream mime type */
  @Test
  void determine_content_type_unknown() throws IOException {
    final Path file = Paths.get("foo");
    final String mime = DefaultContrastScanSDK.determineMime(file);
    assertThat(mime).isEqualTo("application/octet-stream");
  }

  /**
   * Verifies that the {@code findProjectByName} method properly escapes the project name (because
   * we don't have nice URL builders 😢).
   */
  @Test
  void find_project_url_encodes_project_name() throws IOException, UnauthorizedException {
    final ContrastSDK contrastSDK = mock(ContrastSDK.class);
    final DefaultContrastScanSDK scanSDK =
        new DefaultContrastScanSDK(contrastSDK, "https://app.contrastsecurity.com/Contrast/api");
    when(contrastSDK.makeRequest(any(HttpMethod.class), anyString()))
        .thenReturn(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));

    // WHEN find project by name with a name that has characters that must be URL escaped
    scanSDK.findProjectByName("organization-id", "🍔&🍟");

    // THEN URL escapes the project name when building the HTTP URL
    verify(contrastSDK)
        .makeRequest(
            HttpMethod.GET,
            "/sast/organizations/organization-id/projects?unique=true&name=%F0%9F%8D%94%26%F0%9F%8D%9F");
  }
}
