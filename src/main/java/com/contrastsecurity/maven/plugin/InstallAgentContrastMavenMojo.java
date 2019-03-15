package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;


@Mojo(name = "install", defaultPhase = LifecyclePhase.VALIDATE, requiresOnline = true)
public class InstallAgentContrastMavenMojo extends AbstractContrastMavenPluginMojo {
    String applicationName;

    public void execute() throws MojoExecutionException {
        verifyAppIdOrNameNotBlank();
        getLog().info("Attempting to connect to configured TeamServer...");

        ContrastSDK contrast = connectToTeamServer();

        getLog().info("Successfully authenticated to Teamserver. Attempting to install the Java agent.");

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
        argLineBuilder.append(" -Dcontrast.env=qa");
        argLineBuilder.append(" -Dcontrast.override.appversion=").append(computedAppVersion);
        argLineBuilder.append(" -Dcontrast.reporting.period=").append("200");

        argLineBuilder.append(" -Dapplication.name=").append(applicationName);

        if (!StringUtils.isEmpty(serverPath)) {
            argLineBuilder.append(" -Dcontrast.path=").append(serverPath);
        }

        String newArgLine = argLineBuilder.toString();

        getLog().info("Updated argLine is " + newArgLine);
        return newArgLine.trim();
    }
}