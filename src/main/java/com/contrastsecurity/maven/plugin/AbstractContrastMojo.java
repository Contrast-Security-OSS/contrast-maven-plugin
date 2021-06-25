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

/**
 * Abstract mojo for mojos that need to connect to Contrast. Handles authentication, organization
 * selection (multi-tenancy), and proxy configuration.
 *
 * <p>Extensions of this class use the {@link #connectToContrast()} to obtain an instance of the
 * {@link ContrastSDK} with which they can make requests to Contrast.
 */
abstract class AbstractContrastMojo extends AbstractMojo {

  @Parameter(defaultValue = "${settings}", readonly = true)
  private Settings settings;

  /**
   * User name for communicating with Contrast. Agent users lack permissions required by this
   * plugin. <a href="https://docs.contrastsecurity.com/en/personal-keys.html">Find your personal
   * keys</a>
   */
  @Parameter(alias = "username", required = true)
  private String userName;

  /**
   * API Key for communicating with Contrast. <a
   * href="https://docs.contrastsecurity.com/en/personal-keys.html">Find your personal keys</a>
   */
  @Parameter(property = "apiKey", required = true)
  private String apiKey;

  /**
   * Service Key for communicating with Contrast. <a
   * href="https://docs.contrastsecurity.com/en/personal-keys.html">Find your personal keys</a>
   */
  @Parameter(property = "serviceKey", required = true)
  private String serviceKey;

  /** Contrast API URL */
  @Parameter(alias = "apiUrl", defaultValue = "https://app.contrastsecurity.com/Contrast")
  private String url;

  /**
   * Unique ID for the Contrast Organization to which the plugin reports results. <a
   * href="https://docs.contrastsecurity.com/en/personal-keys.html">Find your Organization ID</a>
   */
  // TODO[JG] must this be required? If a user is only in one org, we can look it up using the
  // endpoint /ng/profile/organizations
  @Parameter(alias = "orgUuid", required = true)
  private String organizationID;

  /**
   * When true, will override Maven's proxy settings with Contrast Maven plugin specific proxy
   * configuration
   *
   * @deprecated in a future release, we will remove the proprietary proxy configuration in favor of
   *     standard Maven proxy configuration
   */
  @Parameter(property = "useProxy", defaultValue = "false")
  private boolean useProxy;

  /**
   * Proxy host used to communicate to Contrast when {@code useProxy} is true
   *
   * @deprecated in a future release, we will remove the proprietary proxy configuration in favor of
   *     standard Maven proxy configuration
   */
  @Parameter(property = "proxyHost")
  private String proxyHost;

  /**
   * Proxy port used to communicate to Contrast when {@code useProxy} is true
   *
   * @deprecated in a future release, we will remove the proprietary proxy configuration in favor of
   *     standard Maven proxy configuration
   */
  @Parameter(property = "proxyPort")
  private int proxyPort;

  String getUserName() {
    return userName;
  }

  void setUserName(final String userName) {
    this.userName = userName;
  }

  String getApiKey() {
    return apiKey;
  }

  void setApiKey(final String apiKey) {
    this.apiKey = apiKey;
  }

  String getServiceKey() {
    return serviceKey;
  }

  void setServiceKey(final String serviceKey) {
    this.serviceKey = serviceKey;
  }

  String getURL() {
    return url;
  }

  void setURL(final String url) {
    this.url = url;
  }

  String getOrganizationID() {
    return organizationID;
  }

  void setOrganizationID(final String organizationID) {
    this.organizationID = organizationID;
  }

  /**
   * @return new ContrastSDK configured to connect with the authentication and proxy parameters
   *     defined by this abstract mojo
   * @throws MojoExecutionException when fails to connect to Contrast
   */
  ContrastSDK connectToContrast() throws MojoExecutionException {
    Proxy proxy = getProxy();

    try {
      if (!StringUtils.isEmpty(url)) {
        return new ContrastSDK.Builder(userName, serviceKey, apiKey)
            .withApiUrl(url)
            .withProxy(proxy)
            .build();
      } else {
        return new ContrastSDK.Builder(userName, serviceKey, apiKey).withProxy(proxy).build();
      }
    } catch (IllegalArgumentException e) {
      throw new MojoExecutionException(
          "\n\nWe couldn't connect to Contrast at this address [" + url + "]. The error is: ", e);
    }
  }

  private Proxy getProxy() throws MojoExecutionException {
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
}
