package com.contrastsecurity.maven.plugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InstallAgentContrastMavenMojoTest {
    InstallAgentContrastMavenMojo installMojo;
    Date now;

    @Before
    public void setUp() {
        installMojo = new InstallAgentContrastMavenMojo();
        installMojo.appName = "caddyshack";
        installMojo.serverName = "Bushwood";
        installMojo.contrastAgentLocation = "/usr/local/bin/contrast.jar";

        now = new Date();
    }

   @Test
   public void testGenerateAppVersion() {
        installMojo.appVersion = "mycustomversion";
        assertEquals("mycustomversion",installMojo.generateAppVersion(now));
   }

    @Test
    public void testGenerateAppVersionNoAppVersion() {
        String expectedVersion = new SimpleDateFormat("yyyyMMddHHmmss").format(now);
        assertEquals("caddyshack-" + expectedVersion,installMojo.generateAppVersion(now));
        assertEquals("caddyshack-" + expectedVersion,installMojo.generateAppVersion(now));
    }

    @Test
    public void testBuildArgLine() {
        installMojo.appVersion = "caddyshack-2";
        String currentArgLine = "";
        String expectedArgLine = "-javaagent:/usr/local/bin/contrast.jar -Dcontrast.override.appname=caddyshack -Dcontrast.server=Bushwood -Dcontrast.env=qa -Dcontrast.override.appversion=caddyshack-2";
        assertEquals(expectedArgLine, installMojo.buildArgLine(currentArgLine));
    }

    @Test
    public void testBuildArgNull() {
        installMojo.appVersion = "caddyshack-2";
        String currentArgLine = null;
        String expectedArgLine = "-javaagent:/usr/local/bin/contrast.jar -Dcontrast.override.appname=caddyshack -Dcontrast.server=Bushwood -Dcontrast.env=qa -Dcontrast.override.appversion=caddyshack-2";
        assertEquals(expectedArgLine, installMojo.buildArgLine(currentArgLine));
    }

    @Test
    public void testBuildArgLineAppend() {
        installMojo.appVersion = "caddyshack-2";
        String currentArgLine = "-Xmx1024m";
        String expectedArgLine = "-Xmx1024m -javaagent:/usr/local/bin/contrast.jar -Dcontrast.override.appname=caddyshack -Dcontrast.server=Bushwood -Dcontrast.env=qa -Dcontrast.override.appversion=caddyshack-2";
        assertEquals(expectedArgLine, installMojo.buildArgLine(currentArgLine));
    }

    @Test
    public void testBuildArgLineSkip() {
        installMojo.skipArgLine = true;
        String currentArgLine = "-Xmx1024m";
        String expectedArgLine = "-Xmx1024m";
        assertEquals(expectedArgLine, installMojo.buildArgLine(currentArgLine));
    }
}
