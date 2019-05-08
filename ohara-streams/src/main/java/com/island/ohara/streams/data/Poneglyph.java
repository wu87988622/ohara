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

package com.island.ohara.streams.data;

import com.island.ohara.common.data.Data;
import java.io.Serializable;
import java.util.HashSet;

/**
 * The {@code Poneglyph} represents the overall logic flow in this streamApp. User can recognize the
 * from-and-to view in each operation {@link Stele}. A {@code Poneglyph} is represent a complete
 * flow from data in to data out, there may have multiple {@code Poneglyph} in a streamApp. We
 * extend this class from {@link Data} to use the JSON string function.
 */
public final class Poneglyph extends Data implements Serializable {
  private static final long serialVersionUID = 1L;
  private final HashSet<Stele> steles = new HashSet<>();

  public void addStele(Stele another) {
    this.steles.add(another);
  }

  public HashSet<Stele> getSteles() {
    return this.steles;
  }
}
