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

package oharastream.ohara.common.data;

import java.io.Serializable;
import java.util.Objects;

/**
 * A pair data of left and right elements. Also represent a {@code Tuple} of two elements. This
 * class is an implementation and define a entry of immutable pair class.
 *
 * @param <L> the left element type
 * @param <R> the right element type
 */
public final class Pair<L, R> implements Serializable {

  // Serialization version
  private static final long serialVersionUID = 1L;

  private final L left;
  private final R right;

  private Pair(final L left, final R right) {
    this.left = Objects.requireNonNull(left);
    this.right = Objects.requireNonNull(right);
  }

  /**
   * Create a immutable pair with specify elements. Note: the elements should not be {@code null}
   *
   * @param left left element
   * @param right right element
   * @param <L> left element type
   * @param <R> right element type
   * @return a pair
   */
  public static <L, R> Pair<L, R> of(final L left, final R right) {
    return new Pair<>(left, right);
  }

  /**
   * get the left element from this pair
   *
   * @return left element
   */
  public L left() {
    return left;
  }

  /**
   * get the right element from this pair
   *
   * @return right element
   */
  public R right() {
    return right;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other instanceof Pair) {
      Pair<?, ?> otherPair = (Pair<?, ?>) other;
      return left().equals(otherPair.left()) && right().equals(otherPair.right());
    } else return false;
  }

  @Override
  public int hashCode() {
    return -31 * left().hashCode() ^ 30 * right().hashCode();
  }

  @Override
  public String toString() {
    return "Pair: (" + left() + ", " + right() + ")";
  }
}
