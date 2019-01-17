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

public interface ForeachAction<K, V> {

  void foreachAction(final K key, final V value);

  class TrueForeachAction<K, V> implements org.apache.kafka.streams.kstream.ForeachAction<K, V> {

    private final ForeachAction<K, V> trueForeachAction;

    TrueForeachAction(ForeachAction<K, V> foreachAction) {
      this.trueForeachAction = foreachAction;
    }

    @Override
    public void apply(K key, V value) {
      this.trueForeachAction.foreachAction(key, value);
    }
  }
}
