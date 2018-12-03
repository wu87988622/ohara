package com.island.ohara.kafka.connector;

import static com.island.ohara.kafka.connector.ConnectorUtil.VERSION;

import com.island.ohara.common.data.Serializer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.apache.kafka.connect.source.SourceTaskContext;

/** A wrap to SourceTask. The users should substitute RowSourceRecord for SourceRecord. */
public abstract class RowSourceTask extends SourceTask {

  /**
   * Start the Task. This should handle any configuration parsing and one-time setup from the task.
   *
   * @param config initial configuration
   */
  protected abstract void _start(TaskConfig config);

  /**
   * Signal this SourceTask to stop. In SourceTasks, this method only needs to signal to the task
   * that it should stop trying to poll for new data and interrupt any outstanding poll() requests.
   * It is not required that the task has fully stopped. Note that this method necessarily may be
   * invoked from a different thread than _poll() and _commit()
   */
  protected abstract void _stop();
  /**
   * Poll this SourceTask for new records. This method should block if no data is currently
   * available.
   *
   * @return a array from RowSourceRecord
   */
  protected abstract List<RowSourceRecord> _poll();

  /**
   * Commit an individual RowSourceRecord when the callback from the producer client is received, or
   * if a record is filtered by a transformation. SourceTasks are not required to implement this
   * functionality; Kafka Connect will record offsets automatically. This hook is provided for
   * systems that also need to store offsets internally in their own system.
   *
   * @param record RowSourceRecord that was successfully sent via the producer.
   */
  protected void _commitRecord(RowSourceRecord record) {
    // do nothing
  }

  /**
   * Commit the offsets, up to the offsets that have been returned by _poll(). This method should
   * block until the commit is complete.
   *
   * <p>SourceTasks are not required to implement this functionality; Kafka Connect will record
   * offsets automatically. This hook is provided for systems that also need to store offsets
   * internally in their own system.
   */
  protected void _commit() {
    // do nothing
  }

  /**
   * Get the version from this task. Usually this should be the same as the corresponding Connector
   * class's version.
   *
   * @return the version, formatted as a String
   */
  protected String _version() {
    return VERSION;
  }

  /**
   * @return RowSourceContext is provided to RowSourceTask to allow them to interact with the
   *     underlying runtime.
   */
  protected RowSourceContext rowContext = null;
  // -------------------------------------------------[WRAPPED]-------------------------------------------------//
  @Override
  public final List<SourceRecord> poll() {
    List<RowSourceRecord> value = _poll();
    // kafka connector doesn't support the empty list in testing. see
    // https://github.com/apache/kafka/pull/4958
    if (value == null || value.isEmpty()) return null;
    else
      return value
          .stream()
          .map(
              s ->
                  new SourceRecord(
                      s.sourcePartition(),
                      s.sourceOffset(),
                      s.topic(),
                      s.partition().map(x -> new Integer(x)).orElse(null),
                      Schema.BYTES_SCHEMA,
                      s.key(),
                      Schema.BYTES_SCHEMA,
                      Serializer.ROW.to(s.row()),
                      s.timestamp().map((x) -> new java.lang.Long(x)).orElse(null)))
          .collect(Collectors.toList());
  }

  @Override
  public final void start(Map<String, String> props) {
    _start(ConnectorUtil.toTaskConfig(props));
  }

  @Override
  public final void stop() {
    _stop();
  }

  @Override
  public final void commit() {
    _commit();
  }

  // TODO: We do a extra conversion here (bytes => Row)... by chia
  @Override
  public final void commitRecord(SourceRecord record) {
    _commitRecord(
        RowSourceRecord.builder()
            .sourcePartition(
                record.sourcePartition() == null
                    ? Collections.emptyMap()
                    : record.sourcePartition())
            .sourceOffset(
                record.sourceOffset() == null ? Collections.emptyMap() : record.sourceOffset())
            .row(Serializer.ROW.from((byte[]) record.value()))
            ._timestamp(Optional.ofNullable(record.timestamp()))
            ._partition(Optional.ofNullable(record.kafkaPartition()))
            .build(record.topic()));
  }

  @Override
  public final String version() {
    return _version();
  }

  // -------------------------------------------------[UN-OVERRIDE]-------------------------------------------------//
  @Override
  public final void initialize(SourceTaskContext context) {
    super.initialize(context);
    rowContext = RowSourceContext.toRowSourceContext(context);
  }
}
