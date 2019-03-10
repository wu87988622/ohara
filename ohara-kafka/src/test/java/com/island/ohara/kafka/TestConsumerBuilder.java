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

package com.island.ohara.kafka;

import com.island.ohara.common.rule.MediumTest;
import java.util.Collections;
import org.junit.Test;

public class TestConsumerBuilder extends MediumTest {

  @Test(expected = NullPointerException.class)
  public void nullGroupId() {
    Consumer.builder().groupId(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullTopicName() {
    Consumer.builder().topicName(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullTopicNames() {
    Consumer.builder().topicNames(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyTopicNames() {
    Consumer.builder().topicNames(Collections.emptyList());
  }

  @Test(expected = NullPointerException.class)
  public void nullConnectionProps() {
    Consumer.builder().connectionProps(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyConnectionProps() {
    Consumer.builder().connectionProps("");
  }

  @Test(expected = NullPointerException.class)
  public void nullKeySerializer() {
    Consumer.builder().keySerializer(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullValueSerializer() {
    Consumer.builder().valueSerializer(null);
  }
}
