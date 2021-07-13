package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.models.AgentType;
import com.contrastsecurity.models.Applications;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Includes the Contrast Java agent in integration testing to provide Contrast Assess runtime
 * security analysis
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.VALIDATE, requiresOnline = true)
public final class ContrastInstallAgentMojo extends AbstractAssessMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /**
   * When {@code true}, will not alter the Maven {@code argLine} property
   *
   * @since 2.0
   */
  @Parameter(property = "skipArgLine")
  boolean skipArgLine;

  /**
   * "true" if this is a standalone application (e.g. uses an embedded web server vs war packaging)
   *
   * @since 2.2
   */
  @Parameter(property = "standalone")
  boolean standalone;

  /**
   * Override the reported server environment. {@see
   * https://docs.contrastsecurity.com/en/server-configuration.html}
   */
  @Parameter(property = "environment")
  private String environment;

  /**
   * Override the reported server path. Default is the present working directory of the JVM process
   * Contrast is attached to.
   *
   * <p>In a multi-module build, the default value may lead Contrast to report a unique server per
   * module. Multi-module Maven builds can appear as different servers in the Contrast UI. If you
   * would like to discourage this behavior and would rather see all modules appear under the same
   * server in Contrast, use this property to set a common server path across modules.
   *
   * @since 2.1
   */
  @Parameter(property = "serverPath")
  String serverPath;

  /**
   * Path to an existing Contrast Java agent JAR. Specifying this configures the plugin to omit the
   * "retrieve Contrast JAR" step.
   */
  @Parameter(property = "jarPath")
  private String jarPath;

  /**
   * Define a set of key=value pairs (which conforms to RFC 2253) for specifying user-defined
   * metadata associated with the application. The set must be formatted as a comma-delimited list
   * of {@code key=value} pairs.
   *
   * <p>Example - "business-unit=accounting, office=Baltimore"
   */
  @Parameter(property = "applicationSessionMetadata")
  private String applicationSessionMetadata;

  /** Tags to apply to the Contrast application. Must be formatted as a comma-delimited list */
  @Parameter(property = "applicationTags")
  private String applicationTags;

  /**
   * The {@code appVersion} metadata associated with Contrast analysis findings. Allows users to
   * compare vulnerabilities between applications versions, CI builds, etc. Contrast generates the
   * appVersion in the following order:
   *
   * <ol>
   *   <li>The {@code appVersion} as configured in the plugin properties
   *   <li>If your build is running in TravisCI, Contrast will use {@code
   *       appName-$TRAVIS_BUILD_NUMBER}
   *   <li>If your build is running in CircleCI, Contrast will use {@code appName-$CIRCLE_BUILD_NUM}
   *   <li>If none of the above apply, Contrast will use a timestamp {@code appName-yyyyMMddHHmmss}
   *       format.
   * </ol>
   */
  @Parameter(property = "appVersion")
  String appVersion;

  /** visible for testing */
  String contrastAgentLocation;

  String applicationName;

  static Map<String, String> environmentToSessionMetadata = new TreeMap<>();

  static {
    // Jenkins git plugin environment variables
    environmentToSessionMetadata.put("GIT_BRANCH", "branchName");
    environmentToSessionMetadata.put("GIT_COMMITTER_NAME", "committer");
    environmentToSessionMetadata.put("GIT_COMMIT", "commitHash");
    environmentToSessionMetadata.put("GIT_URL", "repository");
    environmentToSessionMetadata.put("GIT_URL_1", "repository");

    // CI build number environment variables
    environmentToSessionMetadata.put("BUILD_NUMBER", "buildNumber");
    environmentToSessionMetadata.put("TRAVIS_BUILD_NUMBER", "buildNumber");
    environmentToSessionMetadata.put("CIRCLE_BUILD_NUM", "buildNumber");
  }

  public void execute() throws MojoExecutionException {
    verifyAppIdOrNameNotBlank();
    getLog().info("Attempting to connect to Contrast and install the Java agent.");

    ContrastSDK contrast = connectToContrast();

    File agentFile = installJavaAgent(contrast);

    getLog().info("Agent downloaded.");

    if (StringUtils.isNotBlank(getAppId())) {
      applicationName = getAppName(contrast, getAppId());

      if (StringUtils.isNotBlank(getAppName())) {
        getLog().info("Using 'appId' property; 'appName' property is ignored.");
      }

    } else {
      applicationName = getAppName();
    }
    project
        .getProperties()
        .setProperty(
            "argLine",
            buildArgLine(project.getProperties().getProperty("argLine"), applicationName));

    for (Plugin plugin : (List<Plugin>) project.getBuildPlugins()) {
      if ("org.springframework.boot".equals(plugin.getGroupId())
          && "spring-boot-maven-plugin".equals(plugin.getArtifactId())) {
        getLog().debug("Found the spring-boot-maven-plugin, with configuration:");
        String configuration = plugin.getConfiguration().toString();
        getLog().debug(configuration);
        if (configuration.contains("${argLine}")) {
          getLog().info("Skipping set of -Drun.jvmArguments as it references ${argLine}");
        } else {
          String jvmArguments =
              buildArgLine(
                  project.getProperties().getProperty("run.jvmArguments"), applicationName);
          getLog().info(String.format("Setting -Drun.jvmArguments=%s", jvmArguments));
          project.getProperties().setProperty("run.jvmArguments", jvmArguments);
        }

        break;
      }
    }
  }

  private String getAppName(ContrastSDK contrastSDK, String applicationId)
      throws MojoExecutionException {
    Applications applications;
    try {
      final String organizationID = getOrganizationId();
      applications = contrastSDK.getApplication(organizationID, applicationId);
    } catch (Exception e) {
      String logMessage;
      if (e.getMessage().contains("403")) {
        logMessage =
            "\n\n Unable to find the application on Contrast with the id [" + applicationId + "]\n";
      } else {
        logMessage =
            "\n\n Unable to retrieve the application list from Contrast. Please check Contrast connection configuration\n";
      }
      throw new MojoExecutionException(logMessage, e);
    }
    if (applications.getApplication() == null) {
      throw new MojoExecutionException(
          "\n\nApplication with id '"
              + applicationId
              + "' not found. Make sure this application appears in Contrast under the 'Applications' tab.\n");
    }
    return applications.getApplication().getName();
  }

  String computeAppVersion(Date currentDate) {
    if (computedAppVersion != null) {
      return computedAppVersion;
    }

    if (appVersion != null) {
      getLog().info("Using user-specified app version [" + appVersion + "]");
      computedAppVersion = appVersion;
      return computedAppVersion;
    }

    String travisBuildNumber = System.getenv("TRAVIS_BUILD_NUMBER");
    String circleBuildNum = System.getenv("CIRCLE_BUILD_NUM");

    final String appVersionQualifier;
    if (travisBuildNumber != null) {
      getLog()
          .info(
              "Build is running in TravisCI. We'll use TRAVIS_BUILD_NUMBER ["
                  + travisBuildNumber
                  + "]");
      appVersionQualifier = travisBuildNumber;
    } else if (circleBuildNum != null) {
      getLog()
          .info(
              "Build is running in CircleCI. We'll use CIRCLE_BUILD_NUM [" + circleBuildNum + "]");
      appVersionQualifier = circleBuildNum;
    } else {
      getLog().info("No CI build number detected, we'll use current timestamp.");
      appVersionQualifier = new SimpleDateFormat("yyyyMMddHHmmss").format(currentDate);
    }
    if (StringUtils.isNotBlank(getAppId())) {
      computedAppVersion = applicationName + "-" + appVersionQualifier;
    } else {
      computedAppVersion = getAppName() + "-" + appVersionQualifier;
    }

    return computedAppVersion;
  }

  String computeSessionMetadata() {
    List<String> metadata = new ArrayList<>();

    for (Map.Entry<String, String> entry : environmentToSessionMetadata.entrySet()) {
      String environmentValue = System.getenv(entry.getKey());

      if (environmentValue != null) {
        metadata.add(String.format("%s=%s", entry.getValue(), environmentValue));
      }
    }

    return StringUtils.join(metadata, ",");
  }

  String buildArgLine(String currentArgLine) {
    return buildArgLine(currentArgLine, getAppName());
  }

  String buildArgLine(String currentArgLine, String applicationName) {

    if (currentArgLine == null) {
      getLog().info("Current argLine is null");
      currentArgLine = "";
    } else {
      getLog().info("Current argLine is [" + currentArgLine + "]");
    }

    if (skipArgLine) {
      getLog().info("skipArgLine is set to false.");
      getLog()
          .info(
              "You will need to configure the Maven argLine property manually for the Contrast agent to work.");
      return currentArgLine;
    }

    getLog().info("Configuring argLine property.");

    computedAppVersion = computeAppVersion(new Date());

    StringBuilder argLineBuilder = new StringBuilder();
    argLineBuilder.append(currentArgLine);
    argLineBuilder.append(" -javaagent:").append(contrastAgentLocation);
    argLineBuilder.append(" -Dcontrast.server=").append(getServerName());
    if (environment != null) {
      argLineBuilder.append(" -Dcontrast.env=").append(environment);
    } else {
      argLineBuilder.append(" -Dcontrast.env=qa");
    }
    argLineBuilder.append(" -Dcontrast.override.appversion=").append(computedAppVersion);
    argLineBuilder.append(" -Dcontrast.reporting.period=").append("200");

    String sessionMetadata = computeSessionMetadata();
    if (!sessionMetadata.isEmpty()) {
      argLineBuilder
          .append(" -Dcontrast.application.session_metadata='")
          .append(sessionMetadata)
          .append("'");
    }

    if (standalone) {
      argLineBuilder.append(" -Dcontrast.standalone.appname=").append(applicationName);
    } else {
      argLineBuilder.append(" -Dcontrast.override.appname=").append(applicationName);
    }

    if (!StringUtils.isEmpty(serverPath)) {
      argLineBuilder.append(" -Dcontrast.path=").append(serverPath);
    }

    if (!StringUtils.isEmpty(applicationSessionMetadata)) {
      argLineBuilder
          .append(" -Dcontrast.application.session_metadata='")
          .append(applicationSessionMetadata)
          .append("'");
    }

    if (!StringUtils.isEmpty(applicationTags)) {
      argLineBuilder.append(" -Dcontrast.application.tags=").append(applicationTags);
    }

    String newArgLine = argLineBuilder.toString();

    getLog().info("Updated argLine is " + newArgLine);
    return newArgLine.trim();
  }

  File installJavaAgent(ContrastSDK connection) throws MojoExecutionException {
    byte[] javaAgent;
    File agentFile;

    if (StringUtils.isEmpty(jarPath)) {
      getLog().info("No jar path was configured. Downloading the latest contrast.jar...");

      final String organizationID = getOrganizationId();
      try {
        javaAgent = connection.getAgent(AgentType.JAVA, organizationID);
      } catch (IOException e) {
        throw new MojoExecutionException(
            "\n\nCouldn't download the Java agent from Contrast. Please check that all your credentials are correct. If everything is correct, please contact Contrast Support. The error is:",
            e);
      } catch (UnauthorizedException e) {
        throw new MojoExecutionException(
            "\n\nWe contacted Contrast successfully but couldn't authorize with the credentials you provided. The error is:",
            e);
      }

      // Save the jar to the 'target' directory
      agentFile = new File(project.getBuild().getDirectory() + File.separator + AGENT_NAME);

      try {
        FileUtils.writeByteArrayToFile(agentFile, javaAgent);
      } catch (IOException e) {
        throw new MojoExecutionException("Unable to save the latest java agent.", e);
      }

      getLog().info("Saved the latest java agent to " + agentFile.getAbsolutePath());
      contrastAgentLocation = agentFile.getAbsolutePath();

    } else {
      getLog().info("Using configured jar path " + jarPath);

      agentFile = new File(jarPath);

      if (!agentFile.exists()) {
        throw new MojoExecutionException("Unable to load the local Java agent from " + jarPath);
      }

      getLog().info("Loaded the latest java agent from " + jarPath);
      contrastAgentLocation = jarPath;
    }

    return agentFile;
  }

  private static final String AGENT_NAME = "contrast.jar";
}