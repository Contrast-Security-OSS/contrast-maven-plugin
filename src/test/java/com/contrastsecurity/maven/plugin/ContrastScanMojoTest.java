package com.contrastsecurity.maven.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.contrastsecurity.maven.plugin.sdkx.ScanSummary;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/** Unit tests for {@link ContrastScanMojo} */
final class ContrastScanMojoTest {

  private ContrastScanMojo mojo;

  @BeforeEach
  void before() {
    mojo = new ContrastScanMojo();
    mojo.setOrganizationId("organization-id");
    mojo.setProjectId("project-id");
  }

  /**
   * Contrast MOJOs tolerate a variety of HTTP paths in the URL configuration. Regardless of the
   * path that the user has configured, the {@link ContrastScanMojo#createClickableScanURL} method
   * should generate the same URL
   */
  @ValueSource(
      strings = {
        "https://app.contrastsecurity.com/",
        "https://app.contrastsecurity.com/Contrast",
        "https://app.contrastsecurity.com/Contrast/api",
        "https://app.contrastsecurity.com/Contrast/api/"
      })
  @ParameterizedTest
  void it_generates_clickable_url(final String url) throws MojoExecutionException {
    // GIVEN a scan mojo with known URL, organization ID, and project ID
    mojo.setURL(url);

    // WHEN generate URL for the user to click-through to display the scan in their browser
    final String clickableScanURL = mojo.createClickableScanURL("scan-id").toExternalForm();

    // THEN outputs expected URL
    assertThat(clickableScanURL)
        .isEqualTo(
            "https://app.contrastsecurity.com/Contrast/static/ng/index.html#/organization-id/scans/project-id/scans/scan-id");
  }

  @Test
  void it_prints_summary_to_console() {
    // GIVEN the plugin is configured to output scan results to the console
    mojo.setConsoleOutput(true);

    // WHEN print summary to console
    final ScanSummary summary =
        ScanSummary.builder()
            .id("summary-id")
            .organizationId("organization-id")
            .projectId("project-id")
            .scanId("scan-id")
            .createdDate(LocalDateTime.now())
            .totalResults(10)
            .totalNewResults(8)
            .totalFixedResults(1)
            .duration(0)
            .build();
    @SuppressWarnings("unchecked")
    final Consumer<String> console = mock(Consumer.class);
    mojo.writeSummaryToConsole(summary, console);

    // THEN prints expected lines
    final List<String> expected =
        Arrays.asList("Scan completed", "New Results\t8", "Fixed Results\t1", "Total Results\t10");
    final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(console, times(4)).accept(captor.capture());
    assertThat(captor.getAllValues()).hasSameElementsAs(expected);
  }

  @Test
  void it_may_be_configured_to_omit_summary_from_console() {
    // GIVEN the plugin is configured to omit scan results from the console
    mojo.setConsoleOutput(false);

    // WHEN print summary to console
    final ScanSummary summary =
        ScanSummary.builder()
            .id("summary-id")
            .organizationId("organization-id")
            .projectId("project-id")
            .scanId("scan-id")
            .createdDate(LocalDateTime.now())
            .totalResults(10)
            .totalNewResults(8)
            .totalFixedResults(1)
            .duration(0)
            .build();
    @SuppressWarnings("unchecked")
    final Consumer<String> console = mock(Consumer.class);
    mojo.writeSummaryToConsole(summary, console);

    // THEN only prints "completed" line
    verify(console).accept("Scan completed");
  }
}
