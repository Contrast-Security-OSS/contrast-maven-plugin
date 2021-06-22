package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.sdk.ContrastSDK;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;

abstract class AbstractContrastMojo extends AbstractMojo {

  @Parameter(defaultValue = "${settings}", readonly = true)
  protected Settings settings;

  @Parameter(property = "username", required = true)
  protected String username;

  @Parameter(property = "apiKey", required = true)
  protected String apiKey;

  @Parameter(property = "serviceKey", required = true)
  protected String serviceKey;

  @Parameter(property = "apiUrl")
  protected String apiUrl;

  // TODO[JG] must this be required? If a user is only in one org, we can look it up using the
  // endpoint /ng/profile/organizations
  @Parameter(property = "orgUuid", required = true)
  protected String orgUuid;

  // true = Override proxy from settings and use args
  @Parameter(property = "useProxy")
  private boolean useProxy;

  @Parameter(property = "proxyHost")
  private String proxyHost;

  @Parameter(property = "proxyPort")
  private int proxyPort;

  /*
   * As near as I can tell, there doesn't appear to be any way
   * to share data between Mojo phases. However, we need to compute
   * the appVersion in the install phase and then use the computedAppVersion
   * in the verify phase. Setting the field to static is the only
   * way I found for it to work
   */
  protected static String computedAppVersion;

  public void execute() throws MojoExecutionException {}

  Proxy getProxy() throws MojoExecutionException {
    Proxy proxy = Proxy.NO_PROXY;
    final org.apache.maven.settings.Proxy proxySettings = settings.getActiveProxy();
    if (useProxy) {
      getLog().info(String.format("Using a proxy %s:%s", proxyHost, proxyPort));
      if (proxyHost != null && proxyPort != 0) {
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
      } else {
        throw new MojoExecutionException(
            "When useProxy is true, proxyHost and proxyPort is required.");
      }
    } else if (proxySettings != null) {
      getLog()
          .info(
              String.format(
                  "Using a proxy %s:%s", proxySettings.getHost(), proxySettings.getPort()));
      proxy =
          new Proxy(
              Proxy.Type.HTTP,
              new InetSocketAddress(proxySettings.getHost(), proxySettings.getPort()));

      if (proxySettings.getUsername() != null || proxySettings.getPassword() != null) {
        Authenticator.setDefault(
            new Authenticator() {
              @Override
              protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestorType() == RequestorType.PROXY
                    && getRequestingHost().equalsIgnoreCase(proxySettings.getHost())
                    && proxySettings.getPort() == getRequestingPort()) {
                  return new PasswordAuthentication(
                      proxySettings.getUsername(),
                      proxySettings.getPassword() == null
                          ? null
                          : proxySettings.getPassword().toCharArray());
                } else {
                  return null;
                }
              }
            });
      }
    }

    return proxy;
  }

  ContrastSDK connectToTeamServer() throws MojoExecutionException {
    Proxy proxy = getProxy();

    try {
      if (!StringUtils.isEmpty(apiUrl)) {
        return new ContrastSDK.Builder(username, serviceKey, apiKey)
            .withApiUrl(apiUrl)
            .withProxy(proxy)
            .build();
      } else {
        return new ContrastSDK.Builder(username, serviceKey, apiKey).withProxy(proxy).build();
      }
    } catch (IllegalArgumentException e) {
      throw new MojoExecutionException(
          "\n\nWe couldn't connect to TeamServer at this address [" + apiUrl + "]. The error is: ",
          e);
    }
  }
}
