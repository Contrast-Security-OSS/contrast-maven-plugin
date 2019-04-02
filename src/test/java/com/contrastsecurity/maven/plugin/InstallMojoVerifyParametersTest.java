package com.contrastsecurity.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Before;
import org.junit.Test;

public class InstallMojoVerifyParametersTest {
    InstallAgentContrastMavenMojo installMojo;

    @Before
    public void setUp() throws Exception {
        installMojo = new InstallAgentContrastMavenMojo();
    }

    @Test(expected = MojoExecutionException.class)
    public void testNoAppIdOrName() throws MojoExecutionException {
        installMojo.verifyAppIdOrNameNotBlank();
    }

    @Test
    public void testNoAppId() throws MojoExecutionException {
        installMojo.appName = "caddyshack";
        installMojo.verifyAppIdOrNameNotBlank();
        //No exception
    }

    @Test
    public void testNoAppName() throws MojoExecutionException {
        installMojo.appId = "a2edf12";
        installMojo.verifyAppIdOrNameNotBlank();
        //No exception
    }

    @Test
    public void testBoth() throws MojoExecutionException {
        installMojo.appName = "caddyshack";
        installMojo.appId = "a2edf12";
        installMojo.verifyAppIdOrNameNotBlank();
        //No exception
    }
}
