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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import oharastream.ohara.common.annotations.VisibleForTesting;
import oharastream.ohara.common.data.Column;
import oharastream.ohara.common.data.Serializer;
import oharastream.ohara.common.setting.ObjectKey;
import oharastream.ohara.common.setting.SettingDef;
import oharastream.ohara.common.setting.TopicKey;
import oharastream.ohara.common.util.CommonUtils;
import oharastream.ohara.common.util.Releasable;
import oharastream.ohara.common.util.VersionUtils;
import oharastream.ohara.kafka.Header;
import oharastream.ohara.kafka.RecordMetadata;
import oharastream.ohara.metrics.basic.Counter;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.source.SourceTaskContext;

/** A wrap to SourceTask. The users should substitute RowSourceRecord for SourceRecord. */
public abstract class RowSourceTask extends SourceTask {

  /**
   * Start the Task. This should handle any configuration parsing and one-time setup from the task.
   *
   * @param settings initial configuration
   */
  protected abstract void run(TaskSetting settings);

  /**
   * Signal this SourceTask to stop. In SourceTasks, this method only needs to signal to the task
   * that it should stop trying to poll for new data and interrupt any outstanding poll() requests.
   * It is not required that the task has fully stopped. Note that this method necessarily may be
   * invoked from a different thread than pollRecords() and commitOffsets()
   */
  protected abstract void terminate();
  /**
   * Poll this SourceTask for new records. This method should block if no data is currently
   * available.
   *
   * @return a array from RowSourceRecord
   */
  protected abstract List<RowSourceRecord> pollRecords();

  /**
   * Commit an individual RowSourceRecord when the callback from the producer client is received, or
   * if a record is filtered by a transformation. SourceTasks are not required to implement this
   * functionality; Kafka Connect will record offsets automatically. This hook is provided for
   * systems that also need to store offsets internally in their own system.
   *
   * @param record RowSourceRecord that was successfully sent via the producer.
   * @param metadata the metadata of committed data
   */
  protected void commitRecord(RowSourceRecord record, RecordMetadata metadata) {
    // do nothing
  }

  /**
   * Commit the offsets, up to the offsets that have been returned by pollRecords(). This method
   * should block until the commit is complete.
   *
   * <p>SourceTasks are not required to implement this functionality; Kafka Connect will record
   * offsets automatically. This hook is provided for systems that also need to store offsets
   * internally in their own system.
   */
  protected void commitOffsets() {
    // do nothing
  }

  /**
   * RowSourceContext is provided to RowSourceTask to allow them to interact with the underlying
   * runtime.
   */
  protected RowSourceContext rowContext = null;
  // -------------------------------------------------[WRAPPED]-------------------------------------------------//
  @VisibleForTesting Counter messageNumberCounter = null;
  @VisibleForTesting Counter messageSizeCounter = null;
  @VisibleForTesting Counter ignoredMessageNumberCounter = null;
  @VisibleForTesting Counter ignoredMessageSizeCounter = null;
  @VisibleForTesting TaskSetting taskSetting = null;
  /**
   * this value should be immutable after starting this connector task. It is used to generate kafka
   * records and the serialization of jackson is expensive so we cache it.
   */
  @VisibleForTesting byte[] keyInBytes = null;

  @VisibleForTesting
  byte[] classNameInBytes = getClass().getName().getBytes(StandardCharsets.UTF_8);

  /**
   * a helper method used to handle the fucking null produced by kafka...
   *
   * @return kafka's source
   */
  private SourceRecord toKafka(RowSourceRecord record) {
    ConnectHeaders headers = new ConnectHeaders();
    // add the header to mark the source of this data
    // we convert the string to bytes manually since we don't want to use the schema in order to
    // make this header is readable to consumer.
    headers.addBytes(Header.SOURCE_CLASS_KEY, classNameInBytes);
    headers.addBytes(Header.SOURCE_KEY_KEY, keyInBytes);
    return new SourceRecord(
        record.sourcePartition(),
        record.sourceOffset(),
        record.topicKey().topicNameOnKafka(),
        record.partition().orElse(null),
        Schema.BYTES_SCHEMA,
        Serializer.ROW.to(record.row()),
        // TODO: we keep empty value in order to reduce data size in transmission
        Schema.BYTES_SCHEMA,
        null,
        record.timestamp().orElse(null),
        headers);
  }

