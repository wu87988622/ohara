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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import oharastream.ohara.common.data.Cell;
import oharastream.ohara.common.data.Row;
import oharastream.ohara.common.rule.OharaTest;
import oharastream.ohara.common.setting.ConnectorKey;
import oharastream.ohara.common.setting.SettingDef;
import oharastream.ohara.common.setting.TopicKey;
import oharastream.ohara.common.util.CommonUtils;
import oharastream.ohara.kafka.connector.json.ConnectorFormatter;
import org.junit.Assert;
import org.junit.Test;

public class TestRowSourceRecord extends OharaTest {

  @Test(expected = NullPointerException.class)
  public void requireTopic() {
    RowSourceRecord.builder().row(Row.of(Cell.of(CommonUtils.randomString(10), 123))).build();
  }

  @Test(expected = NullPointerException.class)
  public void requireRow() {
    RowSourceRecord.builder().topicKey(TopicKey.of("g", "n")).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullRow() {
    RowSourceRecord.builder().row(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullTopicName() {
    RowSourceRecord.builder().topicKey(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullSourcePartition() {
    RowSourceRecord.builder().sourcePartition(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullSourceOffset() {
    RowSourceRecord.builder().sourcePartition(null);
  }

  @Test
  public void testBuilderWithDefaultValue() {
    Row row = Row.of(Cell.of(CommonUtils.randomString(10), 123));
    TopicKey topic = TopicKey.of("g", "n");

    RowSourceRecord r = RowSourceRecord.builder().topicKey(topic).row(row).build();
    assertEquals(topic, r.topicKey());
    assertEquals(row, r.row());
    assertFalse(r.partition().isPresent());
    assertFalse(r.timestamp().isPresent());
    assertTrue(r.sourceOffset().isEmpty());
    assertTrue(r.sourcePartition().isEmpty());
  }

  @Test
  public void testBuilder() {
    Row row = Row.of(Cell.of(CommonUtils.randomString(10), 123));
    TopicKey topic = TopicKey.of("g", "n");
    long ts = CommonUtils.current();
    int partition = 123;
    Map<String, String> sourceOffset = Collections.singletonMap("abc", "ddd");
    Map<String, String> sourcePartition = Collections.singletonMap("abc", "ddd");

    RowSourceRecord r =
        RowSourceRecord.builder()
            .topicKey(topic)
            .row(row)
            .timestamp(ts)
            .partition(partition)
            .sourceOffset(sourceOffset)
            .sourcePartition(sourcePartition)
            .build();
    assertEquals(topic, r.topicKey());
    assertEquals(row, r.row());
    assertEquals(ts, (long) r.timestamp().get());
    assertEquals(partition, (int) r.partition().get());
    assertEquals(sourceOffset, r.sourceOffset());
    assertEquals(sourcePartition, r.sourcePartition());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void failedToModifySourcePartition() {
    RowSourceRecord.builder()
        .topicKey(TopicKey.of("g", "n"))
        .row(Row.of(Cell.of(CommonUtils.randomString(10), 123)))
        .build()
        .sourceOffset()
        .remove("a");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void failedToModifySourceOffset() {
    RowSourceRecord.builder()
        .topicKey(TopicKey.of("g", "n"))
        .row(Row.of(Cell.of(CommonUtils.randomString(10), 123)))
        .build()
        .sourceOffset()
        .remove("a");
  }

  @Test
  public void testCachedRecords() {
    RowSourceRecord record =
        RowSourceRecord.builder()
            .row(Row.of(Cell.of(CommonUtils.randomString(), CommonUtils.randomString())))
            .topicKey(TopicKey.of("g", "n"))
            .build();
    RowSourceTask task =
        new DumbSourceTask() {
          @Override
          protected List<RowSourceRecord> pollRecords() {
            return Collections.singletonList(record);
          }
        };
    task.start(
        ConnectorFormatter.of()
            .connectorKey(ConnectorKey.of("a", "b"))
            .checkRule(SettingDef.CheckRule.PERMISSIVE)
            .raw());
    Assert.assertEquals(1, task.poll().size());
    Assert.assertEquals(1, task.cachedRecords.size());
    org.apache.kafka.clients.producer.RecordMetadata meta =
        new org.apache.kafka.clients.producer.RecordMetadata(
            new org.apache.kafka.common.TopicPartition(TopicKey.of("g", "n").topicNameOnKafka(), 1),
            1,
            2,
            3,
            4L,
            5,
            6);
    // this loop will remove the elements in the cache so we have to clone another list to prevent
    // ConcurrentModificationException
    new ArrayList<>(task.cachedRecords.keySet()).forEach(r -> task.commitRecord(r, meta));
    Assert.assertEquals(0, task.cachedRecords.size());
  }
}
