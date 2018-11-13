package com.island.ohara.common.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;

public final class CommonUtil {

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

  /** disable to instantiate CommonUtil. */
  private CommonUtil() {}
}
