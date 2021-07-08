package com.contrastsecurity.maven.plugin.it;

import com.contrastsecurity.maven.plugin.it.stub.ContrastAPI;
import com.contrastsecurity.maven.plugin.it.stub.ContrastAPIStub;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.jupiter.api.Test;

@ContrastAPIStub
final class ContrastScanMojoIT {

  @Test
  void scan_submits_artifact_for_scanning(final ContrastAPI contrast)
      throws VerificationException, IOException {
    // GIVEN a spring-boot project that uses the plugin
    final Verifier verifier = Verifiers.springBoot(contrast.connection());

    // WHEN execute the "verify" goal
    verifier.setCliOptions(Arrays.asList("--activate-profiles", "scan"));
    verifier.executeGoal(
        "verify",
        Collections.singletonMap(
            "MAVEN_DEBUG_OPTS",
            "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5000"));

    // THEN plugin submits the spring-boot application artifact for scanning
    verifier.verifyErrorFreeLog();
  }
}
