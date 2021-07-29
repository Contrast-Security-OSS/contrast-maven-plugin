package com.contrastsecurity.maven.plugin.it;

import com.contrastsecurity.maven.plugin.it.stub.ContrastAPI;
import com.contrastsecurity.maven.plugin.it.stub.ContrastAPIStub;
import java.io.IOException;
import java.util.Arrays;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.jupiter.api.Test;

/** Functional test for the "scan" goal */
@ContrastAPIStub
final class ContrastScanMojoIT {

  @Test
  void scan_submits_artifact_for_scanning(final ContrastAPI contrast)
      throws VerificationException, IOException {
    // GIVEN a spring-boot project that uses the plugin
    final Verifier verifier = Verifiers.springBoot(contrast.connection());

    // WHEN execute the "verify" goal
    verifier.setCliOptions(Arrays.asList("--activate-profiles", "scan"));
    verifier.executeGoal("verify");

    // THEN plugin submits the spring-boot application artifact for scanning
    verifier.verifyErrorFreeLog();
    verifier.verifyTextInLog(
        "Uploading spring-test-application-0.0.1-SNAPSHOT.jar to Contrast Scan");
    verifier.verifyTextInLog("Starting scan with label 0.0.1-SNAPSHOT");
    verifier.verifyTextInLog("Scan results will be available at http");
    verifier.verifyTextInLog("Waiting for scan results");
    verifier.verifyTextInLog("Scan completed");
    verifier.assertFilePresent("./target/contrast-scan-reports/contrast-scan-results.sarif.json");
  }
}
