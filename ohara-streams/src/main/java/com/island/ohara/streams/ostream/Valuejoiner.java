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

public interface Valuejoiner<V1, V2, VR> {
  VR valuejoiner(final V1 value1, final V2 value2);

  class TrueValuejoiner<V1, V2, VR>
      implements org.apache.kafka.streams.kstream.ValueJoiner<V1, V2, VR> {

    private final Valuejoiner<V1, V2, VR> trueValuejoiner;

    TrueValuejoiner(Valuejoiner<V1, V2, VR> valuejoiner) {
      this.trueValuejoiner = valuejoiner;
    }

    @Override
    public VR apply(V1 value1, V2 value2) {
      return this.trueValuejoiner.valuejoiner(value1, value2);
    }
  }
}
