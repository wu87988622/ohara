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

import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Row;
import com.island.ohara.streams.OGroupedStream;
import com.island.ohara.streams.OStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KGroupedStream;

@SuppressWarnings({"rawtypes", "unchecked"})
public class OGroupedStreamImpl extends AbstractStream<Row, Row> implements OGroupedStream<Row> {

  OGroupedStreamImpl(
      OStreamBuilder ob, KGroupedStream<Row, Row> kgroupstream, StreamsBuilder builder) {
    super(ob, kgroupstream, builder);
  }

  @Override
  public OStream<Row> count() {
    return new OStreamImpl(
        builder,
        kgroupstream
            .count()
            .mapValues(count -> Row.of(Cell.of("count", count)))
            .toStream()
            .map(
                ((key, value) ->
                    KeyValue.pair(
                        key,
                        Row.of(
                            ArrayUtils.addAll(
                                key.cells().toArray(new Cell[0]),
                                value.cells().toArray(new Cell[0])))))),
        innerBuilder);
  }

  @Override
  public OStream<Row> reduce(final Reducer reducer) {
    Reducer.TrueReducer trueReducer = new Reducer.TrueReducer(reducer);
    return new OStreamImpl(
        builder,
        kgroupstream
            .reduce(trueReducer)
            .toStream()
            .map(
                ((key, value) ->
                    KeyValue.pair(
                        key,
                        Row.of(
                            Stream.concat(key.cells().stream(), value.cells().stream())
                                .distinct()
                                .toArray(Cell[]::new))))),
        innerBuilder);
  }
}
