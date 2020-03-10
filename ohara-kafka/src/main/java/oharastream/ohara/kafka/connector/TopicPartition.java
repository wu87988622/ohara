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

package oharastream.ohara.kafka.connector;

import java.util.Objects;
import oharastream.ohara.common.util.CommonUtils;

/**
 * TopicPartition replaces kafka TopicPartition
 *
 * @see org.apache.kafka.common.TopicPartition
 */
public final class TopicPartition {
  private final String topicName;
  private final int partition;

  public TopicPartition(String topicName, int partition) {
    this.topicName = CommonUtils.requireNonEmpty(topicName);
    this.partition = partition;
  }

  public String topicName() {
    return topicName;
  }

  public int partition() {
    return partition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TopicPartition that = (TopicPartition) o;
    return partition == that.partition && Objects.equals(topicName, that.topicName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(topicName, partition);
  }

  @Override
  public String toString() {
    return "topic=" + topicName + ", partition=" + partition;
  }
}
