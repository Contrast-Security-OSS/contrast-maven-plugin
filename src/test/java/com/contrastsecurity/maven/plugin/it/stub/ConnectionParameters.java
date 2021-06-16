package com.contrastsecurity.maven.plugin.it.stub;

import com.google.auto.value.AutoValue;
import java.util.Properties;

@AutoValue
public abstract class ConnectionParameters {

  static Builder builder() {
    return new AutoValue_ConnectionParameters.Builder();
  }

  public abstract String url();

  public abstract String username();

  public abstract String apiKey();

  public abstract String serviceKey();

  public final Properties toProperties() {
    final Properties properties = new Properties();
    properties.setProperty("contrast.api.url", url());
    properties.setProperty("contrast.api.user_name", username());
    properties.setProperty("contrast.api.api_key", apiKey());
    properties.setProperty("contrast.api.service_key", serviceKey());
    return properties;
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder url(String value);

    public abstract Builder username(String value);

    public abstract Builder apiKey(String value);

    public abstract Builder serviceKey(String value);

    public abstract ConnectionParameters build();
  }
}
