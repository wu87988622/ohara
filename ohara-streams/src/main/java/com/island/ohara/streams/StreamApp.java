/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.streams;

import com.island.ohara.common.annotations.VisibleForTesting;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.exception.ExceptionHandler;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.streams.config.ConfigDef;
import com.island.ohara.streams.ostream.LaunchImpl;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class StreamApp {

  private static final String JAR_URL = "STREAMAPP_JARURL";

  // We set timeout to 30 seconds
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

  /**
   * Running a standalone streamApp. This method is usually called from the main(). It must not be
   * called more than once otherwise exception will be thrown.
   *
   * <p>Usage :
   *
   * <pre>
   *   public static void main(String[] args){
   *     StreamApp.runStreamApp(MyStreamApp.class);
   *   }
   * </pre>
   *
   * @param theClass the streamApp class that is constructed and extends from {@link StreamApp}
   * @param params parameters of {@code theClass} constructor used
   */
  public static void runStreamApp(Class<? extends StreamApp> theClass, Object... params) {
    if (StreamApp.class.isAssignableFrom(theClass))
      ExceptionHandler.DEFAULT.handle(
          () -> {
            LaunchImpl.launchApplication(theClass, params);
            return null;
          });
    else
      throw new RuntimeException(
          "Error: " + theClass + " is not a subclass of " + StreamApp.class.getName());
  }

  /**
   * running a standalone streamApp. This method will try to find each methods in current thread
   * that is extends from {@code StreamApp}
   */
  public static void runStreamApp() {

    String entryClassName = null;
    // Find correct class to call
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();

    boolean found = false;
    for (StackTraceElement se : stack) {
      // Skip entries until we get to the entry for this class
      String className = se.getClassName();
      String methodName = se.getMethodName();
      if (found) {
        entryClassName = className;
        break;
      } else if (StreamApp.class.getName().equals(className) && "runStreamApp".equals(methodName)) {

        found = true;
      }
    }
    if (entryClassName == null) throw new RuntimeException("Unable to find StreamApp class.");

    try {
      Class theClass =
          Class.forName(entryClassName, false, Thread.currentThread().getContextClassLoader());
      if (StreamApp.class.isAssignableFrom(theClass)) LaunchImpl.launchApplication(theClass);
      else
        throw new RuntimeException(
            "Error: " + theClass + " is not a subclass of " + StreamApp.class.getName());
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Constructor */
  public StreamApp() {}

  /**
   * Use to define settings for streamApp usage. Default will load the required configurations only.
   *
   * <p>Usage:
   *
   * <pre>
   *   public ConfigDef config() {
   *       // define your own configs
   *       return ConfigDef.add(SettingDef.builder().key(key).group(group).build());
   *   }
   * </pre>
   *
   * @return the defined settings
   */
  public ConfigDef config() {
    return ConfigDef.DEFAULT;
  }

  /** User defined initialization before running streamApp */
  public void init() {}

  /**
   * Entry function. <b>Usage:</b>
   *
   * <pre>
   *   ostream
   *    .filter()
   *    .map()
   *    ...
   * </pre>
   *
   * @param ostream the entry object to define logic
   * @param configDef configuration object
   */
  public abstract void start(OStream<Row> ostream, ConfigDef configDef);

  /**
   * find main entry of jar in ohara environment container
   *
   * @param args arguments
   */
  public static void main(String[] args) throws Exception {
    if (System.getenv(JAR_URL) == null)
      throw new RuntimeException("It seems you are not running in Ohara Environment?");
    File jarFile = downloadJarByUrl(System.getenv(JAR_URL));
    Map.Entry<String, URLClassLoader> entry = findStreamAppEntry(jarFile);

    if (entry.getKey().isEmpty()) throw new RuntimeException("cannot find any match entry");
    Class clz = Class.forName(entry.getKey(), true, entry.getValue());
    if (StreamApp.class.isAssignableFrom(clz)) {
      if (args != null && args.length > 0) LaunchImpl.launchApplication(clz, (Object[]) args);
      else LaunchImpl.launchApplication(clz);
    } else
      throw new RuntimeException(
          "Error: " + clz + " is not a subclass of " + StreamApp.class.getName());
  }

  @VisibleForTesting
  static File downloadJarByUrl(String jarUrl) throws MalformedURLException {
    return CommonUtils.downloadUrl(new URL(jarUrl), CONNECT_TIMEOUT, READ_TIMEOUT);
  }

  @VisibleForTesting
  static Map.Entry<String, URLClassLoader> findStreamAppEntry(File jarFile)
      throws IOException, ClassNotFoundException {
    String entryClassName = "";

    String jarHeader = "jar:file:";
    String jarTail = "!/";

    // Find the StreamApp entry class name
    JarFile jar = new JarFile(jarFile);
    Enumeration<JarEntry> e = jar.entries();

    URL[] urls = {new URL(jarHeader + jarFile + jarTail)};
    URLClassLoader loader = URLClassLoader.newInstance(urls);

    while (e.hasMoreElements()) {
      JarEntry entry = e.nextElement();
      if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
        String className = entry.getName().replace(".class", "").replaceAll("/", ".");
        Class c = loader.loadClass(className);
        if (StreamApp.class.isAssignableFrom(c)) {
          entryClassName = c.getName();
        }
      }
    }
    return new AbstractMap.SimpleEntry<>(entryClassName, loader);
  }
}
