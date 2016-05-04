package com.aspectsecurity.contrast;

import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.commons.lang.NullArgumentException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.util.Date;


@Mojo(name = "install", defaultPhase = LifecyclePhase.INITIALIZE, requiresOnline = true)
public class InstallAgentContrastMavenMojo extends AbstractContrastMavenPluginMojo {

    public void execute() throws MojoExecutionException {
        super.execute();

        getLog().info("Attempting to connect to configured Teamserver...");

        ContrastSDK contrast = connectToTeamserver();

        getLog().info("Successfully authenticated to Teamserver. Attempting to download the latest Java agent.");

        File agentFile = installJavaAgent(contrast);

        verifyDateTime = new Date();

        getLog().info("Verifying there are no new vulnerabilities after time " + verifyDateTime.toString());

    }

}


