package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.HttpMethod;
import com.contrastsecurity.http.MediaType;
import com.contrastsecurity.sdk.ContrastSDK;
import com.google.gson.Gson;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
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
  private String projectID;

  /**
   * File path to the Java artifact to upload for scanning. By default, uses the path to this
   * module's Maven artifact produced in the {@code package} phase.
   */
  @Parameter(name = "artifactPath")
  private File artifact;

  /** A label to distinguish this scan from others in your project */
  @Parameter(name = "label", defaultValue = "${project.version}")
  private String label;

  private ContrastSDK contrast;

  String getProjectID() {
    return projectID;
  }

  void setProjectID(final String projectID) {
    this.projectID = projectID;
  }

  @Override
  public void execute() throws MojoExecutionException {
    initialize();
    final File file = artifact == null ? project.getArtifact().getFile() : artifact;
    if (!file.exists()) {
      throw new MojoExecutionException(
          file
              + " does not exist. Make sure to bind the scan goal to a phase that will execute after the artifact to scan has been built");
    }
    getLog().info("Submitting " + file.getName() + " to Contrast Scan");
    final CodeArtifact artifact;
    try {
      artifact = uploadCodeArtifact(file);
    } catch (final IOException e) {
      throw new MojoExecutionException("Failed to upload artifact to Contrast Scan", e);
    }
    final StartScanRequest request = new StartScanRequest(artifact.getID(), label);
    final Scan scan;
    try {
      scan = startScan(request);
    } catch (final UnauthorizedException e) {
      throw new MojoExecutionException("Failed to authenticate to Contrast", e);
    } catch (final IOException e) {
      throw new MojoExecutionException(
          "Failed to start scan for code artifact " + artifact.getID(), e);
    }
    final URL clickableScanURL;
    try {
      clickableScanURL = createClickableScanURL(scan);
    } catch (final MalformedURLException e) {
      throw new MojoExecutionException(
          "Error building clickable Scan URL. Please file an issue to support@contrastsecurity.com",
          e);
    }
    getLog().info("Scanning " + file.getName() + ", results " + clickableScanURL.toExternalForm());
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
  synchronized void initialize() throws MojoExecutionException {
    if (contrast != null) {
      throw new IllegalStateException("Already initialized");
    }
    contrast = connectToContrast();
  }

  /**
   * visible for testing
   *
   * @return Contrast browser application URL for users to click-through and see their scan results
   */
  URL createClickableScanURL(final Scan scan) throws MalformedURLException {
    final URL url = new URL(getURL());
    final String path =
        String.join(
            "/",
            "",
            "Contrast",
            "static",
            "ng",
            "index.html#",
            getOrganizationID(),
            "scans",
            projectID,
            "scans",
            scan.getID());
    return new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
  }

  /**
   * visible for testing
   *
   * @param file the file to upload
   * @return new {@link CodeArtifact} from Contrast
   * @throws IOException when an IO error occurs while uploading the file
   */
  CodeArtifact uploadCodeArtifact(File file) throws IOException {
    final String url =
        String.join(
            "/",
            getURL(),
            "api",
            "sast",
            "organizations",
            getOrganizationID(),
            "projects",
            projectID,
            "code-artifacts");
    final String boundary = "---ContrastFormBoundary" + ThreadLocalRandom.current().nextLong();
    final HttpURLConnection connection = contrast.makeConnection(url, "POST");
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    try (OutputStream os = connection.getOutputStream();
        PrintWriter writer =
            new PrintWriter(new OutputStreamWriter(os, StandardCharsets.US_ASCII), false)) {
      writer
          .append("---")
          .append(boundary)
          .append(LINE_FEED)
          .append("Content-Disposition: form-data; name=\"filename\"; filename=\"")
          .append(file.getName())
          .append("\"")
          .append(LINE_FEED)
          .append("Content-Type: ")
          .append(HttpURLConnection.guessContentTypeFromName(file.getName()))
          .append(LINE_FEED)
          .append(LINE_FEED)
          .flush();
      try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {
        final byte[] buffer = new byte[4096];
        int read;
        while ((read = is.read(buffer)) != -1) {
          os.write(buffer, 0, read);
        }
      }
      writer
          .append("---")
          .append(boundary)
          .append("--")
          .append(LINE_FEED)
          .append(LINE_FEED)
          .append("--")
          .append(boundary)
          .append("--")
          .flush();
    }
    final int rc = connection.getResponseCode();
    if (rc != 201) {
      throw new ContrastException(rc, "Failed to upload code artifact to Contrast Scan");
    }
    // TODO[JG] JAVA-3298 this GSON usage will be encapsulated in the Contrast SDK
    final Gson gson = new Gson();
    final CodeArtifact artifact;
    try (Reader reader = new InputStreamReader(connection.getInputStream())) {
      artifact = gson.fromJson(reader, CodeArtifact.class);
    }
    connection.disconnect();
    return artifact;
  }

  private Scan startScan(final StartScanRequest request) throws UnauthorizedException, IOException {
    // TODO[JG] JAVA-3298 unlike requests made with ContrastSDK.makeConnection, requests made with
    // ContrastSDK.makeRequest must have their path prepended with "/". This complexity will migrate
    // to the SDK
    final String path =
        String.join(
            "/", "", "sast", "organizations", getOrganizationID(), "projects", projectID, "scans");
    // TODO[JG] JAVA-3298 this GSON usage will be encapsulated in the Contrast SDK
    final Gson gson = new Gson();
    final String json = gson.toJson(request);
    getLog().debug("Starting Scan");
    getLog().debug(json);
    try (Reader reader =
        new InputStreamReader(
            contrast.makeRequestWithBody(HttpMethod.POST, path, json, MediaType.JSON))) {
      return gson.fromJson(reader, Scan.class);
    }
  }

  private static final String LINE_FEED = "\r\n";
}
