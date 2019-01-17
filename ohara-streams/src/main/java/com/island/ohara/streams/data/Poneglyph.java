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

import java.io.Serializable;
import java.util.HashSet;

@SuppressWarnings({"rawtypes", "serial"})
public final class Poneglyph implements Serializable {
  private final HashSet<Stele> steles = new HashSet<>();

  public void addStele(Stele another) {
    this.steles.add(another);
  }

  public HashSet<Stele> getSteles() {
    return this.steles;
  }

  @Override
  public String toString() {
    return String.format("steles : %s", getSteles());
  }
}
