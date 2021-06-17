package com.contrastsecurity.maven.plugin.it.stub;

import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 test extension that provides test authors with a stubbed instance of the Contrast API.
 */
public final class ContrastAPIStubExtension
    implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

  /**
   * Starts the {@link ContrastAPI}
   *
   * @param context JUnit context
   */
  @Override
  public void beforeEach(final ExtensionContext context) {
    final ContrastAPI contrast = getContrastAPI(context);
    contrast.start();
  }

  /**
   * Stops the {@link ContrastAPI}
   *
   * @param context JUnit context
   */
  @Override
  public void afterEach(final ExtensionContext context) {
    final ContrastAPI contrast = getContrastAPI(context);
    contrast.stop();
  }

  /** @return true if the parameter is of type {@link ContrastAPI} */
  @Override
  public boolean supportsParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType() == ContrastAPI.class;
  }

  /** @return the {@link ContrastAPI} in the current test context */
  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return getContrastAPI(extensionContext);
  }

  /** @return new or existing {@link ContrastAPI} in the current test context */
  private static ContrastAPI getContrastAPI(final ExtensionContext context) {
    return context
        .getStore(NAMESPACE)
        .getOrComputeIfAbsent(
            "server", ignored -> createFromConfiguration(context), ContrastAPI.class);
  }

  /**
   * @param context the current JUnit {@link ExtensionContext}
   * @return {@link ExternalContrastAPI} if Contrast connection properties are provided, otherwise a
   *     {@link FakeContrastAPI}
   */
  private static ContrastAPI createFromConfiguration(final ExtensionContext context) {
    // gather configuration parameters from the current context
    final Optional<String> url = context.getConfigurationParameter("contrast.api.url");
    final Optional<String> username = context.getConfigurationParameter("contrast.api.user_name");
    final Optional<String> apiKey = context.getConfigurationParameter("contrast.api.api_key");
    final Optional<String> serviceKey =
        context.getConfigurationParameter("contrast.api.service_key");
    final Optional<String> organization =
        context.getConfigurationParameter("contrast.api.organization");

    // if all connection parameters are present, then use end-to-end testing mode
    if (url.isPresent()
        && username.isPresent()
        && apiKey.isPresent()
        && serviceKey.isPresent()
        && organization.isPresent()) {
      context.publishReportEntry(
          "end-to-end testing enabled: using provided Contrast API connection instead of the stub");
      final ConnectionParameters connection =
          ConnectionParameters.builder()
              .url(url.get())
              .username(username.get())
              .apiKey(apiKey.get())
              .serviceKey(serviceKey.get())
              .organizationID(organization.get())
              .build();
      return new ExternalContrastAPI(connection);
    }
    // default case, use a fake Contrast API
    return new FakeContrastAPI();
  }

  private static final Namespace NAMESPACE = Namespace.create(ContrastAPIStubExtension.class);
}
