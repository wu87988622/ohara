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

import java.util.Set;
import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.setting.TopicKey;
import oharastream.ohara.kafka.connector.TopicPartition;
import org.junit.Test;

public class TestConsumerBuilder extends OharaTest {

  @Test(expected = NullPointerException.class)
  public void nullGroupId() {
    Consumer.builder().groupId(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullTopicKey() {
    Consumer.builder().topicKey(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullTopicKeys() {
    Consumer.builder().topicKeys(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyTopicKeys() {
    Consumer.builder().topicKeys(Set.of());
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

  @Test(expected = IllegalArgumentException.class)
  public void emptyAssignments() {
    Consumer.builder().assignments(Set.of());
  }

  @Test(expected = NullPointerException.class)
  public void nullAssignments() {
    Consumer.builder().assignments(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void assignBothTopicAndAssignments() {
    Consumer.builder()
        .assignments(Set.of(new TopicPartition(TopicKey.of("g", "n"), 1)))
        .topicKey(TopicKey.of("a", "b"));
  }
}
