package com.contrastsecurity.maven.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.maven.plugin.it.stub.ConnectionParameters;
import com.contrastsecurity.maven.plugin.it.stub.ContrastAPI;
import com.contrastsecurity.maven.plugin.it.stub.ContrastAPIStub;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@ContrastAPIStub
final class ContrastScanMojoTest {

  @Test
  void upload_file_test(final ContrastAPI contrast, @TempDir final Path temp)
      throws IOException, MojoExecutionException {
    // GIVEN some temporary jar file
    final File jar = Files.createTempFile(temp, "my-application", ".jar").toFile();
    try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
      jos.putNextEntry(new ZipEntry("Main.class"));
    }
    // AND scan mojo is configured to communicate with fake Contrast API
    final ContrastScanMojo scan = new ContrastScanMojo();
    final ConnectionParameters connection = contrast.connection();
    scan.setURL(connection.url());
    scan.setApiKey(connection.apiKey());
    scan.setServiceKey(connection.serviceKey());
    scan.setUsername(connection.username());
    scan.setOrganizationID(connection.organizationID());
    scan.setProjectID("31e5c292-72fd-425b-a822-c26d13867304");
    scan.setSettings(new Settings());
    scan.initialize();

    // WHEN upload file as a new code artifact
    final File file =
        new File(
            "./target/test-classes/it/spring-boot/target/spring-test-application-0.0.1-SNAPSHOT.jar");
    final CodeArtifact artifact = scan.uploadCodeArtifact(file);

    // THEN returns new artifact
    assertThat(artifact.getID()).isNotNull();
  }
}
