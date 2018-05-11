package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.sdk.ContrastSDK;
import java.text.SimpleDateFormat;
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

    public String generateAppVersion(Date currentDate) {
        if (appVersion != null) {
            return appVersion;
        }

        String appVersionTimestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(currentDate);
        appVersion = appName + "-" + appVersionTimestamp;
        return appVersion;
    }

    public String buildArgLine(String currentArgLine) {

        if(currentArgLine == null) {
            getLog().info("currentArgLine is null");
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

        appVersion = generateAppVersion(new Date());

        String newArgLine = currentArgLine + " -javaagent:" + contrastAgentLocation + " -Dcontrast.override.appname=" + appName + " -Dcontrast.server=" + serverName + " -Dcontrast.env=qa -Dcontrast.override.appversion=" + appVersion;

        getLog().info("Updated argLine is " + newArgLine);
        return newArgLine.trim();
    }

}