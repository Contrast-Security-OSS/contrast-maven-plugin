package com.contrastsecurity.maven.plugin.sdkx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describes a page of results from the Scan API.
 *
 * <p>This class is package-private, because we are not yet supporting paging through results, so
 * this remains an implementation detail of this package. We may add support ofr paging through scan
 * results in a future version of the Contrast Java SDK.
 *
 * @param <T> the type of item in the page
 */
final class ScanPagedResult<T> {

  private final List<T> content;
  private final int totalElements;

  ScanPagedResult(final List<T> content, final int totalElements) {
    this.content = Collections.unmodifiableList(new ArrayList<>(content));
    this.totalElements = totalElements;
  }

  List<T> getContent() {
    return content;
  }

  int getTotalElements() {
    return totalElements;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ScanPagedResult<?> that = (ScanPagedResult<?>) o;
    return totalElements == that.totalElements && content.equals(that.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, totalElements);
  }

  @Override
  public String toString() {
    return "ScanPagedResult{" + "content=" + content + ", totalElements=" + totalElements + '}';
  }
}
