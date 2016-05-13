package com.aspectsecurity.contrast;

import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.util.Date;
import java.util.Properties;


@Mojo(name = "install", defaultPhase = LifecyclePhase.VALIDATE, requiresOnline = true)
public class InstallAgentContrastMavenMojo extends AbstractContrastMavenPluginMojo {

    public void execute() throws MojoExecutionException {
        getLog().info("Attempting to connect to configured TeamServer...");

        ContrastSDK contrast = connectToTeamServer();

        getLog().info("Successfully authenticated to Teamserver. Attempting to install the Java agent.");

        File agentFile = installJavaAgent(contrast);

        Properties projectProperties = project.getProperties();

        if (jarPath != null) {
            projectProperties.setProperty("javaagent", jarPath);
        } else {
            projectProperties.setProperty("javaagent", agentFile.getName());
        }

        verifyDateTime = new Date();

        getLog().info("Verifying there are no new vulnerabilities after time " + verifyDateTime.toString());
    }

}


