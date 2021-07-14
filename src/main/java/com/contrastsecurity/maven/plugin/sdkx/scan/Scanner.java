package com.contrastsecurity.maven.plugin.sdkx.scan;

import com.contrastsecurity.maven.plugin.sdkx.ContrastScanSDK;
import java.nio.file.Path;

public final class Scanner {

  private final ContrastScanSDK contrast;
  private final String organizationId;
  private final String projectId;

  public Scanner(
      final ContrastScanSDK contrast, final String organizationId, final String projectId) {
    this.contrast = contrast;
    this.organizationId = organizationId;
    this.projectId = projectId;
  }

  public ScanOperation scanFile(final Path file, final String label) {
    throw new UnsupportedOperationException("not yet implemented exception");
  }
}
