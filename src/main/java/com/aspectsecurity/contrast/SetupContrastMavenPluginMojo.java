package com.aspectsecurity.contrast;

import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.util.Date;

@Mojo(name = "setup", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresOnline = true)
public class SetupContrastMavenPluginMojo extends AbstractContrastMavenPluginMojo {

    private static final String AGENT_NAME = "contrast.jar";

    public void execute() throws MojoExecutionException {
        ContrastSDK contrast = connectToTeamserver();

        getLog().info("Successfully authenticated to Teamserver. Attempting to download the latest Java agent.");

        File agentFile = installJavaAgent(contrast);


        verifiyDateTime = new Date();
    }
}

