package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.maven.plugin.sdkx.CodeArtifact;
import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.NewCodeArtifactRequest;
import com.contrastsecurity.maven.plugin.sdkx.Scan;
import com.contrastsecurity.maven.plugin.sdkx.StartScanRequest;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Analyzes the Maven project's artifact with Contrast Scan to provide security insights
 *
 * @since 2.13
 */
@Mojo(
    name = "scan",
    defaultPhase = LifecyclePhase.INTEGRATION_TEST,
    requiresOnline = true,
    threadSafe = true)
public final class ContrastScanMojo extends AbstractContrastMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /**
   * Contrast Scan project unique ID to which the plugin runs new Scans. This will be replaced
   * imminently with a project name
   */
  // TODO[JAVA-3297] replace this with "project"
  @Parameter(required = true)
  private String projectId;

  /**
   * File path to the Java artifact to upload for scanning. By default, uses the path to this
   * module's Maven artifact produced in the {@code package} phase.
   */
  @Parameter(name = "artifactPath")
  private Path artifact;

  /** A label to distinguish this scan from others in your project */
  @Parameter(name = "label", defaultValue = "${project.version}")
  private String label;

  private ContrastSDK contrast;

  /** visible for testing */
  String getProjectId() {
    return projectId;
  }

  /** visible for testing */
  void setProjectId(final String projectId) {
    this.projectId = projectId;
  }

  @Override
  public void execute() throws MojoExecutionException {
    // initialize plugin
    initialize();
    final ContrastScanSDK contrastScan = new ContrastScanSDK(contrast, getURL());

    // check that file exists
    final Path file = artifact == null ? project.getArtifact().getFile().toPath() : artifact;
    if (!Files.exists(file)) {
      throw new MojoExecutionException(
          file
              + " does not exist. Make sure to bind the scan goal to a phase that will execute after the artifact to scan has been built");
    }
    final NewCodeArtifactRequest codeArtifactRequest = NewCodeArtifactRequest.of(projectId, file);
    getLog().info("Uploading " + file.getFileName() + " to Contrast Scan");

    // create new code artifact
    final CodeArtifact artifact;
    try {
      artifact = contrastScan.createCodeArtifact(getOrganizationId(), codeArtifactRequest);
    } catch (final UnauthorizedException e) {
      throw new MojoExecutionException("Failed to authenticate to Contrast", e);
    } catch (final IOException e) {
      throw new MojoExecutionException("Failed to upload artifact to Contrast Scan", e);
    }

    // start scan for new code artifact
    final StartScanRequest request =
        StartScanRequest.builder()
            .projectId(projectId)
            .codeArtifactId(artifact.getId())
            .label(label)
            .build();
    final Scan scan;
    try {
      scan = contrastScan.startScan(getOrganizationId(), request);
    } catch (final UnauthorizedException e) {
      throw new MojoExecutionException("Failed to authenticate to Contrast", e);
    } catch (final IOException e) {
      throw new MojoExecutionException(
          "Failed to start scan for code artifact " + artifact.getId(), e);
    }

    // show link in build log
    final URL clickableScanURL;
    try {
      clickableScanURL = createClickableScanURL(scan.getId());
    } catch (final MalformedURLException e) {
      throw new MojoExecutionException(
          "Error building clickable Scan URL. Please contact support@contrastsecurity.com for help",
          e);
    }
    getLog().info("Starting scan with label " + label);
    getLog()
        .info("Contrast Scan results will be available at " + clickableScanURL.toExternalForm());
  }

  /**
   * visible for testing
   *
   * @return Contrast browser application URL for users to click-through and see their scan results
   */
  URL createClickableScanURL(final String scanId) throws MalformedURLException {
    final URL url = new URL(getURL());
    final String path =
        String.join(
            "/",
            "",
            "Contrast",
            "static",
            "ng",
            "index.html#",
            getOrganizationId(),
            "scans",
            projectId,
            "scans",
            scanId);
    return new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
  }

  /**
   * Must be called after Maven has completed field injection.
   *
   * <p>I don't believe Maven has a post-injection callback that we bind to this method, so the
   * {@link #execute()} method calls this before continuing.
   *
   * <p>This is useful for tests to initialize the {@link ContrastSDK} without running the whole
   * {@link #execute()} method
   *
   * @throws IllegalStateException when has already been initialized
   * @throws MojoExecutionException when cannot connect to Contrast
   */
  private synchronized void initialize() throws MojoExecutionException {
    if (contrast != null) {
      throw new IllegalStateException("Already initialized");
    }
    contrast = connectToContrast();
  }
}
