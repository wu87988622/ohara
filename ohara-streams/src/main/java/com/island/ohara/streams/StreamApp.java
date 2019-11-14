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

import com.island.ohara.common.data.Row;
import com.island.ohara.common.exception.ExceptionHandler;
import com.island.ohara.common.exception.OharaException;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.streams.config.StreamDefUtils;
import com.island.ohara.streams.config.StreamDefinitions;
import com.island.ohara.streams.ostream.LaunchImpl;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class StreamApp {

  // Exception handler
  private static ExceptionHandler handler =
      ExceptionHandler.builder()
          .with(IOException.class, OharaException::new)
          .with(MalformedURLException.class, OharaException::new)
          .with(ClassNotFoundException.class, OharaException::new)
          .build();

  /**
   * Running a standalone streamApp. This method is usually called from the main method. It must not
   * be called more than once otherwise exception will be thrown.
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
   */
  public static void runStreamApp(Class<? extends StreamApp> theClass) {
    if (StreamApp.class.isAssignableFrom(theClass))
      ExceptionHandler.DEFAULT.handle(
          () -> {
            LaunchImpl.launchApplication(theClass);
            return null;
          });
    else
      throw new RuntimeException(
          "Error: " + theClass + " is not a subclass of " + StreamApp.class.getName());
  }

  /** Constructor */
  public StreamApp() {}

  /**
   * Use to define settings for streamApp usage. Default will load the required configurations only.
   *
   * <p>Usage:
   *
   * <pre>
   *   public StreamDefinitions config() {
   *       // define your own configs
   *       return StreamDefinitions.create().add(SettingDef.builder().key(key).group(group).build());
   *   }
   * </pre>
   *
   * @return the defined settings
   */
  public StreamDefinitions config() {
    return StreamDefinitions.create();
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
   * @param streamDefinitions configuration object
   */
  public abstract void start(OStream<Row> ostream, StreamDefinitions streamDefinitions);

  /**
   * find main entry of jar in ohara environment container
   *
   * @param lines arguments
   * @throws OharaException exception
   */
  public static void main(String[] lines) throws OharaException {
    Map<String, String> args = CommonUtils.parse(Arrays.asList(lines));
    String className = args.get(StreamDefUtils.CLASS_NAME_DEFINITION.key());
    if (CommonUtils.isEmpty(className))
      throw new RuntimeException(
          "Where is the value of " + StreamDefUtils.CLASS_NAME_DEFINITION.key());
    Class clz = handler.handle(() -> Class.forName(className));
    if (StreamApp.class.isAssignableFrom(clz)) LaunchImpl.launchApplication(clz);
    else
      throw new RuntimeException(
          "Error: " + clz + " is not a subclass of " + StreamApp.class.getName());
  }
}
