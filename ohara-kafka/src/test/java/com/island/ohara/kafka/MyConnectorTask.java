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

package com.island.ohara.kafka;

import com.island.ohara.kafka.connector.RowSourceRecord;
import com.island.ohara.kafka.connector.RowSourceTask;
import com.island.ohara.kafka.connector.TaskConfig;
import java.util.Collections;
import java.util.List;

public class MyConnectorTask extends RowSourceTask {
  private long lastSent = 0;
  private String topicName;

  @Override
  protected void _start(TaskConfig config) {
    this.topicName = config.topics().get(0);
  }

  @Override
  protected void _stop() {}

  @Override
  protected List<RowSourceRecord> _poll() {
    long current = System.currentTimeMillis();
    if (current - lastSent >= 1000) {
      lastSent = current;
      return Collections.singletonList(RowSourceRecord.of(topicName, TestConnectorClient.ROW));
    } else return Collections.emptyList();
  }
}
