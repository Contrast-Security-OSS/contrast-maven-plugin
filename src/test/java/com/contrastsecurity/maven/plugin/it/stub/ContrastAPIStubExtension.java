package com.contrastsecurity.maven.plugin.it.stub;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public final class ContrastAPIStubExtension
    implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(final ExtensionContext context) throws Exception {
    final ContrastAPI contrast = getContrastAPI(context);
    contrast.start();
  }

  @Override
  public void afterEach(final ExtensionContext context) throws Exception {
    final ContrastAPI contrast = getContrastAPI(context);
    contrast.stop();
  }

  @Override
  public boolean supportsParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType() == ContrastAPI.class;
  }

  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return getContrastAPI(extensionContext);
  }

  private static ContrastAPI getContrastAPI(final ExtensionContext context) {
    return context
        .getStore(NAMESPACE)
        .getOrComputeIfAbsent(
            "server", ignored -> ContrastAPI.createFromEnvironment(), ContrastAPI.class);
  }

  private static final Namespace NAMESPACE = Namespace.create(ContrastAPIStubExtension.class);
}
