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

@Mojo(
    name = "scan",
    defaultPhase = LifecyclePhase.INTEGRATION_TEST,
    requiresOnline = true,
    threadSafe = true)
public final class ContrastScanMojo extends AbstractContrastMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /** TODO[JAVA-3297] replace this with "project" */
  @Parameter(required = true)
  private String projectID;

  @Parameter(name = "artifactPath")
  private File artifact;

  // TODO[JG] pick default value
  @Parameter(name = "label", defaultValue = "maven")
  private String label;

  private ContrastSDK contrast;

  /** seam for testing */
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
    getLog().info("Submitting " + file + " to Contrast Scan");
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
    // https://teamserver-staging.contsec.com/Contrast/static/ng/index.html#/6fb73b19-37de-44e2-8ac8-0a8de2707048/scans/31e5c292-72fd-425b-a822-c26d13867304/scans/465c272c-f5ae-4f2c-8183-b663d0c5aaa4
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

  private URL createClickableScanURL(final Scan scan) throws MalformedURLException {
    final String url = getURL();
    final String base = url.substring(0, url.lastIndexOf("/"));
    final String scanURL =
        String.join(
            "/",
            base,
            "static",
            "ng",
            "index.html#",
            getOrganizationID(),
            "scans",
            projectID,
            "scans",
            scan.getID());
    return new URL(scanURL);
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
      // TODO[JG] consider a different exception type
      throw new IOException("Contrast returned status code " + rc);
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
