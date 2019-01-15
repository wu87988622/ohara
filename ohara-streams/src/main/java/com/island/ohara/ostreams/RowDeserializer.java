package com.island.ohara.ostreams;

import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import java.util.Map;

public class RowDeserializer implements org.apache.kafka.common.serialization.Deserializer<Row> {

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {}

  @Override
  public Row deserialize(String topic, byte[] data) {
    if (data == null) return null;
    else return Serializer.ROW.from(data);
  }

  @Override
  public void close() {}
}
