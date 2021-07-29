package com.contrastsecurity.maven.plugin.sdkx;

/*-
 * #%L
 * Contrast Maven Plugin
 * %%
 * Copyright (C) 2021 Contrast Security, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
