package com.contrastsecurity.maven.plugin;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

class AbstractAssessMojo extends AbstractContrastMojo {

  @Parameter(property = "appName")
  protected String appName;

  @Parameter(property = "appId")
  protected String appId;

  // TODO[JG] why is this required?
  @Parameter(property = "serverName", required = true)
  protected String serverName;

  void verifyAppIdOrNameNotBlank() throws MojoExecutionException {
    if (StringUtils.isBlank(appId) && StringUtils.isBlank(appName)) {
      throw new MojoExecutionException(
          "Please specify appId or appName in the plugin configuration.");
    }
  }
}
