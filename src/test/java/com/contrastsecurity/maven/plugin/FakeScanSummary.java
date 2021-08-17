package com.contrastsecurity.maven.plugin;

import com.contrastsecurity.sdk.scan.ScanSummary;
import com.google.auto.value.AutoValue;
import java.time.Duration;
import java.time.Instant;

/** Fake implementation of {@link ScanSummary} for testing. */
@AutoValue
abstract class FakeScanSummary implements ScanSummary {

  /** new {@link Builder} */
  static Builder builder() {
    return new AutoValue_FakeScanSummary.Builder();
  }

  /** Builder for {@link ScanSummary}. */
  @AutoValue.Builder
  abstract static class Builder {

    /** @see ScanSummary#id() */
    abstract Builder id(String value);

    /** @see ScanSummary#scanId() */
    abstract Builder scanId(String value);

    /** @see ScanSummary#projectId() */
    abstract Builder projectId(String value);

    /** @see ScanSummary#organizationId() */
    abstract Builder organizationId(String value);

    /** @see ScanSummary#duration() */
    abstract Builder duration(Duration value);

    /** @see ScanSummary#totalResults() */
    abstract Builder totalResults(int value);

    /** @see ScanSummary#totalNewResults() () */
    abstract Builder totalNewResults(int value);

    /** @see ScanSummary#totalFixedResults() () */
    abstract Builder totalFixedResults(int value);

    /** @see ScanSummary#createdDate() */
    abstract Builder createdDate(Instant value);

    /** @return new {@link ScanSummary} */
    abstract FakeScanSummary build();
  }
}
