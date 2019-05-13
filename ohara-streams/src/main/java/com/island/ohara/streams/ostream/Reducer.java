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
 * The {@code Reducer} interface for combining two {@code Row} values of the same type.
 *
 * @see org.apache.kafka.streams.kstream.Reducer
 */
public interface Reducer {

  Row reducer(Row value1, Row value2);

  class TrueReducer implements org.apache.kafka.streams.kstream.Reducer<Row> {

    private final Reducer trueReducer;

    TrueReducer(Reducer reducer) {
      this.trueReducer = reducer;
    }

    @Override
    public Row apply(Row value1, Row value2) {
      return this.trueReducer.reducer(value1, value2);
    }
  }
}
