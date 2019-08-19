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

package com.island.ohara.streams.metric;

import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.metrics.basic.Counter;
import com.island.ohara.streams.config.StreamDefUtils;

/** This is a helper class to Get the desire bean object */
public final class MetricFactory {

  /**
   * Get counter beans.
   *
   * @param type the {@code IOType}
   * @return counter bean
   */
  public static Counter getCounter(IOType type) {
    return Counter.builder()
        // the group is individual for each streamApp
        // so it is OK to use same group value
        .group(StreamDefUtils.STREAMAPP_METRIC_GROUP_DEFINITION.defaultValue())
        .name(type.name())
        .unit("row")
        .document(type.value + ": the number of rows")
        .startTime(CommonUtils.current())
        .value(0)
        .register();
  }

  /**
   * We support two different IOType :
   *
   * <p>TOPIC_IN (the consume topic) and TOPIC_OUT (the produce topic)
   */
  public enum IOType {
    TOPIC_IN("Input"),
    TOPIC_OUT("Output");

    private final String value;

    IOType(String s) {
      value = s;
    }
  }

  // prevent construction
  private MetricFactory() {
    throw new AssertionError();
  }
}
