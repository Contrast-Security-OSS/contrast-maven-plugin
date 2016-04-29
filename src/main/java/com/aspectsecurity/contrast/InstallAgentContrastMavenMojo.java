package com.aspectsecurity.contrast;

import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;

@Mojo(name = "install", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresOnline = true)
public class InstallAgentContrastMavenMojo extends AbstractContrastMavenPluginMojo {

    private static final String AGENT_NAME = "contrast.jar";

    public void execute() throws MojoExecutionException {
        // run integration tests

        // Fail based on discovery of new vulnerabilities

        // configure maven for vulnerability types

        ContrastSDK contrast = connectToTeamserver();

        getLog().info("Successfully authenticated to Teamserver. Attempting to download the latest Java agent.");

        File agentFile = installJavaAgent(contrast);
    }

}


