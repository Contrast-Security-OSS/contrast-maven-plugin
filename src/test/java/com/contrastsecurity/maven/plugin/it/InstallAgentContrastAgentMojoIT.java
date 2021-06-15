package com.contrastsecurity.maven.plugin.it;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

public class InstallAgentContrastAgentMojoIT {

  @Test
  public void test() throws IOException, VerificationException {
    final File projectDir =
        ResourceExtractor.simpleExtractResources(
            InstallAgentContrastAgentMojoIT.class, "/it/install");

    final Verifier verifier = new Verifier(projectDir.getAbsolutePath());
    final String testRepository =
        Objects.requireNonNull(
            System.getProperty("contrast.test-repository"),
            "required system property test-repository must be set to a directory containing a maven repository with the contrast-maven-plugin installed");
    verifier.setLocalRepo(testRepository);

    verifier.executeGoal("verify");
    verifier.verifyErrorFreeLog();
  }
}
