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

/**
 * The {@code Valuejoiner} interface represents a row-returned joiner function. This function should
 * use to join two different row data into one.
 *
 * @see org.apache.kafka.streams.kstream.ValueJoiner
 */
public interface Valuejoiner {

  Row valuejoiner(final Row value1, final Row value2);

  class TrueValuejoiner implements org.apache.kafka.streams.kstream.ValueJoiner<Row, Row, Row> {

    private final Valuejoiner trueValuejoiner;

    TrueValuejoiner(Valuejoiner valuejoiner) {
      this.trueValuejoiner = valuejoiner;
    }

    @Override
    public Row apply(Row value1, Row value2) {
      return this.trueValuejoiner.valuejoiner(value1, value2);
    }
  }
}
