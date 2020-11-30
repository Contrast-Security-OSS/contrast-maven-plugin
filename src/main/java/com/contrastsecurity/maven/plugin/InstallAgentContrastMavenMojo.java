package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


@Mojo(name = "install", defaultPhase = LifecyclePhase.VALIDATE, requiresOnline = true)
public class InstallAgentContrastMavenMojo extends AbstractContrastMavenPluginMojo {
    String applicationName;

    static Map<String, String> environmentToSessionMetadata = new TreeMap<String, String>();
    static {
        //Jenkins git plugin environment variables
        environmentToSessionMetadata.put("GIT_BRANCH", "branchName");
        environmentToSessionMetadata.put("GIT_COMMITTER_NAME", "committer");
        environmentToSessionMetadata.put("GIT_COMMIT", "commitHash");
        environmentToSessionMetadata.put("GIT_URL", "repository");
        environmentToSessionMetadata.put("GIT_URL_1", "repository");

        //CI build number environment variables
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
        project.getProperties().setProperty("argLine", buildArgLine(project.getProperties().getProperty("argLine"), applicationName));

        for (Plugin plugin : (List<Plugin>)project.getBuildPlugins()) {
            if ("org.springframework.boot".equals(plugin.getGroupId()) && "spring-boot-maven-plugin".equals(plugin.getArtifactId())) {
                getLog().debug("Found the spring-boot-maven-plugin, with configuration:");
                String configuration = plugin.getConfiguration().toString();
                getLog().debug(configuration);
                if (configuration.contains("${argLine}")) {
                 getLog().info("Skipping set of -Drun.jvmArguments as it references ${argLine}");
                } else {
                    String jvmArguments = buildArgLine(project.getProperties().getProperty("run.jvmArguments"), applicationName);
                    getLog().info(String.format("Setting -Drun.jvmArguments=%s", jvmArguments));
                    project.getProperties().setProperty("run.jvmArguments", jvmArguments);
                }

                break;
            }
        }
    }

    public String computeAppVersion(Date currentDate) {
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
        if(travisBuildNumber != null) {
            getLog().info("Build is running in TravisCI. We'll use TRAVIS_BUILD_NUMBER [" + travisBuildNumber + "]");
            appVersionQualifier = travisBuildNumber;
        } else if (circleBuildNum != null) {
            getLog().info("Build is running in CircleCI. We'll use CIRCLE_BUILD_NUM [" + circleBuildNum + "]");
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

    public String computeSessionMetadata() {
        List<String> metadata = new ArrayList<String>();

        for(Map.Entry<String, String> entry: environmentToSessionMetadata.entrySet()) {
            String environmentValue = System.getenv(entry.getKey());

            if (environmentValue != null) {
                metadata.add(String.format("%s=%s", entry.getValue(), environmentValue));
            }
        }

        return StringUtils.join(metadata, ",");
    }

    public String buildArgLine(String currentArgLine) {
        return buildArgLine(currentArgLine, appName);
    }

    public String buildArgLine(String currentArgLine, String applicationName) {

        if(currentArgLine == null) {
            getLog().info("Current argLine is null");
            currentArgLine = "";
        } else {
            getLog().info("Current argLine is [" + currentArgLine + "]");
        }

        if(skipArgLine) {
            getLog().info("skipArgLine is set to false.");
            getLog().info("You will need to configure the Maven argLine property manually for the Contrast agent to work.");
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
            argLineBuilder.append(" -Dcontrast.application.session_metadata='").append(sessionMetadata).append("'");
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
            argLineBuilder.append(" -Dcontrast.application.session_metadata='").append(applicationSessionMetadata).append("'");
        }

        if (!StringUtils.isEmpty(applicationTags)) {
            argLineBuilder.append(" -Dcontrast.application.tags=").append(applicationTags);
        }

        String newArgLine = argLineBuilder.toString();

        getLog().info("Updated argLine is " + newArgLine);
        return newArgLine.trim();
    }
}