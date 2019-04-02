package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.models.Application;
import com.contrastsecurity.models.Applications;
import com.contrastsecurity.sdk.ContrastSDK;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InstallMojoAppIdTest {

    InstallAgentContrastMavenMojo installMojo;

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    ContrastSDK mockContrastSdk;

    @Before
    public void setUp() throws Exception {
        mockContrastSdk = mock(ContrastSDK.class);

        installMojo = new InstallAgentContrastMavenMojo();
        installMojo.contrastSdk = mockContrastSdk;
        installMojo.appId = "aeeefe323ef";
        installMojo.orgUuid = "4";
    }

    @Test
    public void testComputeAppNameIdOnly() throws Exception {
        String expectedApplicationName = "expectedApplicationName";

        Applications applications = mock(Applications.class);
        Application application = mock(Application.class);

        when(application.getName()).thenReturn(expectedApplicationName);
        when(applications.getApplication()).thenReturn(application);
        when(mockContrastSdk.getApplication(installMojo.orgUuid, installMojo.appId)).thenReturn(applications);

        String actualAppName = installMojo.computeAppName(mockContrastSdk);
        assertEquals(expectedApplicationName, actualAppName);
    }

    @Test
    public void testComputeAppIdAndNameSpecified() throws Exception {
        String expectedApplicationName = "exampleAppName";
        installMojo.appName = "wrongAppName";

        ContrastSDK sdk = mock(ContrastSDK.class);
        Applications applications = mock(Applications.class);
        Application application = mock(Application.class);

        when(application.getName()).thenReturn(expectedApplicationName);
        when(applications.getApplication()).thenReturn(application);
        when(sdk.getApplication(installMojo.orgUuid, installMojo.appId)).thenReturn(applications);

        String actualAppName = installMojo.computeAppName(sdk);
        assertEquals(expectedApplicationName, actualAppName);
    }
}
