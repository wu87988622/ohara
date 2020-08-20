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

package oharastream.ohara.kafka;

import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.setting.TopicKey;
import org.apache.kafka.common.TopicPartition;
import org.junit.Assert;
import org.junit.Test;

public class TestRecordMetadata extends OharaTest {

  @Test
  public void testConversion() {
    var topicKey = TopicKey.of("g", "n");
    var partition = 100;
    var timestamp = 11;
    var offset = 5;
    var serializedKeySize = 111;
    var serializedValueSize = 88;
    var record =
        RecordMetadata.of(
            new org.apache.kafka.clients.producer.RecordMetadata(
                new TopicPartition(topicKey.toPlain(), partition),
                0,
                offset,
                timestamp,
                10L,
                serializedKeySize,
                serializedValueSize));
    Assert.assertEquals(topicKey, record.topicKey());
    Assert.assertEquals(partition, record.partition());
    Assert.assertEquals(timestamp, record.timestamp());
    Assert.assertEquals(offset, record.offset());
    Assert.assertEquals(serializedKeySize, record.serializedKeySize());
    Assert.assertEquals(serializedValueSize, record.serializedValueSize());
  }
}
