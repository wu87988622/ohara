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

import com.island.ohara.kafka.exception.CheckedExceptionUtil;
import com.island.ohara.streams.ostream.LaunchImpl;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.FileUtils;

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
   * @param theClass the streamapp class that is constructed and extends from {@link StreamApp}
   * @param params parameters of {@code theClass} constructor used
   */
  public static void runStreamApp(Class<? extends StreamApp> theClass, Object... params) {
    if (StreamApp.class.isAssignableFrom(theClass)) {
      CheckedExceptionUtil.wrap(() -> LaunchImpl.launchApplication(theClass, params));
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
        Class<? extends StreamApp> streamAppClass = theClass;
        CheckedExceptionUtil.wrap(() -> LaunchImpl.launchApplication(streamAppClass));
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
   * User defined initialize stage before running streamapp
   *
   * @throws Exception
   */
  public void init() throws Exception {}

  /**
   * Entry function. <b>Usage:</b>
   *
   * <pre>
   *   OStream.builder()
   *   .fromTopic("topic-name")
   *   ...
   * </pre>
   */
  public abstract void start() throws Exception;

  public void stop() throws Exception {}

  /** for ohara container use */
  public static void main(String[] args) {
    CheckedExceptionUtil.wrap(
        () -> {
          String entryClass = findStreamAppEntry(System.getenv(JAR_URL));

          ClassLoader loader =
              URLClassLoader.newInstance(
                  new URL[] {new URL(System.getenv(JAR_URL))}, StreamApp.class.getClassLoader());
          Class<?> clz = Class.forName(entryClass, true, loader);
          Object obj = clz.newInstance();
          Method method = clz.getMethod("start");
          method.invoke(obj);
        });
  }

  private static String findStreamAppEntry(String jarUrl)
      throws IOException, ClassNotFoundException {
    String jarHeader = "jar:file:";
    String jarTail = "!/";
    File outputFile = File.createTempFile("streamApp-", ".jar");

    URL url = new URL(jarUrl);

    // Download the jar
    FileUtils.copyURLToFile(url, outputFile, CONNECT_TIMEOUT, READ_TIMEOUT);

    // Find the StreamApp entry class name
    JarFile jar = new JarFile(outputFile);
    Enumeration<JarEntry> e = jar.entries();

    URL[] urls = {new URL(jarHeader + outputFile + jarTail)};
    URLClassLoader cl = URLClassLoader.newInstance(urls);

    while (e.hasMoreElements()) {
      JarEntry entry = e.nextElement();
      if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
        String className = entry.getName().replace(".class", "").replaceAll("/", ".");
        Class c = cl.loadClass(className);
        if (StreamApp.class.isAssignableFrom(c)) {
          return c.getName();
        }
      }
    }
    return null;
  }
}
