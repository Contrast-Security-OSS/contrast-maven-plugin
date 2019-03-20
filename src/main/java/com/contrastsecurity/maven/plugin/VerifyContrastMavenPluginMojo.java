package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.RuleSeverity;
import com.contrastsecurity.http.ServerFilterForm;
import com.contrastsecurity.http.TraceFilterForm;
import com.contrastsecurity.models.*;
import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

@Mojo(name = "verify", requiresOnline = true)
public class VerifyContrastMavenPluginMojo extends AbstractContrastMavenPluginMojo {

    public void execute() throws MojoExecutionException {
        verifyParameters();
        ContrastSDK contrast = connectToTeamServer();

        getLog().info("Successfully authenticated to TeamServer.");

        getLog().info("Checking for new vulnerabilities for appVersion [" + computedAppVersion + "]");

        String applicationId;
        if (StringUtils.isNotBlank(appId)) {
            applicationId = appId;

            if (StringUtils.isNotBlank(appName)) {
                getLog().info("Using 'appId' property; 'appName' property is ignored.");
            }

        } else {
            applicationId = getApplicationId(contrast, appName);
        }

        List<Long> serverIds = null;

        if (StringUtils.isNotBlank(serverId)) {
            if (StringUtils.isNotBlank(serverName)) {
                getLog().info("Using 'serverId' property; 'serverName' property is ignored.");
            }
            serverIds = new ArrayList<Long>();
            serverIds.add(Long.valueOf(serverId));
        } else {
            serverIds = getServerId(contrast, applicationId);
        }

        TraceFilterForm form = getTraceFilterForm(serverIds);

        getLog().info("Sending vulnerability request to TeamServer.");

        Traces traces;

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            traces = contrast.getTraces(orgUuid, applicationId, form);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to retrieve the traces.", e);
        } catch (UnauthorizedException e) {
            throw new MojoExecutionException("Unable to connect to TeamServer.", e);
        }

        if (traces != null && traces.getCount() > 0) {
            getLog().info(traces.getCount() + " new vulnerability(s) were found.");

            for (Trace trace: traces.getTraces()) {
                getLog().info(generateTraceReport(trace));
            }

            throw new MojoExecutionException("Your application is vulnerable. Please see the above report for new vulnerabilities.");
        } else {
            getLog().info("No new vulnerabilities were found.");
        }

        getLog().info("Finished verifying your application.");
    }

    TraceFilterForm getTraceFilterForm(List<Long> serverIds) {
        TraceFilterForm form = new TraceFilterForm();
        form.setSeverities(getSeverityList(minSeverity));
        form.setAppVersionTags(Collections.singletonList(computedAppVersion));
        if (serverIds != null) {
            form.setServerIds(serverIds);
        }
        return form;
    }

    /** Retrieves the server id by server name
     *
     * @param sdk Contrast SDK object
     * @param applicationId application id to filter on
     * @return List<Long> id of the servers
     * @throws MojoExecutionException
     */
    private List<Long> getServerId(ContrastSDK sdk, String applicationId) throws MojoExecutionException {
        ServerFilterForm serverFilterForm = new ServerFilterForm();
        serverFilterForm.setApplicationIds(Arrays.asList(applicationId));

        Servers servers;
        List<Long> serverIds;

        try {
            serverFilterForm.setQ(URLEncoder.encode(serverName, "UTF-8"));
            servers = sdk.getServersWithFilter(orgUuid, serverFilterForm);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to retrieve the servers.", e);
        } catch (UnauthorizedException e) {
            throw new MojoExecutionException("Unable to connect to TeamServer.", e);
        }

        if (!servers.getServers().isEmpty()) {
            serverIds = new ArrayList<Long>();
            for (Server server : servers.getServers()) {
                serverIds.add(server.getServerId());
            }
        } else {
            throw new MojoExecutionException("\n\nServer with name '" + serverName + "' not found. Make sure this server name appears in TeamServer under the 'Servers' tab.\n");
        }

        return serverIds;
    }

    /**
     * Creates a basic report for a Trace object
     *
     * @param trace Trace object
     * @return String report
     */
    private String generateTraceReport(Trace trace) {
        StringBuilder sb = new StringBuilder();
        sb.append("Trace: ");
        sb.append(trace.getTitle().replaceAll("\\{\\{\\#unlicensed\\}\\}", "(").replaceAll("\\{\\{\\/unlicensed\\}\\}", ")"));
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
    private static EnumSet<RuleSeverity> getSeverityList(String severity) {

        List<String> severityList = SEVERITIES.subList(SEVERITIES.indexOf(severity), SEVERITIES.size());

        List<RuleSeverity> ruleSeverities = new ArrayList<RuleSeverity>();

        for (String severityToAdd : severityList) {
            ruleSeverities.add(RuleSeverity.valueOf(severityToAdd.toUpperCase()));
        }

        return EnumSet.copyOf(ruleSeverities);
    }

    // Severity levels
    private static final List<String> SEVERITIES = Arrays.asList("Note", "Low", "Medium", "High", "Critical");
}