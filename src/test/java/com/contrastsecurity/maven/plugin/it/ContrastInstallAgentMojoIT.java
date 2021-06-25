package com.contrastsecurity.maven.plugin.it;

import com.contrastsecurity.maven.plugin.it.stub.ContrastAPI;
import com.contrastsecurity.maven.plugin.it.stub.ContrastAPIStub;
import java.io.IOException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.jupiter.api.Test;

/**
 * Functional test for the "install" goal.
 *
 * <p>Verifies that the agent downloads a Contrast Java agent from the Contrast API. This test lacks
 * the following verifications for this goal:
 *
 * <ul>
 *   <li>Does not verify that the plugin configures the maven-surefire-plugin's argLine property to
 *       include the agent
 *   <li>Does not verify that the plugin configures the spring-boot-maven-plugin's
 *       "run.jvmArguments" property to include the agent
 *   <li>Does not verify that the agent returned by the Contrast API is preconfigured to report to
 *       the user's Contrast organization
 * </ul>
 *
 * We accept the risk that the aforementioned features are not verified in this test, because we
 * plan to make breaking changes to this goal imminently. This functional test serves mainly as a
 * guiding example for future functional tests.
 */
@ContrastAPIStub
final class ContrastInstallAgentMojoIT {

  @Test
  public void test(final ContrastAPI contrast) throws IOException, VerificationException {
    // GIVEN a spring-boot project that uses the plugin
    final Verifier verifier = Verifiers.springBoot(contrast.connection());

    // WHEN execute the "verify" goal
    verifier.executeGoal("verify");

    // THEN the plugin retrieves a Contrast Java agent from the Contrast API without errors
    verifier.verifyErrorFreeLog();
    verifier.assertFilePresent("target/contrast.jar");
  }
}
