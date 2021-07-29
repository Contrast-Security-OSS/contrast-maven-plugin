package com.contrastsecurity.maven.plugin.sdkx;

import java.time.ZonedDateTime;
import java.util.Objects;

/** Value type that describes a scan results summary */
public final class ScanSummary {

  /** @return new {@link Builder} */
  public static Builder builder() {
    return new Builder();
  }

  private final String id;
  private final String scanId;
  private final String organizationId;
  private final String projectId;
  private final long duration;
  private final int totalResults;
  private final int totalNewResults;
  private final int totalFixedResults;
  private final ZonedDateTime createdDate;

  /** visible for GSON */
  ScanSummary(
      final String id,
      final String scanId,
      final String projectId,
      final String organizationId,
      final long duration,
      final int totalResults,
      final int totalNewResults,
      final int totalFixedResults,
      final ZonedDateTime createdDate) {
    this.id = Objects.requireNonNull(id);
    this.scanId = Objects.requireNonNull(scanId);
    this.organizationId = Objects.requireNonNull(organizationId);
    this.projectId = Objects.requireNonNull(projectId);
    this.duration = duration;
    this.totalResults = totalResults;
    this.totalNewResults = totalNewResults;
    this.totalFixedResults = totalFixedResults;
    this.createdDate = Objects.requireNonNull(createdDate);
  }

  /** @return unique ID of this summary */
  public String getId() {
    return id;
  }

  /** @return unique ID of the scan */
  public String getScanId() {
    return scanId;
  }

  /** @return unique ID of the scan project */
  public String getProjectId() {
    return projectId;
  }

  /** @return unique ID of the Contrast organization */
  public String getOrganizationId() {
    return organizationId;
  }

  /** @return duration of the scan in milliseconds */
  public long getDuration() {
    return duration;
  }

  /** @return number of vulnerabilities detected in this scan */
  public int getTotalResults() {
    return totalResults;
  }

  /**
   * @return number of vulnerabilities detected in this scan that have not been previously detected
   *     in an earlier scan
   */
  public int getTotalNewResults() {
    return totalNewResults;
  }

  /**
   * @return number of vulnerabilities that are no longer detected but were detected in previous
   *     scans
   */
  public int getTotalFixedResults() {
    return totalFixedResults;
  }

  /** @return time at which this scan summary was created */
  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ScanSummary that = (ScanSummary) o;
    return duration == that.duration
        && totalResults == that.totalResults
        && totalNewResults == that.totalNewResults
        && totalFixedResults == that.totalFixedResults
        && id.equals(that.id)
        && scanId.equals(that.scanId)
        && organizationId.equals(that.organizationId)
        && projectId.equals(that.projectId)
        && createdDate.equals(that.createdDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        scanId,
        organizationId,
        projectId,
        duration,
        totalResults,
        totalNewResults,
        totalFixedResults,
        createdDate);
  }

  @Override
  public String toString() {
    return "ScanSummary{"
        + "id='"
        + id
        + '\''
        + ", scanId='"
        + scanId
        + '\''
        + ", organizationId='"
        + organizationId
        + '\''
        + ", projectId='"
        + projectId
        + '\''
        + ", duration="
        + duration
        + ", totalResults="
        + totalResults
        + ", totalNewResults="
        + totalNewResults
        + ", totalFixedResults="
        + totalFixedResults
        + ", createdDate="
        + createdDate
        + '}';
  }

  /** Builder for {@code ScanSummary} */
  public static final class Builder {

    private String id;
    private String scanId;
    private String organizationId;
    private String projectId;
    private long duration;
    private int totalResults;
    private int totalNewResults;
    private int totalFixedResults;
    private ZonedDateTime createdDate;

    public Builder id(final String id) {
      this.id = id;
      return this;
    }

    public Builder scanId(final String scanId) {
      this.scanId = scanId;
      return this;
    }

    public Builder organizationId(final String organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder projectId(final String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder duration(final long duration) {
      this.duration = duration;
      return this;
    }

    public Builder totalResults(final int totalResults) {
      this.totalResults = totalResults;
      return this;
    }

    public Builder totalNewResults(final int totalNewResults) {
      this.totalNewResults = totalNewResults;
      return this;
    }

    public Builder totalFixedResults(final int totalFixedResults) {
      this.totalFixedResults = totalFixedResults;
      return this;
    }

    public Builder createdDate(final ZonedDateTime createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    /** @return new {@code ScanSummary} */
    public ScanSummary build() {
      return new ScanSummary(
          id,
          scanId,
          projectId,
          organizationId,
          duration,
          totalResults,
          totalNewResults,
          totalFixedResults,
          createdDate);
    }
  }
}
