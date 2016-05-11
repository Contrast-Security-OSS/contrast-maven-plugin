package com.aspectsecurity.contrast;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.FilterForm;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "verify", requiresOnline = true)
public class VerifyContrastMavenPluginMojo extends AbstractContrastMavenPluginMojo {

    public void execute() throws MojoExecutionException {
        getLog().info("Checking for new vulnerabilities...");

        ContrastSDK contrast = connectToTeamserver();

        getLog().info("Successfully authenticated to Teamserver.");

        FilterForm form = new FilterForm();
        form.setSeverities(getSeverityList(minSeverity));
        form.setStartDate(verifyDateTime);

        getLog().info("Sending vulnerability request to Teamserver.");

        Traces traces = null;

        try {
            traces = contrast.getTracesWithFilter(orgUuid, appId, form);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to retrieve the traces.", e);
        } catch (UnauthorizedException e) {
            throw new MojoExecutionException("Unable to connect to Teamserver.", e);
        }

        if (traces != null && traces.getCount() > 0) {
            throw new MojoExecutionException(traces.getCount() + " new vulnerability(s) were found!");
        } else {
            getLog().info("No new vulnerabilities were found!");
        }

        getLog().info("Finished verifying your application.");
    }

    // Returns the sublist of severities greater than or equal to the configured severity level
    private static List<String> getSeverityList(String severity) {
        return SEVERITIES.subList(SEVERITIES.indexOf(severity), SEVERITIES.size());
    }

    // Severity levels
    private static final List<String> SEVERITIES = Arrays.asList("Note", "Low", "Medium", "High", "Critical");
}


