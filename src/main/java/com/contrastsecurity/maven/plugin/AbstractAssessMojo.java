package com.contrastsecurity.maven.plugin;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/** Abstract mojo for mojos that report Assess data to a Contrast instrumented application */
abstract class AbstractAssessMojo extends AbstractContrastMojo {

  /**
   * As near as I can tell, there doesn't appear to be any way to share data between Mojo phases.
   * However, we need to compute the appVersion in the install phase and then use the
   * computedAppVersion in the verify phase. Setting the field to static is the only way I found for
   * it to work
   *
   * @deprecated [JG] the Maven solution for the aforementioned issue is to save the information to
   *     the file system between goals, so we will deprecate this static field in favor of that
   *     approach
   */
  @Deprecated static String computedAppVersion;

  @Parameter(property = "appName")
  private String appName;

  @Parameter(property = "appId")
  private String appId;

  // TODO[JG] why is this required?
  @Parameter(property = "serverName", required = true)
  private String serverName;

  void verifyAppIdOrNameNotBlank() throws MojoExecutionException {
    if (StringUtils.isBlank(appId) && StringUtils.isBlank(appName)) {
      throw new MojoExecutionException(
          "Please specify appId or appName in the plugin configuration.");
    }
  }

  String getAppName() {
    return appName;
  }

  /** For testing. Maven will set the field directly */
  void setAppName(String appName) {
    this.appName = appName;
  }

  String getAppId() {
    return appId;
  }

  /** For testing. Maven will set the field directly */
  void setAppId(String appId) {
    this.appId = appId;
  }

  String getServerName() {
    return serverName;
  }

  /** For testing. Maven will set the field directly */
  void setServerName(String serverName) {
    this.serverName = serverName;
  }
}
