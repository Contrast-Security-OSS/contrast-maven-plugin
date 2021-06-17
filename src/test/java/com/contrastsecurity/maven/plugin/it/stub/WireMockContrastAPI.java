package com.contrastsecurity.maven.plugin.it.stub;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilterAction.continueWith;
import static com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilterAction.stopWith;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilter;
import com.github.tomakehurst.wiremock.extension.requestfilter.RequestFilterAction;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

final class WireMockContrastAPI implements ContrastAPI {

  private final WireMockServer server =
      new WireMockServer(wireMockConfig().dynamicPort().extensions(new AuthorizationFilter()));

  @Override
  public void start() {
    server.start();
    server.stubFor(
        get("/ng/" + ORGANIZATION_ID + "/agents/default/java")
            .withQueryParam("jvm", equalTo("1_6"))
            .willReturn(aResponse().withBodyFile("contrast-agent.jar")));
  }

  @Override
  public ConnectionParameters connection() {
    return ConnectionParameters.builder()
        .url(server.baseUrl())
        .username(USER_NAME)
        .apiKey(API_KEY)
        .serviceKey(SERVICE_KEY)
        .organizationID(ORGANIZATION_ID)
        .build();
  }

  @Override
  public void stop() {
    server.stop();
    for (final LoggedRequest request : server.findAllUnmatchedRequests()) {
      System.out.println(request);
    }
  }

  private static final class AuthorizationFilter implements RequestFilter {

    @Override
    public RequestFilterAction filter(final Request request) {
      // local pair class for iterating over common header verification logic
      class ExpectedHeader {
        final String name;
        final String value;

        ExpectedHeader(final String name, final String value) {
          this.name = name;
          this.value = value;
        }
      }
      final List<ExpectedHeader> expectedHeaders =
          Arrays.asList(
              new ExpectedHeader("API-Key", API_KEY),
              new ExpectedHeader("Authorization", AUTHORIZATION));
      for (final ExpectedHeader expected : expectedHeaders) {
        final String value = request.getHeader(expected.name);
        if (value == null) {
          return stopWith(aResponse().withStatus(401).build());
        }
        if (!expected.value.equals(value)) {
          return stopWith(aResponse().withStatus(403).build());
        }
      }
      return continueWith(request);
    }

    @Override
    public boolean applyToAdmin() {
      return false;
    }

    @Override
    public boolean applyToStubs() {
      return true;
    }

    @Override
    public String getName() {
      return "auth-filter";
    }
  }

  private static final String USER_NAME = "test-user";
  private static final String API_KEY = "test-api-key";
  private static final String SERVICE_KEY = "test-service-key";
  private static final String ORGANIZATION_ID = "organization-id";
  public static final String AUTHORIZATION =
      Base64.getEncoder()
          .encodeToString((USER_NAME + ":" + SERVICE_KEY).getBytes(StandardCharsets.US_ASCII));
}
