package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDKImpl;
import com.contrastsecurity.maven.plugin.sdkx.ScanSummary;
import com.contrastsecurity.maven.plugin.sdkx.scan.ArtifactScanner;
import com.contrastsecurity.maven.plugin.sdkx.scan.ScanOperation;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
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
  @Parameter private File artifactPath;

  /** A label to distinguish this scan from others in your project */
  @Parameter(defaultValue = "${project.version}")
  private String label;

  /**
   * When true, will wait for and retrieve scan results before completing the goal. Otherwise, will
   * start a scan then complete the goal without waiting for Contrast Scan to complete.
   */
  @Parameter(defaultValue = "" + true)
  private boolean waitForResults;

  @Parameter(defaultValue = "" + true)
  private boolean consoleOutput;

  @Parameter(
      defaultValue =
          "${project.build.directory}/contrast-scan-reports/contrast-scan-results.sarif.json")
  private File outputPath;

  /**
   * Maximum time (in milliseconds) to wait for a Scan to complete. Scans that exceed this threshold
   * fail this goal.
   */
  @Parameter(defaultValue = "" + 20 * 60 * 1000)
  private long timeout;

  private ContrastSDK contrast;

  /** visible for testing */
  String getProjectId() {
    return projectId;
  }

  /** visible for testing */
  void setProjectId(final String projectId) {
    this.projectId = projectId;
  }

  /** visible for testing */
  boolean isConsoleOutput() {
    return consoleOutput;
  }

  /** visible for testing */
  public void setConsoleOutput(final boolean consoleOutput) {
    this.consoleOutput = consoleOutput;
  }

  @Override
  public void execute() throws MojoExecutionException {
    // initialize plugin
    initialize();
    final ContrastScanSDK contrastScan = new ContrastScanSDKImpl(contrast, getURL());

    // check that file exists
    final Path file =
        artifactPath == null ? project.getArtifact().getFile().toPath() : artifactPath.toPath();
    if (!Files.exists(file)) {
      throw new MojoExecutionException(
          file
              + " does not exist. Make sure to bind the scan goal to a phase that will execute after the artifact to scan has been built");
    }

    final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    try {
      final Duration pollStatusInterval = Duration.ofSeconds(30);
      final ArtifactScanner scanner =
          new ArtifactScanner(
              executor, contrastScan, getOrganizationId(), getProjectId(), pollStatusInterval);

      getLog().info("Uploading " + file.getFileName() + " to Contrast Scan");
      final ScanOperation operation = scanner.scanArtifact(file, label);
      getLog().info("Starting scan with label " + label);

      // show link in build log
      final URL clickableScanURL;
      try {
        clickableScanURL = createClickableScanURL(operation.id());
      } catch (final MalformedURLException e) {
        throw new MojoExecutionException(
            "Error building clickable Scan URL. Please contact support@contrastsecurity.com for help",
            e);
      }
      getLog().info("Scan results will be available at " + clickableScanURL.toExternalForm());

      // if should not wait, then stop asking for scan results and return
      if (!waitForResults) {
        operation.disconnect();
        return;
      }

      // else wait for results, output summary to console, output sarif to file system
      getLog().info("Waiting for scan results");
      final Path outputFile = outputPath.toPath();
      final Path reportsDirectory = outputFile.getParent();
      try {
        Files.createDirectories(reportsDirectory);
      } catch (final IOException e) {
        throw new MojoExecutionException("Failed to create Contrast Scan reports directory", e);
      }
      final CompletionStage<Void> save = operation.saveSarifToFile(outputFile);
      final CompletionStage<Void> output =
          operation
              .summary()
              .thenAccept(summary -> writeSummaryToConsole(summary, line -> getLog().info(line)));
      CompletableFuture.allOf(save.toCompletableFuture(), output.toCompletableFuture())
          .get(timeout, TimeUnit.MILLISECONDS);
    } catch (final ExecutionException e) {
      // try to unwrap the extraneous ExecutionException
      final Throwable cause = e.getCause();
      // ExecutionException should always have a cause, but its constructor does not enforce this,
      // so check if the cause is null
      final Throwable inner = cause == null ? e : cause;
      throw new MojoExecutionException("Failed to retrieve Contrast Scan results", inner);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException("Interrupted while retrieving Contrast Scan results", e);
    } catch (final TimeoutException e) {
      final Duration duration = Duration.ofMillis(timeout);
      final String durationString =
          duration.toMinutes() > 0
              ? duration.toMinutes() + " minutes"
              : (duration.toMillis() / 1000) + " seconds";
      throw new MojoExecutionException(
          "Failed to retrieve Contrast Scan results in " + durationString, e);
    } finally {
      executor.shutdown();
    }
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
   * visible for testing
   *
   * @param summary the scan summary to write to console
   * @param consoleLogger describes a console logger where each accepted string is printed to a new
   *     line
   */
  void writeSummaryToConsole(final ScanSummary summary, final Consumer<String> consoleLogger) {
    consoleLogger.accept("Scan completed");
    if (consoleOutput) {
      consoleLogger.accept("New Results\t" + summary.getTotalNewResults());
      consoleLogger.accept("Fixed Results\t" + summary.getTotalFixedResults());
      consoleLogger.accept("Total Results\t" + summary.getTotalResults());
    }
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
