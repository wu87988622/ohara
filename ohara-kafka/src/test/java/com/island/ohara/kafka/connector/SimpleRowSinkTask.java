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

package com.island.ohara.kafka.connector;

import static com.island.ohara.kafka.connector.Constants.BROKER;
import static com.island.ohara.kafka.connector.Constants.OUTPUT;

import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import com.island.ohara.kafka.Producer;
import java.util.List;

/** Used for testing. */
public class SimpleRowSinkTask extends RowSinkTask {
  private TaskConfig config = null;
  private String outputTopic = null;
  private Producer<byte[], Row> producer = null;

  @Override
  protected void _start(TaskConfig props) {
    this.config = props;
    outputTopic = config.options().get(OUTPUT);
    producer =
        Producer.builder()
            .connectionProps(config.options().get(BROKER))
            .build(Serializer.BYTES, Serializer.ROW);
  }

  @Override
  protected void _stop() {
    try {
      producer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void _put(List<RowSinkRecord> records) {
    records.forEach(r -> producer.sender().key(r.key()).value(r.row()).send(outputTopic));
  }
}
