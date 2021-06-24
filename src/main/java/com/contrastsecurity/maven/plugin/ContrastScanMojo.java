package com.contrastsecurity.maven.plugin;

import java.util.List;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(
    name = "scan",
    defaultPhase = LifecyclePhase.INTEGRATION_TEST,
    requiresOnline = true,
    threadSafe = true)
public final class ContrastScanMojo extends AbstractContrastMojo {

  @Parameter(property = "scan.packages", required = true)
  private List<String> packages;

  @Override
  public void execute() {
    for (final String aPackage : packages) {
      getLog().info("include package " + aPackage);
    }
  }
}
