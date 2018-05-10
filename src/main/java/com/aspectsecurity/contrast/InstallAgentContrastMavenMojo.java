package com.aspectsecurity.contrast;

import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo(name = "install", defaultPhase = LifecyclePhase.VALIDATE, requiresOnline = true)
public class InstallAgentContrastMavenMojo extends AbstractContrastMavenPluginMojo {

//    @Parameter(defaultValue = "${project}")
//    private org.apache.maven.project.MavenProject project;

    // and in execute(), use it:

    public void execute() throws MojoExecutionException {
        getLog().info("Attempting to connect to configured TeamServer...");

        ContrastSDK contrast = connectToTeamServer();

        getLog().info("Successfully authenticated to Teamserver. Attempting to install the Java agent.");

        File agentFile = installJavaAgent(contrast);

        getLog().info("Agent downloaded.");

        getLog().info("Configuring argLine property.");

        String currentArgLine = project.getProperties().getProperty("argLine");

        if(currentArgLine == null) {
            getLog().info("currentArgLine is null");
            currentArgLine = "";
        } else {
            getLog().info("Current argLine is [" + currentArgLine + "]");
        }

        appVersion = generateAppVersion();

        String newArgLine = currentArgLine + " -javaagent:" + contrastAgentLocation + " -Dcontrast.override.appname=" + appName + " -Dcontrast.server=" + serverName + " -Dcontrast.env=qa -Dcontrast.override.appversion=" + appVersion;

        getLog().info("Updated argLine is " + newArgLine);

        project.getProperties().setProperty("argLine", newArgLine);

        verifyDateTime = new Date();

        getLog().info("Verifying there are no new vulnerabilities after time " + verifyDateTime.toString());
    }

}