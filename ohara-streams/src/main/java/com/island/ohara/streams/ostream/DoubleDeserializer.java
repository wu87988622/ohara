package com.island.ohara.streams.ostream;

import com.island.ohara.common.data.Serializer;
import java.util.Map;

public class DoubleDeserializer
    implements org.apache.kafka.common.serialization.Deserializer<Double> {

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {}

  @Override
  public Double deserialize(String topic, byte[] data) {
    if (data == null) return null;
    else return Serializer.DOUBLE.from(data);
  }

  @Override
  public void close() {}
}
