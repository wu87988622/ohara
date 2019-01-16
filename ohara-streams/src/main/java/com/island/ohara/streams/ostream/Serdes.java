package com.island.ohara.streams.ostream;

import com.island.ohara.common.data.Row;
import java.util.Map;

public class Serdes {

  public static Serde<String> StringSerde = STRING.get();
  public static Serde<Row> RowSerde = ROW.get();
  public static Serde<Double> DoubleSerde = DOUBLE.get();

  protected static class WrapperSerde<T> implements Serde<T> {

    private final org.apache.kafka.common.serialization.Serializer<T> serializer;
    private final org.apache.kafka.common.serialization.Deserializer<T> deserializer;

    WrapperSerde(
        org.apache.kafka.common.serialization.Serializer<T> serializer,
        org.apache.kafka.common.serialization.Deserializer<T> deserializer) {
      this.serializer = serializer;
      this.deserializer = deserializer;
    }

    public void configure(Map<String, ?> configs, boolean isKey) {
      serializer.configure(configs, isKey);
      deserializer.configure(configs, isKey);
    }

    public void close() {
      serializer.close();
      deserializer.close();
    }

    public org.apache.kafka.common.serialization.Serializer<T> serializer() {
      return serializer;
    }

    public org.apache.kafka.common.serialization.Deserializer<T> deserializer() {
      return deserializer;
    }
  }

  public static final class STRING extends WrapperSerde<String> {
    public STRING() {
      super(new StringSerializer(), new StringDeserializer());
    }

    static Serde<String> get() {
      return new WrapperSerde<>(new StringSerializer(), new StringDeserializer());
    }
  }

  public static final class ROW extends WrapperSerde<Row> {
    public ROW() {
      super(new RowSerializer(), new RowDeserializer());
    }

    static Serde<Row> get() {
      return new WrapperSerde<>(new RowSerializer(), new RowDeserializer());
    }
  }

  public static final class DOUBLE extends WrapperSerde<Double> {
    public DOUBLE() {
      super(new DoubleSerializer(), new DoubleDeserializer());
    }

    static Serde<Double> get() {
      return new WrapperSerde<>(new DoubleSerializer(), new DoubleDeserializer());
    }
  }
}
