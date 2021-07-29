package com.contrastsecurity.maven.plugin.sdkx;

/*-
 * #%L
 * Contrast Maven Plugin
 * %%
 * Copyright (C) 2021 Contrast Security, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Objects;

/** TODO[JG] JAVA-3298 move this to the Contrast Java SDK and flesh it out */
public final class Scan {

  /**
   * static factory that enforces invariants for a waiting scan
   *
   * @param id unique ID of this scan
   * @param projectId unique ID of the Scan project
   * @param organizationId unique Contrast organization ID
   * @return new waiting {@code Scan}
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
   * @return new running {@code Scan}
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
   * @return new completed {@code Scan}
   */
  public static Scan createCompleted(
      final String id, final String projectId, final String organizationId) {
    return new Scan(id, projectId, organizationId, Status.COMPLETED, null);
  }

  /**
   * static factory that enforces invariants for a canceled scan
   *
   * @param id unique ID of this scan
   * @param projectId unique ID of the Scan project
   * @param organizationId unique Contrast organization ID
   * @return new canceled {@code Scan}
   */
  public static Scan createCanceled(
      final String id, final String projectId, final String organizationId) {
    return new Scan(id, projectId, organizationId, Status.CANCELLED, null);
  }

  /**
   * static factory that enforces invariants for a failed scan
   *
   * @param id unique ID of this scan
   * @param projectId unique ID of the Scan project
   * @param organizationId unique Contrast organization ID
   * @param errorMessage error message returned by the Scan API
   * @return new failed {@code Scan}
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

  /** @return true when the scan has completed, failed, or been canceled */
  public boolean isFinished() {
    return status == Status.FAILED || status == Status.COMPLETED || status == Status.CANCELLED;
  }

  /**
   * Factory that creates a new {@code Scan} with the same properties as this one albeit in the
   * "running" state
   *
   * @return new {@code Scan} in the running state
   * @throws IllegalStateException when this is not in the "waiting" state
   */
  public Scan toRunning() {
    if (status != Status.WAITING) {
      throw new IllegalStateException("Only a waiting scan can transition to a running state");
    }
    return createRunning(id, projectId, organizationId);
  }

  /**
   * Factory that creates a new {@code Scan} with the same properties as this one albeit in the
   * "completed" state
   *
   * @return new {@code Scan} in the running state
   * @throws IllegalStateException when this is not in the "running" state
   */
  public Scan toCompleted() {
    if (status != Status.RUNNING) {
      throw new IllegalStateException("Only a running scan can transition to a completed state");
    }
    return createCompleted(id, projectId, organizationId);
  }

  /**
   * Factory that creates a new {@code Scan} with the same properties as this one albeit in the
   * "failed" state
   *
   * @param errorMessage error message returned by the Scan API
   * @return new {@code Scan} in the running state
   * @throws IllegalStateException when this is not in the "running" state
   */
  public Scan toFailed(final String errorMessage) {
    if (status != Status.RUNNING) {
      throw new IllegalStateException("Only a running scan can transition to a failed state");
    }
    return createFailed(id, projectId, organizationId, errorMessage);
  }

  /**
   * Factory that creates a new {@code Scan} with the same properties as this one albeit in the
   * "canceled" state
   *
   * @return new {@code Scan} in the running state
   * @throws IllegalStateException when this is not in the "running" state
   */
  public Scan toCanceled() {
    if (status != Status.RUNNING) {
      throw new IllegalStateException("Only a running scan can transition to a failed state");
    }
    return createCanceled(id, projectId, organizationId);
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

  /** Describes the possible states that a scan can have */
  public enum Status {
    WAITING,
    RUNNING,
    CANCELLED,
    COMPLETED,
    FAILED
  }
}
