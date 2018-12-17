package com.island.ohara.common.util;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the {@link AutoCloseable} and it make sure {@link #close()} is executed
 * only once. Since java disallow interface to have member, this class has got to be a abstract
 * class. Hence, you SHOULD NOT apply this class to the "interface" layer. This class is more
 * suitable to the implementation.
 */
public abstract class ReleaseOnce implements Releasable {
  private static final Logger LOG = LoggerFactory.getLogger(ReleaseOnce.class);
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** @return true if this object have been closed */
  public boolean isClosed() {
    return closed.get();
  }
  /** Do what you want to do when calling closing. */
  protected abstract void doClose();

  @Override
  public final void close() {
    if (closed.compareAndSet(false, true)) doClose();
  }

  /**
   * this helper method close object if it is not null.
   *
   * @param obj releasable object
   */
  public static void close(AutoCloseable obj) {
    close(obj, true);
  }

  /**
   * this helper method close object if it is not null.
   *
   * @param obj releasable object
   * @param swallow true if you don't want to "see" the exception.
   */
  public static void close(AutoCloseable obj, boolean swallow) {
    try {
      if (obj != null) obj.close();
    } catch (Throwable e) {
      if (swallow) LOG.error("Failed to release object", e);
      // TODO: What exception should be thrown here?
      else throw new RuntimeException(e);
    }
  }
}
