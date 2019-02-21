package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.http.TraceFilterForm;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

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

}
