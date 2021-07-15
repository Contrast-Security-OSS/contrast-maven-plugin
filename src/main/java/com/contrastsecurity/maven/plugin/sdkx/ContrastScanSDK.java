package com.contrastsecurity.maven.plugin.sdkx;

import com.contrastsecurity.exceptions.UnauthorizedException;
import java.io.IOException;
import java.io.InputStream;

public interface ContrastScanSDK {

  CodeArtifact createCodeArtifact(String organizationId, NewCodeArtifactRequest request)
      throws IOException, UnauthorizedException;

  Scan startScan(String organizationId, StartScanRequest request)
      throws IOException, UnauthorizedException;

  Scan getScanById(String organizationId, String projectId, String scanId)
      throws IOException, UnauthorizedException;

  InputStream getSarif(String organizationId, String projectId, String scanId)
      throws IOException, UnauthorizedException;

  ScanSummary getScanSummary(String organizationId, String projectId, String scanId)
      throws IOException, UnauthorizedException;
}
