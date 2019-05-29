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

package com.island.ohara.kafka.connector.text.csv;

import com.island.ohara.common.data.Pair;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class StreamUtils {

  // Suppress default constructor for noninstantiability
  private StreamUtils() {
    throw new AssertionError();
  }

  /** Converts an {@link java.util.Iterator} to {@link java.util.stream.Stream}. */
  public static <T> Stream<T> iterate(Iterator<? extends T> iterator) {
    int characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE;
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(iterator, characteristics), false);
  }

  /** Zips the specified stream with its indices. */
  public static <T> Stream<Pair<Integer, T>> zipWithIndex(Stream<? extends T> stream) {
    return iterate(
        new Iterator<Pair<Integer, T>>() {
          private final Iterator<? extends T> streamIterator = stream.iterator();
          private int index = 0;

          @Override
          public boolean hasNext() {
            return streamIterator.hasNext();
          }

          @Override
          public Pair<Integer, T> next() {
            return Pair.of(index++, streamIterator.next());
          }
        });
  }

  /**
   * Returns a stream consisting of the results of applying the given two-arguments function to the
   * elements of this stream. The first argument of the function is the element index and the second
   * one - the element value.
   */
  public static <T, R> Stream<R> mapWithIndex(
      Stream<? extends T> stream, BiFunction<Integer, ? super T, ? extends R> mapper) {
    return zipWithIndex(stream).map(pair -> mapper.apply(pair.left(), pair.right()));
  }
}