  /** the conversion is too expensive so we keep this mapping. */
  @VisibleForTesting final Map<SourceRecord, RowSourceRecord> cachedRecords = new HashMap<>();

  @Override
  public final List<SourceRecord> poll() {
    List<RowSourceRecord> records = pollRecords();
    // kafka connector doesn't support the empty list in testing. see
    // https://github.com/apache/kafka/pull/4958
    if (CommonUtils.isEmpty(records)) return null;

    SettingDef.CheckRule rule = taskSetting.checkRule();
    List<Column> columns = taskSetting.columns();
    List<SourceRecord> raw =
        records.stream()
            .map(record -> Map.entry(record, toKafka(record)))
            .filter(
                pair -> {
                  cachedRecords.put(pair.getValue(), pair.getKey());
                  long rowSize = ConnectorUtils.sizeOf(pair.getValue());
                  boolean pass =
                      ConnectorUtils.match(
                          rule,
                          pair.getKey().row(),
                          rowSize,
                          columns,
                          false,
                          ignoredMessageNumberCounter,
                          ignoredMessageSizeCounter);
                  if (pass && messageSizeCounter != null) messageSizeCounter.addAndGet(rowSize);
                  return pass;
                })
            .map(Map.Entry::getValue)
            .collect(Collectors.toUnmodifiableList());
    if (messageNumberCounter != null) messageNumberCounter.addAndGet(raw.size());
    return raw;
  }

  /**
   * create counter builder. This is a helper method for custom connector which want to expose some
   * number via ohara's metrics. NOTED: THIS METHOD MUST BE USED AFTER STARTING THIS CONNECTOR.
   * otherwise, an IllegalArgumentException will be thrown.
   *
   * @return counter
   */
  protected CounterBuilder counterBuilder() {
    if (taskSetting == null)
      throw new IllegalArgumentException("you can't create a counter before starting connector");
    return CounterBuilder.of().key(taskSetting.connectorKey());
  }

  @Override
  public final void start(Map<String, String> props) {
    taskSetting = TaskSetting.of(Collections.unmodifiableMap(props));
    messageNumberCounter = ConnectorUtils.messageNumberCounter(taskSetting.connectorKey());
    messageSizeCounter = ConnectorUtils.messageSizeCounter(taskSetting.connectorKey());
    ignoredMessageNumberCounter =
        ConnectorUtils.ignoredMessageNumberCounter(taskSetting.connectorKey());
    ignoredMessageSizeCounter =
        ConnectorUtils.ignoredMessageSizeCounter(taskSetting.connectorKey());
    keyInBytes =
        ObjectKey.toJsonString(taskSetting.connectorKey()).getBytes(StandardCharsets.UTF_8);
    run(taskSetting);
  }

  @Override
  public final void stop() {
    try {
      terminate();
    } finally {
      Releasable.close(messageNumberCounter);
      Releasable.close(messageSizeCounter);
      Releasable.close(ignoredMessageNumberCounter);
      Releasable.close(ignoredMessageSizeCounter);
    }
  }

  @Override
  public final void commit() {
    commitOffsets();
  }

  // TODO: We do a extra conversion here (bytes => Row)... by chia
  @Override
  public final void commitRecord(
      SourceRecord record, org.apache.kafka.clients.producer.RecordMetadata metadata) {
    RowSourceRecord r = cachedRecords.remove(record);
    // It is impossible to observer the null since we cache all records in #poll method.
    // However, we all hate the null so the workaround is to create a new record :(
    if (r == null) {
      RowSourceRecord.Builder builder = RowSourceRecord.builder();
      builder.topicKey(TopicKey.requirePlain(record.topic()));
      if (record.sourceOffset() != null) builder.sourceOffset(record.sourceOffset());
      if (record.sourcePartition() != null) builder.sourcePartition(record.sourcePartition());
      if (record.kafkaPartition() != null) builder.partition(record.kafkaPartition());
      if (record.timestamp() != null) builder.timestamp(record.timestamp());
      builder.row(Serializer.ROW.from((byte[]) record.key()));
      r = builder.build();
    }
    commitRecord(r, RecordMetadata.of(metadata));
  }

  @Override
  public final String version() {
    return VersionUtils.VERSION;
  }

  // -------------------------------------------------[UN-OVERRIDE]-------------------------------------------------//
  @Override
  public final void initialize(SourceTaskContext context) {
    super.initialize(context);
    rowContext = RowSourceContext.toRowSourceContext(context);
  }
}
