package com.island.ohara.kafka.connector;

import static com.island.ohara.kafka.connector.Constants.BROKER;
import static com.island.ohara.kafka.connector.Constants.OUTPUT;

import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import com.island.ohara.kafka.Producer;
import java.util.List;

/** Used for testing. */
public class SimpleRowSinkTask extends RowSinkTask {
  private TaskConfig config = null;
  private String outputTopic = null;
  private Producer<byte[], Row> producer = null;

  @Override
  protected void _start(TaskConfig props) {
    this.config = props;
    outputTopic = config.options().get(OUTPUT);
    producer =
        Producer.builder()
            .brokers(config.options().get(BROKER))
            .build(Serializer.BYTES, Serializer.ROW);
  }

  @Override
  protected void _stop() {
    try {
      producer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void _put(List<RowSinkRecord> records) {
    records.forEach(
        r -> {
          producer.sender().key(r.key()).value(r.row()).send(outputTopic);
        });
  }
}
