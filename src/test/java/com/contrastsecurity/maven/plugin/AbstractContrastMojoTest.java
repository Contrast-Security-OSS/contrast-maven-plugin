package com.contrastsecurity.maven.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.sdk.UserAgentProduct;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link AbstractContrastMojo}. */
final class AbstractContrastMojoTest {

  @Test
  void creates_user_agent_product_with_expected_values() {
    // GIVEN some AbstractContrastMojo with the mavenVersion property injected
    final AbstractContrastMojo mojo =
        new AbstractContrastMojo() {
          @Override
          public void execute() {}
        };
    mojo.setMavenVersion("3.8.1");

    // WHEN build User-Agent product
    final UserAgentProduct ua = mojo.getUserAgentProduct();

    // THEN has expected values
    assertThat(ua.name()).isEqualTo("contrast-maven-plugin");
    assertThat(ua.version()).matches("\\d+\\.\\d+(\\.\\d+)?(-SNAPSHOT)?");
    assertThat(ua.comment()).isEqualTo("Apache Maven 3.8.1");
  }
}
