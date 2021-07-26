package com.contrastsecurity.maven.plugin.sdkx;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.HttpMethod;
import com.contrastsecurity.http.MediaType;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.utils.ContrastSDKUtils;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Extensions to the Contrast SDK for Contrast Scan
 *
 * <p>TODO[JAVA-3298] migrate this to the Contrast SDK and implement testing according to that
 * project's testing strategy
 */
public final class ContrastScanSDKImpl implements ContrastScanSDK {

  private final ContrastSDK contrast;
  private final String restURL;
  private final Gson gson = GsonFactory.create();

  /**
   * Creates a new {@code ContrastScanSDK} that delegates to the given {@link ContrastSDK}
   *
   * @param contrast the {@link ContrastSDK} this type extends with Scan support
   * @param restURL URL for the Contrast REST API
   */
  public ContrastScanSDKImpl(final ContrastSDK contrast, final String restURL) {
    this.contrast = contrast;
    this.restURL = ContrastSDKUtils.ensureApi(restURL);
  }

  /**
   * Transfers a file from the file system to Contrast Scan to create a new code artifact for
   * analysis.
   *
   * @param organizationId unique ID for the user's organization
   * @param request parameters for creating the new code artifact
   * @return new {@link CodeArtifact} from Contrast
   * @throws IOException when an IO error occurs while uploading the file
   * @throws UnauthorizedException when Contrast rejects this request as unauthorized
   */
  @Override
  public CodeArtifact createCodeArtifact(
      final String organizationId, final NewCodeArtifactRequest request)
      throws IOException, UnauthorizedException {
    final Path file = request.getFile();
    final String url =
        String.join(
            "/",
            restURL,
            "sast",
            "organizations",
            organizationId,
            "projects",
            request.getProjectId(),
            "code-artifacts");
    final String boundary = "ContrastFormBoundary" + ThreadLocalRandom.current().nextLong();
    final String header =
        "--"
            + boundary
            + CRLF
            + "Content-Disposition: form-data; name=\"filename\"; filename=\""
            + file.getFileName().toString()
            + '"'
            + CRLF
            + "Content-Type: "
            + determineMime(file)
            + CRLF
            + "Content-Transfer-Encoding: binary"
            + CRLF
            + CRLF;
    final String footer = CRLF + "--" + boundary + "--" + CRLF;
    final long contentLength = header.length() + Files.size(file) + footer.length();

    final HttpURLConnection connection = contrast.makeConnection(url, "POST");
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    connection.setFixedLengthStreamingMode(contentLength);
    try (OutputStream os = connection.getOutputStream();
        PrintWriter writer =
            new PrintWriter(new OutputStreamWriter(os, StandardCharsets.US_ASCII), true)) {
      writer.append(header).flush();
      Files.copy(file, os);
      os.flush();
      writer.append(footer).flush();
    }
    final int rc = connection.getResponseCode();
    // for consistency with other SDK methods, throw UnauthorizedException when request's
    // authentication is rejected. Unlike other SDK requests, do not conflate all 400 errors with an
    // authentication problem
    if (rc == HttpURLConnection.HTTP_FORBIDDEN || rc == HttpURLConnection.HTTP_UNAUTHORIZED) {
      throw new UnauthorizedException(rc);
    }
    if (rc != 201) {
      throw new ContrastAPIException(rc, "Failed to upload code artifact to Contrast Scan");
    }
    try (Reader reader = new InputStreamReader(connection.getInputStream())) {
      return gson.fromJson(reader, CodeArtifact.class);
    }
  }

  /**
   * Guesses the mime type from the file extension. Returns the arbitrary "application/octet-stream"
   * if no mime type can be inferred from the file extension.
   *
   * <p>Visible for testing
   */
  static String determineMime(final Path file) throws IOException {
    // trust the content type Java can determine
    final String contentType = Files.probeContentType(file);
    if (contentType != null) {
      return contentType;
    }
    // special checks for Java archive types, because not all of these types are identified by
    // Files.probeContentType(file) and we want to make sure we handle Java extensions correctly
    // since users of this code will most likely be uploading Java artifacts
    final String name = file.getFileName().toString();
    if (name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".ear")) {
      return "application/java-archive";
    }
    return "application/octet-stream";
  }

  /**
   * Starts a new scan
   *
   * @param organizationId unique ID for the user's organization
   * @param request parameters for requesting a new scan
   * @return new scan operation
   * @throws IOException when an IO error occurs while uploading the file
   * @throws UnauthorizedException when Contrast rejects this request as unauthorized
   */
  @Override
  public Scan startScan(final String organizationId, final StartScanRequest request)
      throws IOException, UnauthorizedException {
    // requests made with ContrastSDK.makeRequest must have their path prepended with "/"
    final String path =
        String.join(
            "/",
            "",
            "sast",
            "organizations",
            organizationId,
            "projects",
            request.getProjectId(),
            "scans");
    final String json = gson.toJson(request);
    try (Reader reader =
        new InputStreamReader(
            contrast.makeRequestWithBody(HttpMethod.POST, path, json, MediaType.JSON))) {
      return gson.fromJson(reader, Scan.class);
    }
  }

  @Override
  public Scan getScanById(final String organizationId, final String projectId, final String scanId)
      throws UnauthorizedException, IOException {
    // requests made with ContrastSDK.makeRequest must have their path prepended with "/"
    final String path =
        String.join(
            "/",
            "",
            "sast",
            "organizations",
            organizationId,
            "projects",
            projectId,
            "scans",
            scanId);
    try (Reader reader = new InputStreamReader(contrast.makeRequest(HttpMethod.GET, path))) {
      return gson.fromJson(reader, Scan.class);
    }
  }

  @Override
  public InputStream getSarif(
      final String organizationId, final String projectId, final String scanId)
      throws IOException, UnauthorizedException {
    // requests made with ContrastSDK.makeRequest must have their path prepended with "/"
    final String path =
        String.join(
            "/",
            "",
            "sast",
            "organizations",
            organizationId,
            "projects",
            projectId,
            "scans",
            scanId,
            "raw-output");
    return contrast.makeRequest(HttpMethod.GET, path);
  }

  @Override
  public ScanSummary getScanSummary(
      final String organizationId, final String projectId, final String scanId)
      throws IOException, UnauthorizedException {
    // requests made with ContrastSDK.makeRequest must have their path prepended with "/"
    final String path =
        String.join(
            "/",
            "",
            "sast",
            "organizations",
            organizationId,
            "projects",
            projectId,
            "scans",
            scanId,
            "summary");
    try (Reader reader = new InputStreamReader(contrast.makeRequest(HttpMethod.GET, path))) {
      return gson.fromJson(reader, ScanSummary.class);
    }
  }

  private static final String CRLF = "\r\n";
}
