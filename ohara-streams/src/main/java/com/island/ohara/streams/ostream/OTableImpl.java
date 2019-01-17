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

import com.island.ohara.streams.OStream;
import com.island.ohara.streams.OTable;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KTable;

public class OTableImpl<K, V> extends AbstractStream<K, V> implements OTable<K, V> {
  OTableImpl(OStreamBuilder ob, KTable<K, V> ktable, StreamsBuilder builder) {
    super(ob, ktable, builder);
  }

  @Override
  public OStream<K, V> toOStream() {
    return new OStreamImpl<>(builder, ktable.toStream(), innerBuilder);
  }
}
