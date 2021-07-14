package com.contrastsecurity.maven.plugin.sdkx;

import com.contrastsecurity.exceptions.UnauthorizedException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface ContrastScanSDK {

  CodeArtifact createCodeArtifact(String organizationId, NewCodeArtifactRequest request)
      throws IOException, UnauthorizedException;

  Scan startScan(String organizationId, StartScanRequest request)
      throws IOException, UnauthorizedException;

  Scan getScanById(String organizationId, String projectId, String scanId)
      throws IOException, UnauthorizedException;
}
