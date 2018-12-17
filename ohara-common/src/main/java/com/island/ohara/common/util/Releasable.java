package com.island.ohara.common.util;

public interface Releasable extends AutoCloseable {
  /** ohara doesn't use checked exception. */
  @Override
  void close();
}
