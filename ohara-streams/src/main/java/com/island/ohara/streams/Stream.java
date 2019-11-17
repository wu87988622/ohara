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
import com.island.ohara.common.setting.TopicKey;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.streams.config.StreamDefUtils;
import com.island.ohara.streams.config.StreamDefinitions;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class Stream {
  // Exception handler
  private static ExceptionHandler handler =
      ExceptionHandler.builder()
          .with(IOException.class, OharaException::new)
          .with(MalformedURLException.class, OharaException::new)
          .with(ClassNotFoundException.class, OharaException::new)
          .build();

  /**
   * Running a standalone stream. This method is usually called from the main method. It must not be
   * called more than once otherwise exception will be thrown.
   *
   * <p>Usage :
   *
   * <pre>
   *   public static void main(String[] args){
   *     Stream.execute(MyStream.class);
   *   }
   * </pre>
   *
   * @param clz the stream class that is constructed and extends from {@link Stream}
   */
  public static void execute(Class<? extends Stream> clz) {
    ExceptionHandler.DEFAULT.handle(
        () -> {
          Constructor<? extends Stream> cons = clz.getConstructor();
          final Stream theApp = cons.newInstance();
          StreamDefinitions streamDefinitions = theApp.config();

          OStream<Row> ostream =
              OStream.builder()
                  .appId(streamDefinitions.name())
                  .bootstrapServers(streamDefinitions.brokerConnectionProps())
                  // TODO: Currently, the number of from topics must be 1
                  // https://github.com/oharastream/ohara/issues/688
                  .fromTopic(
                      streamDefinitions.fromTopicKeys().stream()
                          .map(TopicKey::topicNameOnKafka)
                          .findFirst()
                          .orElse(null))
                  // TODO: Currently, the number of to topics must be 1
                  // https://github.com/oharastream/ohara/issues/688
                  .toTopic(
                      streamDefinitions.toTopicKeys().stream()
                          .map(TopicKey::topicNameOnKafka)
                          .findFirst()
                          .orElse(null))
                  .build();
          theApp.init();
          theApp.start(ostream, streamDefinitions);
          return null;
        });
  }

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
    if (Stream.class.isAssignableFrom(clz)) execute(clz);
    else
      throw new RuntimeException(
          "Error: " + clz + " is not a subclass of " + Stream.class.getName());
  }

  // ------------------------[public APIs]------------------------//

  /** Constructor */
  public Stream() {}

  /**
   * Use to define settings for stream usage. Default will load the required configurations only.
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

  /** User defined initialization before running stream */
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
}