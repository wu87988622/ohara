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
import com.island.ohara.metrics.BeanChannel;
import com.island.ohara.metrics.BeanObject;

import java.util.HashMap;
import java.util.Map;

public interface CounterMBean {
  String DOMAIN = "com.island.ohara";
  String TYPE_KEY = "type";
  String TYPE_VALUE = "counter";
  /**
   * we have to put the name in properties in order to distinuish the metrics in GUI tool (for example, jmc)
   */
  String NAME_KEY = "name";
  String START_TIME_KEY = "StartTime";
  String VALUE_KEY = "Value";
  String DOCUMENT_KEY = "Document";

  /**
   * create and register a mutable CounterMBean.
   * @param name the name of counter
   * @return Counter
   */
  static Counter register(String name) {
    Map<String, String> properties = new HashMap<>();
    properties.put(TYPE_KEY, TYPE_VALUE);
    properties.put(NAME_KEY, CommonUtils.requireNonEmpty(name));
    return BeanChannel.<Counter>register()
      .domain(DOMAIN)
      .properties(properties)
      .beanObject(Counter.builder()
        .name(name)
        .value(0)
        .startTime(CommonUtils.current())
        .build())
      .run();
  }

  static boolean is(BeanObject obj) {
    return obj.domainName().equals(DOMAIN)
      && TYPE_VALUE.equals(obj.properties().get(TYPE_KEY))
      && obj.properties().containsKey(NAME_KEY)
      && obj.attributes().containsKey(START_TIME_KEY)
      && obj.attributes().containsKey(VALUE_KEY);
  }

  static CounterMBean of(BeanObject obj) {
    return Counter.builder()
      .name(obj.properties().get(NAME_KEY))
      .startTime((long) obj.attributes().get(START_TIME_KEY))
      .value((long) obj.attributes().get(VALUE_KEY))
      .build();
  }

  /**
   * @return name of this counter
   */
  String name();

  /**
   * NOTED: if you are going to change the method name, you have to rewrite the {@link CounterMBean#START_TIME_KEY} also
   * @return the statt time of this counter
   */
  long getStartTime();

  /**
   * NOTED: if you are going to change the method name, you have to rewrite the {@link CounterMBean#VALUE_KEY} also
   * @return current value of counter
   */
  long getValue();

  /**
   * NOTED: if you are going to change the method name, you have to rewrite the {@link CounterMBean#DOCUMENT_KEY} also
   * @return description of counter
   */
  String getDocument();
}
