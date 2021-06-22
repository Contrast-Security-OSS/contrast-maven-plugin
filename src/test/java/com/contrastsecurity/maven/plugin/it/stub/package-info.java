/**
 * JUnit extension for stubbing the Contrast API for integration testing. Before a test, the
 * extension starts a new web server that simulates the subset of the Contrast API that the plugin
 * needs. At the conclusion of the test, the extension terminates the web server.
 *
 * <p>Some tests may be compatible with an external Contrast API system (that has already been
 * configured to be in the right state) instead of a stub. In this case, test authors can configure
 * this extension (using standard JUnit configuration) to provide connection parameters to the
 * external system instead of starting a stub system.
 *
 * <p>Set the following configuration parameters to configure the extension to use an external
 * Contrast API system instead of starting a stub:
 *
 * <ul>
 *   <li>{@code contrast.api.url}
 *   <li>{@code contrast.api.user_name}
 *   <li>{@code contrast.api.api_key}
 *   <li>{@code contrast.api.service_key}
 *   <li>{@code contrast.api.organization}
 * </ul>
 */
package com.contrastsecurity.maven.plugin.it.stub;
