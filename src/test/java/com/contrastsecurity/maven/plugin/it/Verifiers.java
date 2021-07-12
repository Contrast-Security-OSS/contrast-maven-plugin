package com.contrastsecurity.maven.plugin.it;

import com.contrastsecurity.maven.plugin.it.stub.ConnectionParameters;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Reusable static factory methods for creating new Maven Verifier instances from well-known sample
 * projects
 */
final class Verifiers {

  /** @return new {@link Verifier} for the /it/spring-boot sample Maven project */
  static Verifier springBoot(final ConnectionParameters connection)
      throws IOException, VerificationException {
    final File projectDir =
        ResourceExtractor.simpleExtractResources(
            ContrastInstallAgentMojoIT.class, "/it/spring-boot");

    final Verifier verifier = new Verifier(projectDir.getAbsolutePath());
    final String testRepository =
        Objects.requireNonNull(
            System.getProperty("contrast.test-repository"),
            "required system property contrast.test-repository must be set to a directory containing a maven repository with the contrast-maven-plugin installed");
    verifier.setLocalRepo(testRepository);
    // AND the user provides common agent configuration connection parameters for connecting to
    // Contrast
    final Properties connectionProperties = connection.toProperties();
    verifier.setSystemProperties(connectionProperties);
    return verifier;
  }

  /** static members only */
  private Verifiers() {}
}
