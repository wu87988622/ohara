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
import com.island.ohara.streams.ostream.Serdes;

public class SimpleApplicationForExternalEnv extends StreamApp {

  /**
   * This is a simple version of streamApp running in an external environment.
   *
   * @throws Exception
   */
  @Override
  public void start() throws Exception {
    OStream<String, Row> ostream =
        OStream.builder()
            .bootstrapServers("your_broker_list")
            .appid("simple-application-for-external")
            .fromTopicWith("consume_topic_name", Serdes.STRING, Serdes.ROW)
            .toTopic("target_topic_name")
            .build();

    // do nothing but only start streamApp
    ostream.start();
  }
}
