package com.aspectsecurity.contrast;

import com.contrastsecurity.sdk.ContrastSDK;
import com.sun.tools.attach.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.util.Date;


@Mojo(name = "install", defaultPhase = LifecyclePhase.INITIALIZE, requiresOnline = true)
public class InstallAgentContrastMavenMojo extends AbstractContrastMavenPluginMojo {

    public void execute() throws MojoExecutionException {
        getLog().info("Attempting to connect to configured Teamserver...");

        ContrastSDK contrast = connectToTeamserver();

        getLog().info("Successfully authenticated to Teamserver. Attempting to install the Java agent.");

        File agentFile = installJavaAgent(contrast);

        getLog().info("Retrieving the current JVM. Attempting to attach to the target JVM.");

        VirtualMachine virtualMachine = null;

        try {
            virtualMachine = VirtualMachine.attach(getProcessId());
        } catch (AttachNotSupportedException e) {
            throw new MojoExecutionException("Unable to attach to this JVM.", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable load the JVM.", e);
        }

        if (agentFile.exists() && virtualMachine != null) {
            try {
                getLog().info("Attempting to attach the Java agent to the target JVM.");
                virtualMachine.loadAgent(agentFile.getName());
                // virtualMachine.detach();
            } catch (AgentLoadException e) {
                throw new MojoExecutionException("Unable to load the Java agent.", e);
            } catch (AgentInitializationException e) {
                throw new MojoExecutionException("Unable to initialize the Java agent.", e);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to load the Java agent.", e);
            }
        }

        verifyDateTime = new Date();

        getLog().info("Verifying there are no new vulnerabilities after time " + verifyDateTime.toString());
    }

}


