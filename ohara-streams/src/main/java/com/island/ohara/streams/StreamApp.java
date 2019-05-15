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
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.kafka.exception.CheckedExceptionUtils;
import com.island.ohara.streams.ostream.LaunchImpl;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class StreamApp {

  private static final String JAR_URL = "STREAMAPP_JARURL";
  private static final int CONNECT_TIMEOUT = 30 * 1000;
  private static final int READ_TIMEOUT = 30 * 1000;

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
    if (StreamApp.class.isAssignableFrom(theClass)) {
      CheckedExceptionUtils.wrap(() -> LaunchImpl.launchApplication(theClass, params));
    } else {
      throw new RuntimeException(
          "Error: " + theClass + " is not a subclass of " + StreamApp.class.getName());
    }
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
    if (entryClassName == null) {
      throw new RuntimeException("Unable to find StreamApp class.");
    }

    try {
      Class theClass =
          Class.forName(entryClassName, false, Thread.currentThread().getContextClassLoader());
      if (StreamApp.class.isAssignableFrom(theClass)) {
        CheckedExceptionUtils.wrap(() -> LaunchImpl.launchApplication(theClass));
      } else {
        throw new RuntimeException(
            "Error: " + theClass + " is not a subclass of " + StreamApp.class.getName());
      }
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Constructor */
  public StreamApp() {}

  /**
   * User defined initialize stage before running streamApp
   *
   * @throws Exception initial Exception
   */
  public void init() throws Exception {}

  /**
   * Entry function. <b>Usage:</b>
   *
   * <pre>
   *   OStream.builder().toOharaEnvStream();
   *    .filter()
   *    .map()
   *    ...
   * </pre>
   *
   * @throws Exception start Exception
   */
  public abstract void start() throws Exception;

  /**
   * find main entry of jar in ohara environment container
   *
   * @param args arguments
   */
  public static void main(String[] args) {
    CheckedExceptionUtils.wrap(
        () -> {
          if (System.getenv(JAR_URL) == null) {
            throw new RuntimeException("It seems you are not running in Ohara Environment?");
          }
          File jarFile = downloadJarByUrl(System.getenv(JAR_URL));
          Map.Entry<String, URLClassLoader> entry = findStreamAppEntry(jarFile);

          if (entry.getKey().isEmpty()) {
            throw new RuntimeException("cannot find any match entry");
          }
          Class clz = Class.forName(entry.getKey(), true, entry.getValue());
          if (StreamApp.class.isAssignableFrom(clz)) {
            if (args != null && args.length > 0) {
              CheckedExceptionUtils.wrap(
                  () -> LaunchImpl.launchApplication(clz, Arrays.asList(args)));
            } else {
              CheckedExceptionUtils.wrap(() -> LaunchImpl.launchApplication(clz));
            }
          } else {
            throw new RuntimeException(
                "Error: " + clz + " is not a subclass of " + StreamApp.class.getName());
          }
        });
  }

  @VisibleForTesting
  static File downloadJarByUrl(String jarUrl) throws MalformedURLException {
    // create a tempFolder and new a file instance : /tmp/streamApp-XXXXX/streamApp.jar
    File tmpFolder = CommonUtils.createTempFolder("streamApp-");
    File outputFile = new File(tmpFolder, "streamApp.jar");

    URL url = new URL(jarUrl);

    // Download the jar
    CommonUtils.copyURLToFile(url, outputFile, CONNECT_TIMEOUT, READ_TIMEOUT);
    return outputFile;
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
