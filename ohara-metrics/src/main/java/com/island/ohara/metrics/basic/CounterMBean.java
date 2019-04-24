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

package com.island.ohara.metrics.basic;

import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.metrics.BeanObject;

public interface CounterMBean {
  String DOMAIN = "com.island.ohara";
  String TYPE_KEY = "type";
  String TYPE_VALUE = "counter";
  /**
   * we have to put the name in properties in order to distinguish the metrics in GUI tool (for
   * example, jmc)
   */
  String GROUP_KEY = "group";
  /**
   * we have to put the name in properties in order to distinguish the metrics in GUI tool (for
   * example, jmc)
   */
  String NAME_KEY = "name";

  /** This is a internal property used to distinguish the counter. */
  String ID_KEY = "id";

  String START_TIME_KEY = "StartTime";
  String VALUE_KEY = "Value";
  String DOCUMENT_KEY = "Document";
  String UNIT_KEY = "Unit";

  static boolean is(BeanObject obj) {
    return obj.domainName().equals(DOMAIN)
        && TYPE_VALUE.equals(obj.properties().get(TYPE_KEY))
        && obj.properties().containsKey(NAME_KEY)
        && obj.properties().containsKey(GROUP_KEY)
        && obj.attributes().containsKey(START_TIME_KEY)
        && obj.attributes().containsKey(VALUE_KEY)
        && obj.attributes().containsKey(DOCUMENT_KEY)
        && obj.attributes().containsKey(UNIT_KEY);
  }

  static CounterMBean of(BeanObject obj) {
    return Counter.builder()
        // NOTED: group is NOT a part of attribute!!!!
        .group(obj.properties().get(GROUP_KEY))
        // NOTED: name is NOT a part of attribute!!!!
        .name(obj.properties().get(NAME_KEY))
        .startTime((long) obj.attributes().get(START_TIME_KEY))
        .value((long) obj.attributes().get(VALUE_KEY))
        .document((String) obj.attributes().get(DOCUMENT_KEY))
        .unit((String) obj.attributes().get(UNIT_KEY))
        .build();
  }

  /**
   * NOTED: this is NOT a part of java beans!!!
   *
   * @return name of this counter
   */
  String group();

  /**
   * NOTED: this is NOT a part of java beans!!!
   *
   * @return name of this counter
   */
  String name();

  /**
   * NOTED: if you are going to change the method name, you have to rewrite the {@link
   * CounterMBean#START_TIME_KEY} also
   *
   * @return the start time of this counter
   */
  long getStartTime();

  /**
   * NOTED: if you are going to change the method name, you have to rewrite the {@link
   * CounterMBean#VALUE_KEY} also
   *
   * @return current value of counter
   */
  long getValue();

  /**
   * NOTED: if you are going to change the method name, you have to rewrite the {@link
   * CounterMBean#UNIT_KEY} also
   *
   * @return the unit of value
   */
  String getUnit();

  /**
   * NOTED: if you are going to change the method name, you have to rewrite the {@link
   * CounterMBean#DOCUMENT_KEY} also
   *
   * @return description of counter
   */
  String getDocument();

  /**
   * A helper method to calculate the average in per second
   *
   * @return value in per sec
   */
  default double valueInPerSec() {
    return (double) (getValue() / 1000) / (double) (CommonUtils.current() - getStartTime());
  }
}
