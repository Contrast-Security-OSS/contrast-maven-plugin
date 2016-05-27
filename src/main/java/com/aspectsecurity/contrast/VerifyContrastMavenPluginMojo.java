package com.aspectsecurity.contrast;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.FilterForm;
import com.contrastsecurity.http.ServerFilterForm;
import com.contrastsecurity.models.Servers;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "verify", requiresOnline = true)
public class VerifyContrastMavenPluginMojo extends AbstractContrastMavenPluginMojo {

    public void execute() throws MojoExecutionException {
        getLog().info("Checking for new vulnerabilities...");

        ContrastSDK contrast = connectToTeamServer();

        getLog().info("Successfully authenticated to TeamServer.");

        ServerFilterForm serverFilterForm = new ServerFilterForm();
        serverFilterForm.setApplicationIds(Arrays.asList(appId));
        serverFilterForm.setQ(serverName);

        Servers servers;
        long serverId;

        try {
            servers = contrast.getServersWithFilter(orgUuid, serverFilterForm);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to retrieve the servers.", e);
        } catch (UnauthorizedException e) {
            throw new MojoExecutionException("Unable to connect to TeamServer.", e);
        }

        if (!servers.getServers().isEmpty()) {
            serverId = servers.getServers().get(0).getServerId();
        } else {
            throw new MojoExecutionException("Server with name '" + serverName + "' not found.");
        }

        FilterForm form = new FilterForm();
        form.setSeverities(getSeverityList(minSeverity));
        form.setStartDate(verifyDateTime);

        getLog().info("Sending vulnerability request to TeamServer.");

        Traces traces;

        try {
            traces = contrast.getTracesWithFilter(orgUuid, appId, "servers", Long.toString(serverId), form);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to retrieve the traces.", e);
        } catch (UnauthorizedException e) {
            throw new MojoExecutionException("Unable to connect to TeamServer.", e);
        }

        if (traces != null && traces.getCount() > 0) {
            getLog().info(traces.getCount() + " new vulnerability(s) were found! Printing vulnerability report.");

            for (Trace trace: traces.getTraces()) {
                getLog().info(generateTraceReport(trace));
            }

            throw new MojoExecutionException("Your application is vulnerable. Please see the above report for new vulnerabilities.");
        } else {
            getLog().info("No new vulnerabilities were found!");
        }

        getLog().info("Finished verifying your application.");
    }

    /**
     * Creates a basic report for a Trace object
     * @param trace Trace object
     * @return String report
     */
    private String generateTraceReport(Trace trace) {
        StringBuilder sb = new StringBuilder();
        sb.append("Trace: ");
        sb.append(trace.getTitle());
        sb.append("\nTrace Uuid: ");
        sb.append(trace.getUuid());
        sb.append("\nTrace Severity: ");
        sb.append(trace.getSeverity());
        sb.append("\nTrace Likelihood: ");
        sb.append(trace.getLikelihood());
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Returns the sublist of severities greater than or equal to the configured severity level
     *
     * @param severity include severity to filter with severity list with
     * @return list of severity strings
     */
    private static List<String> getSeverityList(String severity) {
        return SEVERITIES.subList(SEVERITIES.indexOf(severity), SEVERITIES.size());
    }

    // Severity levels
    private static final List<String> SEVERITIES = Arrays.asList("Note", "Low", "Medium", "High", "Critical");
}