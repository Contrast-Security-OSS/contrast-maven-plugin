package com.contrastsecurity.maven.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.maven.plugin.sdkx.Scan;
import java.net.MalformedURLException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for {@link ContrastScanMojo} */
final class ContrastScanMojoTest {

  /**
   * Contrast MOJOs tolerate a variety of HTTP paths in the URL configuration. Regardless of the
   * path that the user has configured, the {@link ContrastScanMojo#createClickableScanURL(Scan)}
   * method should generate the same URL
   */
  @ValueSource(
      strings = {
        "https://app.contrastsecurity.com/",
        "https://app.contrastsecurity.com/Contrast",
        "https://app.contrastsecurity.com/Contrast/api",
        "https://app.contrastsecurity.com/Contrast/api/"
      })
  @ParameterizedTest
  void it_generates_clickable_url(final String url) throws MalformedURLException {
    // GIVEN a scan mojo with known URL, organization ID, and project ID
    final ContrastScanMojo mojo = new ContrastScanMojo();
    mojo.setURL(url);
    mojo.setOrganizationId("organization-id");
    mojo.setProjectId("project-id");

    // WHEN generate URL for the user to click-through to display the scan in their browser
    final String clickableScanURL = mojo.createClickableScanURL("scan-id").toExternalForm();

    // THEN outputs expected URL
    assertThat(clickableScanURL)
        .isEqualTo(
            "https://app.contrastsecurity.com/Contrast/static/ng/index.html#/organization-id/scans/project-id/scans/scan-id");
  }
}
