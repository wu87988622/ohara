package com.island.ohara.ostreams;

import com.island.ohara.common.data.Serializer;
import java.util.Map;

public class DoubleSerializer implements org.apache.kafka.common.serialization.Serializer<Double> {

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {}

  @Override
  public byte[] serialize(String topic, Double data) {
    if (data == null) return null;
    else return Serializer.DOUBLE.to(data);
  }

  @Override
  public void close() {}
}
