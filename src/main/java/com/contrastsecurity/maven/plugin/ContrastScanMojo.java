package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.HttpMethod;
import com.contrastsecurity.http.MediaType;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.utils.ContrastSDKUtils;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
            getOrganizationId(),
            "scans",
            projectID,
            "scans",
            scan.getID());
    return new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
  }

  @SuppressWarnings("Since15")
  CodeArtifact uploadCodeArtifactJDK11(File file) throws IOException {
    final HttpClient http = HttpClient.newHttpClient();
    final String url =
        String.join(
            "/",
            getURL(),
            "api",
            "sast",
            "organizations",
            getOrganizationId(),
            "projects",
            projectID,
            "code-artifacts");
    final HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("API-Key", getApiKey())
            .header(
                "Authorization",
                ContrastSDKUtils.makeAuthorizationToken(getUserName(), getServiceKey()))
            .POST(BodyPublishers.ofFile(file.toPath()))
            .build();
    final HttpResponse<InputStream> response;
    try {
      response = http.send(request, BodyHandlers.ofInputStream());
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while receiving data", e);
    }
    final Gson gson = new Gson();
    try (InputStreamReader reader = new InputStreamReader(response.body())) {
      return gson.fromJson(reader, CodeArtifact.class);
    }
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
            getOrganizationId(),
            "projects",
            projectID,
            "code-artifacts");
    final String boundary = "ContrastFormBoundary" + ThreadLocalRandom.current().nextLong();
    final String header =
        "--"
            + boundary
            + LINE_FEED
            + "Content-Disposition: form-data; name=\"filename\"; filename=\""
            + file.getName()
            + '"'
            + LINE_FEED
            + "Content-Type: application/java-archive"
            + LINE_FEED
            + "Content-Transfer-Encoding: binary"
            + LINE_FEED
            + LINE_FEED;
    final String footer = LINE_FEED + "--" + boundary + "--" + LINE_FEED;
    final long contentLength = header.length() + file.length() + footer.length();

    final HttpURLConnection connection = contrast.makeConnection(url, "POST");
    connection.setConnectTimeout(10000);
    connection.setReadTimeout(10000);
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    connection.setFixedLengthStreamingMode(contentLength);
    try (OutputStream os = connection.getOutputStream();
        PrintWriter writer =
            new PrintWriter(new OutputStreamWriter(os, StandardCharsets.US_ASCII), true)) {
      writer.append(header).flush();
      Files.copy(file.toPath(), os);
      os.flush();
      writer.append(footer).flush();
    }
    final int rc = connection.getResponseCode();
    if (rc != 201) {
      throw new ContrastException(rc, "Failed to upload code artifact to Contrast Scan");
    }
    // TODO[JG] JAVA-3298 this GSON usage will be encapsulated in the Contrast SDK
    final Gson gson = new Gson();
    try (Reader reader = new InputStreamReader(connection.getInputStream())) {
      return gson.fromJson(reader, CodeArtifact.class);
    }
  }

  private Scan startScan(final StartScanRequest request) throws UnauthorizedException, IOException {
    // TODO[JG] JAVA-3298 unlike requests made with ContrastSDK.makeConnection, requests made with
    // ContrastSDK.makeRequest must have their path prepended with "/". This complexity will migrate
    // to the SDK
    final String path =
        String.join(
            "/",
            "",
            "api",
            "sast",
            "organizations",
            getOrganizationId(),
            "projects",
            projectID,
            "scans");
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
