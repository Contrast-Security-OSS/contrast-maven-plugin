package com.contrastsecurity.maven.plugin.it;

import com.contrastsecurity.maven.plugin.it.stub.ContrastAPI;
import com.contrastsecurity.maven.plugin.it.stub.ContrastAPIStub;
import java.io.IOException;
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
    verifier.executeGoal("verify");

    // THEN plugin submits the spring-boot application artifact for scanning
    verifier.verifyErrorFreeLog();
  }
}
