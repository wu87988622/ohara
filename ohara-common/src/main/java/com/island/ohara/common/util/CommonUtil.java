package com.island.ohara.common.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommonUtil {
  private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

  /** a interface used to represent current time. */
  @FunctionalInterface
  public interface Timer {
    /** @return current time in ms. */
    long current();
  }

  /** Wrap to {@link System#currentTimeMillis} */
  private static final Timer DEFAULT_TIMER = System::currentTimeMillis;

  private static volatile Timer TIMER = DEFAULT_TIMER;

  public static void inject(Timer newOne) {
    TIMER = newOne;
  }

  public static void reset() {
    TIMER = DEFAULT_TIMER;
  }

  public static long current() {
    return TIMER.current();
  }

  /**
   * create a uuid.
   *
   * @return uuid
   */
  public static String uuid() {
    return java.util.UUID.randomUUID().toString();
  }

  /**
   * Determines the IP address of a host, given the host's name.
   *
   * @param hostname host's name
   * @return the IP address string in textual presentation.
   */
  public static String address(String hostname) {
    try {
      return InetAddress.getByName(hostname).getHostAddress();
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String hostname() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String anyLocalAddress() {
    return "0.0.0.0";
  }

  public static String timezone() {
    return Calendar.getInstance().getTimeZone().getID();
  }

  /**
   * compose a full path based on parent (folder) and name (file).
   *
   * @param parent parent folder
   * @param name file name
   * @return path
   */
  public static String path(String parent, String name) {
    if (parent.endsWith("/")) return parent + name;
    else return parent + "/" + name;
  }

  /**
   * extract the file name from the path
   *
   * @param path path
   * @return name
   */
  public static String name(String path) {
    if (path.equalsIgnoreCase("/")) throw new IllegalArgumentException("no file name for " + path);
    else {
      int last = path.lastIndexOf("/");
      if (last == -1) return path;
      else return path.substring(last + 1);
    }
  }

  /**
   * replace the path's parent path by new parent
   *
   * @param parent new parent
   * @param path original path
   * @return new path
   */
  public static String replaceParent(String parent, String path) {
    return path(parent, name(path));
  }

  /**
   * helper method. Loop the specified method until timeout or get true from method
   *
   * @param f function
   * @param d duration
   * @return false if timeout and (useException = true). Otherwise, the return value is true
   */
  public static Boolean await(Supplier<Boolean> f, Duration d) {
    return await(f, d, Duration.ofMillis(5000), true);
  }

  /**
   * helper method. Loop the specified method until timeout or get true from method
   *
   * @param f function
   * @param d duration
   * @param freq frequency to call the method
   * @param useException true make this method throw exception after timeout.
   * @return false if timeout and (useException = true). Otherwise, the return value is true
   */
  public static Boolean await(
      Supplier<Boolean> f, Duration d, Duration freq, Boolean useException) {
    long startTs = current();
    long runningTime = System.currentTimeMillis() - startTs;
    while (d.toMillis() >= runningTime) {
      if (f.get()) return true;
      else {
        try {
          TimeUnit.MILLISECONDS.sleep(freq.toMillis());
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
    if (useException) {
      logger.error(
          "Running test method time is "
              + runningTime
              + " seconds more than the timeout time "
              + d.getSeconds()
              + " seconds. Please turning your timeout time.");
      throw new IllegalStateException("timeout");
    } else return false;
  }

  /** disable to instantiate CommonUtil. */
  private CommonUtil() {}
}
