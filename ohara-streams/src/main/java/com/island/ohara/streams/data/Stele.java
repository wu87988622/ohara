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
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public final class Stele extends Data implements Serializable {
  private final String kind;
  private final String key;
  private final String name;
  private final List<String> from;
  private final List<String> to;

  static {
    ToStringBuilder.setDefaultStyle(ToStringStyle.JSON_STYLE);
  }

  public Stele(String kind, String key, String name, List<String> from, List<String> to) {
    this.kind = kind;
    this.key = key;
    this.name = name;
    this.from = from;
    this.to = to;
  }

  public String getKind() {
    return kind;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public List<String> getFrom() {
    return from;
  }

  public List<String> getTo() {
    return to;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
    //    return "{'kind' : "
    //        + kind
    //        + "\n"
    //        + " 'key' : "
    //        + key
    //        + "\n"
    //        + " 'name' : "
    //        + name
    //        + "\n"
    //        + " 'from' : ["
    //        + String.join(",", from)
    //        + "]\n"
    //        + " 'to' : ["
    //        + String.join(",", to)
    //        + "]\n"
    //        + "}";
  }
}
