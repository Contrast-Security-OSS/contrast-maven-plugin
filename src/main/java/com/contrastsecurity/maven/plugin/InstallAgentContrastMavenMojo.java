package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.util.Date;


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

        project.getProperties().setProperty("argLine", buildArgLine());

        verifyDateTime = new Date();

        getLog().info("Verifying there are no new vulnerabilities after time " + verifyDateTime.toString());
    }

}