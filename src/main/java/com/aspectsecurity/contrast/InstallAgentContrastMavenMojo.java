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

    private static final String JAVAAGENT_PROPERTY = "javaagent";
    private static final String CONTRAST_ENABLED = "contrast.enabled";

    public void execute() throws MojoExecutionException {
        getLog().info("Attempting to connect to configured Teamserver...");

        ContrastSDK contrast = connectToTeamserver();

        getLog().info("Successfully authenticated to Teamserver. Attempting to install the Java agent.");

        File agentFile = installJavaAgent(contrast);

        final Properties projectProperties = project.getProperties();

        if (jarPath != null) {
            projectProperties.setProperty(JAVAAGENT_PROPERTY, jarPath);
        } else {
            projectProperties.setProperty(JAVAAGENT_PROPERTY, agentFile.getName());
        }

        projectProperties.setProperty(CONTRAST_ENABLED, "true");

        verifyDateTime = new Date();

        getLog().info("Verifying there are no new vulnerabilities after time " + verifyDateTime.toString());
    }

}


