package com.contrastsecurity.maven.plugin.sdkx;

import com.contrastsecurity.exceptions.UnauthorizedException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extensions to the Contrast SDK for Contrast Scan
 *
 * <p>TODO[JAVA-3298] migrate this to the Contrast SDK and implement testing according to that
 * project's testing strategy
 */
public interface ContrastScanSDK {

  /**
   * Creates a new Scan project.
   *
   * @param organizationId unique ID for the user's organization
   * @param request new project request
   * @return the new {@link Project}
   * @throws IOException when an IO error occurs sending the request to the Contrast Scan API
   * @throws UnauthorizedException when Contrast rejects this request as unauthorized
   */
  Project createProject(final String organizationId, final CreateProjectRequest request)
      throws IOException, UnauthorizedException;

  /**
   * Scan project lookup.
   *
   * @param organizationId unique ID for the user's organization
   * @param projectName unique project name to find
   * @return the {@link Project}, or {@code null} if no such project is found
   * @throws IOException when an IO error occurs sending the query to the Contrast Scan API
   * @throws UnauthorizedException when Contrast rejects this request as unauthorized
   */
  Project findProjectByName(final String organizationId, final String projectName)
      throws IOException, UnauthorizedException;

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
  CodeArtifact createCodeArtifact(String organizationId, NewCodeArtifactRequest request)
      throws IOException, UnauthorizedException;

  /**
   * Starts a new scan
   *
   * @param organizationId unique ID for the user's organization
   * @param request parameters for requesting a new scan
   * @return new scan operation
   * @throws IOException when an IO error occurs while uploading the file
   * @throws UnauthorizedException when Contrast rejects this request as unauthorized
   */
  Scan startScan(String organizationId, StartScanRequest request)
      throws IOException, UnauthorizedException;

  /**
   * Retrieve a scan by its ID
   *
   * @param organizationId unique ID for the user's organization
   * @param projectId unique ID of the Scan project to which the scan belongs
   * @param scanId unique ID of the scan
   * @return {@code Scan} record
   * @throws IOException when an IO error occurs while uploading the file
   * @throws UnauthorizedException when Contrast rejects this request as unauthorized
   */
  Scan getScanById(String organizationId, String projectId, String scanId)
      throws IOException, UnauthorizedException;

  /**
   * Retrieves scan results in <a href="https://sarifweb.azurewebsites.net">SARIF</a>
   *
   * @param organizationId unique ID for the user's organization
   * @param projectId unique ID of the Scan project to which the scan belongs
   * @param scanId unique ID of the scan
   * @return {@code InputStream} for reading the SARIF response from Contrast
   * @throws IOException when an IO error occurs while uploading the file
   * @throws UnauthorizedException when Contrast rejects this request as unauthorized
   */
  InputStream getSarif(String organizationId, String projectId, String scanId)
      throws IOException, UnauthorizedException;

  /**
   * Retrieves summary of scan results
   *
   * @param organizationId unique ID for the user's organization
   * @param projectId unique ID of the Scan project to which the scan belongs
   * @param scanId unique ID of the scan
   * @return {@code ScanSummary} describing a scan
   * @throws IOException when an IO error occurs while uploading the file
   * @throws UnauthorizedException when Contrast rejects this request as unauthorized
   */
  ScanSummary getScanSummary(String organizationId, String projectId, String scanId)
      throws IOException, UnauthorizedException;
}
