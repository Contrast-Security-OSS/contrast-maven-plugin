package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.sdk.ContrastSDK;
import java.text.SimpleDateFormat;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.util.Date;


@Mojo(name = "install", defaultPhase = LifecyclePhase.VALIDATE, requiresOnline = true)
public class InstallAgentContrastMavenMojo extends AbstractContrastMavenPluginMojo {

    public void execute() throws MojoExecutionException {
        getLog().info("Attempting to connect to configured TeamServer...");

        ContrastSDK contrast = connectToTeamServer();

        getLog().info("Successfully authenticated to Teamserver. Attempting to install the Java agent.");

        File agentFile = installJavaAgent(contrast);

        getLog().info("Agent downloaded.");

        project.getProperties().setProperty("argLine", buildArgLine(project.getProperties().getProperty("argLine")));
    }

    public String computeAppVersion(Date currentDate) {
        if (computedAppVersion != null) {
            return computedAppVersion;
        }

        if (userSpecifiedAppVersion != null) {
            computedAppVersion = userSpecifiedAppVersion;
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
        computedAppVersion = appName + "-" + appVersionQualifier;
        return computedAppVersion;
    }

    public String buildArgLine(String currentArgLine) {

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
        argLineBuilder.append(" -Dcontrast.override.appname=").append(appName);
        argLineBuilder.append(" -Dcontrast.server=").append(serverName);
        argLineBuilder.append(" -Dcontrast.env=qa");
        argLineBuilder.append(" -Dcontrast.override.appversion=").append(computedAppVersion);

        String newArgLine = argLineBuilder.toString();

        getLog().info("Updated argLine is " + newArgLine);
        return newArgLine.trim();
    }

}