package com.contrastsecurity.maven.plugin;

import static org.junit.Assert.*;

import com.contrastsecurity.http.RuleSeverity;
import com.contrastsecurity.http.TraceFilterForm;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class VerifyContrastMavenPluginMojoTest {

  VerifyContrastMavenPluginMojo verifyContrastMavenPluginMojo;

  @Before
  public void setUp() {
    verifyContrastMavenPluginMojo = new VerifyContrastMavenPluginMojo();
    verifyContrastMavenPluginMojo.minSeverity = "Medium";
  }

  @Test
  public void testGetTraceFilterFormServerIdsNull() {
    TraceFilterForm traceFilterForm = verifyContrastMavenPluginMojo.getTraceFilterForm(null);
    assertNull(traceFilterForm.getServerIds());
  }

  @Test
  public void testGetTraceFilterForm() {
    List<Long> serverIds = new ArrayList<Long>();
    long server1 = 123l;
    long server2 = 456l;
    long server3 = 789l;

    serverIds.add(server1);
    serverIds.add(server2);
    serverIds.add(server3);

    TraceFilterForm traceFilterForm = verifyContrastMavenPluginMojo.getTraceFilterForm(serverIds);
    assertNotNull(traceFilterForm.getServerIds());
    assertEquals(3, traceFilterForm.getServerIds().size());
    assertEquals((Long) server1, traceFilterForm.getServerIds().get(0));
    assertEquals((Long) server2, traceFilterForm.getServerIds().get(1));
    assertEquals((Long) server3, traceFilterForm.getServerIds().get(2));
  }

  @Test
  public void testGetTraceFilterFormSeverities() {
    verifyContrastMavenPluginMojo.minSeverity = "Note";
    TraceFilterForm traceFilterForm = verifyContrastMavenPluginMojo.getTraceFilterForm(null);

    assertEquals(5, traceFilterForm.getSeverities().size());
    assertTrue(traceFilterForm.getSeverities().contains(RuleSeverity.NOTE));
    assertTrue(traceFilterForm.getSeverities().contains(RuleSeverity.LOW));
    assertTrue(traceFilterForm.getSeverities().contains(RuleSeverity.MEDIUM));
    assertTrue(traceFilterForm.getSeverities().contains(RuleSeverity.HIGH));
    assertTrue(traceFilterForm.getSeverities().contains(RuleSeverity.CRITICAL));
  }

  @Test
  public void testGetTraceFilterFormAppVersionTags() {
    String appVersion = "WebGoat-1";

    verifyContrastMavenPluginMojo.computedAppVersion = appVersion;
    TraceFilterForm traceFilterForm = verifyContrastMavenPluginMojo.getTraceFilterForm(null);

    assertEquals(1, traceFilterForm.getAppVersionTags().size());
    assertEquals(appVersion, traceFilterForm.getAppVersionTags().get(0));
  }
}
