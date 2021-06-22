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

@Mojo(name = "install", defaultPhase = LifecyclePhase.VALIDATE, requiresOnline = true)
public class ContrastInstallAgentMojo extends AbstractAssessMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Parameter(property = "skipArgLine")
  boolean skipArgLine;

  @Parameter(property = "standalone")
  boolean standalone;

  @Parameter(property = "profile")
  private String profile;

  @Parameter(property = "environment")
  private String environment;

  @Parameter(property = "serverPath")
  String serverPath;

  @Parameter(property = "jarPath")
  private String jarPath;

  String contrastAgentLocation;

  @Parameter(property = "applicationSessionMetadata")
  private String applicationSessionMetadata;

  @Parameter(property = "applicationTags")
  private String applicationTags;

  @Parameter(property = "appVersion")
  String appVersion;

  String applicationName;

  static Map<String, String> environmentToSessionMetadata = new TreeMap<String, String>();

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

    ContrastSDK contrast = connectToTeamServer();

    File agentFile = installJavaAgent(contrast);

    getLog().info("Agent downloaded.");

    if (StringUtils.isNotBlank(appId)) {
      applicationName = getAppName(contrast, appId);

      if (StringUtils.isNotBlank(appName)) {
        getLog().info("Using 'appId' property; 'appName' property is ignored.");
      }

    } else {
      applicationName = appName;
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
      applications = contrastSDK.getApplication(orgUuid, applicationId);
    } catch (Exception e) {
      String logMessage;
      if (e.getMessage().contains("403")) {
        logMessage =
            "\n\n Unable to find the application on TeamServer with the id ["
                + applicationId
                + "]\n";
      } else {
        logMessage =
            "\n\n Unable to retrieve the application list from TeamServer. Please check that TeamServer is running at this address ["
                + apiUrl
                + "]\n";
      }
      throw new MojoExecutionException(logMessage, e);
    }
    if (applications.getApplication() == null) {
      throw new MojoExecutionException(
          "\n\nApplication with id '"
              + applicationId
              + "' not found. Make sure this application appears in TeamServer under the 'Applications' tab.\n");
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

    String appVersionQualifier = "";
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
    if (StringUtils.isNotBlank(appId)) {
      computedAppVersion = applicationName + "-" + appVersionQualifier;
    } else {
      computedAppVersion = appName + "-" + appVersionQualifier;
    }

    return computedAppVersion;
  }

  String computeSessionMetadata() {
    List<String> metadata = new ArrayList<String>();

    for (Map.Entry<String, String> entry : environmentToSessionMetadata.entrySet()) {
      String environmentValue = System.getenv(entry.getKey());

      if (environmentValue != null) {
        metadata.add(String.format("%s=%s", entry.getValue(), environmentValue));
      }
    }

    return StringUtils.join(metadata, ",");
  }

  String buildArgLine(String currentArgLine) {
    return buildArgLine(currentArgLine, appName);
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
    argLineBuilder.append(" -Dcontrast.server=").append(serverName);
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

      try {
        if (profile != null) {
          javaAgent = connection.getAgent(AgentType.JAVA, orgUuid, profile);
        } else {
          javaAgent = connection.getAgent(AgentType.JAVA, orgUuid);
        }
      } catch (IOException e) {
        throw new MojoExecutionException(
            "\n\nWe couldn't download the Java agent from TeamServer with this user ["
                + username
                + "]. Please check that all your credentials are correct. If everything is correct, please contact Contrast Support. The error is:",
            e);
      } catch (UnauthorizedException e) {
        throw new MojoExecutionException(
            "\n\nWe contacted TeamServer successfully but couldn't authorize with the credentials you provided. The error is:",
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
