package com.contrastsecurity.maven.plugin.it;

import com.contrastsecurity.maven.plugin.it.stub.ContrastAPI;
import com.contrastsecurity.maven.plugin.it.stub.ContrastAPIStub;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

final class InstallAgentContrastAgentMojoIT {

  @ContrastAPIStub
  @Test
  public void test(final ContrastAPI contrast) throws IOException, VerificationException {
    // GIVEN a spring-boot project that uses the plugin
    final File projectDir =
        ResourceExtractor.simpleExtractResources(
            InstallAgentContrastAgentMojoIT.class, "/it/install");

    final Verifier verifier = new Verifier(projectDir.getAbsolutePath());
    final String testRepository =
        Objects.requireNonNull(
            System.getProperty("contrast.test-repository"),
            "required system property contrast.test-repository must be set to a directory containing a maven repository with the contrast-maven-plugin installed");
    verifier.setLocalRepo(testRepository);
    // AND the user provides common agent configuration connection parameters for connecting to
    // Contrast
    final Properties connectionProperties = contrast.connection().toProperties();
    verifier.setSystemProperties(connectionProperties);

    verifier.executeGoal("verify");

    verifier.verifyErrorFreeLog();
  }
}
