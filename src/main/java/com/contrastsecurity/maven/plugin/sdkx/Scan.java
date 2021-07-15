package com.contrastsecurity.maven.plugin.sdkx;

import java.util.Objects;

/** TODO[JG] JAVA-3298 move this to the Contrast Java SDK and flesh it out */
public final class Scan {

  /**
   * static factory that enforces invariants for a waiting scan
   *
   * @param id unique ID of this scan
   * @param projectId unique ID of the Scan project
   * @param organizationId unique Contrast organization ID
   * @return new completed Scan
   */
  public static Scan createWaiting(
      final String id, final String projectId, final String organizationId) {
    return new Scan(id, projectId, organizationId, Status.WAITING, null);
  }

  /**
   * static factory that enforces invariants for a running scan
   *
   * @param id unique ID of this scan
   * @param projectId unique ID of the Scan project
   * @param organizationId unique Contrast organization ID
   * @return new completed Scan
   */
  public static Scan createRunning(
      final String id, final String projectId, final String organizationId) {
    return new Scan(id, projectId, organizationId, Status.RUNNING, null);
  }

  /**
   * static factory that enforces invariants for a completed scan
   *
   * @param id unique ID of this scan
   * @param projectId unique ID of the Scan project
   * @param organizationId unique Contrast organization ID
   * @return new completed Scan
   */
  public static Scan createCompleted(
      final String id, final String projectId, final String organizationId) {
    return new Scan(id, projectId, organizationId, Status.COMPLETED, null);
  }

  /**
   * static factory that enforces invariants for a failed scan
   *
   * @param id unique ID of this scan
   * @param projectId unique ID of the Scan project
   * @param organizationId unique Contrast organization ID
   * @param errorMessage error message returned by the Scan API
   * @return new failed Scan
   */
  public static Scan createFailed(
      final String id,
      final String projectId,
      final String organizationId,
      final String errorMessage) {
    return new Scan(
        id, projectId, organizationId, Status.FAILED, Objects.requireNonNull(errorMessage));
  }

  private final String id;
  private final String projectId;
  private final String organizationId;
  private final Status status;
  private final String errorMessage;

  /** visible for GSON */
  Scan(
      final String id,
      final String projectId,
      final String organizationId,
      final Status status,
      final String errorMessage) {
    this.id = Objects.requireNonNull(id);
    this.projectId = Objects.requireNonNull(projectId);
    this.organizationId = Objects.requireNonNull(organizationId);
    this.status = Objects.requireNonNull(status);
    this.errorMessage = errorMessage;
  }

  /** @return unique ID of this scan */
  public String getId() {
    return id;
  }

  /** @return unique ID of the Scan project that owns this scan */
  public String getProjectId() {
    return projectId;
  }

  /** @return unique ID of the Contrast organization that owns this scan */
  public String getOrganizationId() {
    return organizationId;
  }

  /** @return scan status */
  public Status getStatus() {
    return status;
  }

  /** @return error message for failed scan, or {@code null} if the scan has not failed */
  public String getErrorMessage() {
    return errorMessage;
  }

  public boolean isFinished() {
    return status == Status.FAILED || status == Status.COMPLETED;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Scan scan = (Scan) o;
    return id.equals(scan.id)
        && projectId.equals(scan.projectId)
        && organizationId.equals(scan.organizationId)
        && status == scan.status
        && Objects.equals(errorMessage, scan.errorMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, projectId, organizationId, status, errorMessage);
  }

  public enum Status {
    WAITING,
    RUNNING,
    CANCELLED,
    COMPLETED,
    FAILED
  }
}
