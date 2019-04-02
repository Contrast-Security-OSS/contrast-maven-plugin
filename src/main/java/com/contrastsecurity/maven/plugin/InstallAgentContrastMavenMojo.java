package com.contrastsecurity.maven.plugin;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;


@Mojo(name = "install", defaultPhase = LifecyclePhase.VALIDATE, requiresOnline = true)
public class InstallAgentContrastMavenMojo extends AbstractContrastMavenPluginMojo {

    public void execute() throws MojoExecutionException {
        init();
        File agentFile = installJavaAgent(contrastSdk);
        getLog().info("Agent downloaded to " + agentFile);
        String argLine = buildArgLine(project.getProperties().getProperty("argLine"));
        project.getProperties().setProperty("argLine", argLine);
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

        StringBuilder argLineBuilder = new StringBuilder();
        argLineBuilder.append(currentArgLine);
        argLineBuilder.append(" -javaagent:").append(contrastAgentLocation);
        argLineBuilder.append(" -Dcontrast.server=").append(serverName);
        argLineBuilder.append(" -Dcontrast.env=qa");
        argLineBuilder.append(" -Dcontrast.override.appversion=").append(computedAppVersion);
        argLineBuilder.append(" -Dcontrast.reporting.period=").append("200");

        if (standalone) {
            argLineBuilder.append(" -Dcontrast.standalone.appname=").append(computedAppName);
        } else {
            argLineBuilder.append(" -Dcontrast.override.appname=").append(computedAppName);
        }

        if (!StringUtils.isEmpty(serverPath)) {
            argLineBuilder.append(" -Dcontrast.path=").append(serverPath);
        }

        String newArgLine = argLineBuilder.toString();

        getLog().info("Updated argLine is " + newArgLine);
        return newArgLine.trim();
    }
}