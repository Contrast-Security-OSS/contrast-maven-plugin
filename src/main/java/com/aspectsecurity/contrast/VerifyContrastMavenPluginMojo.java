package com.aspectsecurity.contrast;

import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.FilterForm;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.sdk.ContrastSDK;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Mojo(name = "verify", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, requiresOnline = true)
public class VerifyContrastMavenPluginMojo extends AbstractContrastMavenPluginMojo {

    public void execute() throws MojoExecutionException {
        getLog().info("Integration tests have finished. Checking for new vulnerabilities...");

        ContrastSDK contrast = connectToTeamserver();

        getLog().info("Successfully authenticated to Teamserver.");

        // TODO build severities list based on parameter
        List<String> severities = new ArrayList<String>();
        severities.add("medium");
        severities.add("high");
        severities.add("critical");

        FilterForm form = new FilterForm();
        form.setSeverities(severities);
        form.setStartDate(verifiyDateTime);

        getLog().info("Sending vulnerability request to Teamserver");

        Traces traces;

        try {
            traces = contrast.getTracesWithFilter(orgUuid, appId, form);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to retrieve the traces.", e);
        } catch (UnauthorizedException e) {
            throw new MojoExecutionException("Unable to connect to Teamserver.", e);
        }

        if (traces.getCount() > 0) {
            throw new MojoExecutionException(traces.getCount() + " new vulnerability(s) were found after running the integration tests.");
        }
    }
}


