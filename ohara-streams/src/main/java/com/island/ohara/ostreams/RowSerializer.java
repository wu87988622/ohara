package com.island.ohara.ostreams;

import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import java.util.Map;

public class RowSerializer implements org.apache.kafka.common.serialization.Serializer<Row> {

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {}

  @Override
  public byte[] serialize(String topic, Row data) {
    if (data == null) return null;
    else return Serializer.ROW.to(data);
  }

  @Override
  public void close() {}
}
