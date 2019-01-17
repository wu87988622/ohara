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

package com.island.ohara.streams.ostream;

import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import java.util.Map;

public class RowDeserializer implements org.apache.kafka.common.serialization.Deserializer<Row> {

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {}

  @Override
  public Row deserialize(String topic, byte[] data) {
    if (data == null) return null;
    else return Serializer.ROW.from(data);
  }

  @Override
  public void close() {}
}
