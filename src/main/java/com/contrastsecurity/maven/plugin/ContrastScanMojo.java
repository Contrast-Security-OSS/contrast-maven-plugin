package com.contrastsecurity.maven.plugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "scan", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresOnline = true)
public class ContrastScanMojo extends AbstractContrastMojo {}
