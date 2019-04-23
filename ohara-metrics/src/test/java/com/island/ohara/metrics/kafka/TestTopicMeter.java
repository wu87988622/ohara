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

package com.island.ohara.metrics.kafka;

import com.island.ohara.metrics.BeanChannel;
import com.island.ohara.testing.WithBrokerWorker;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/** Worker instance will create three topics on broker so we can use it to test our APIs. */
public class TestTopicMeter extends WithBrokerWorker {

  @Test
  public void list() {
    List<TopicMeter> meters = BeanChannel.local().topicMeters();
    Assert.assertFalse(meters.isEmpty());
  }
}
