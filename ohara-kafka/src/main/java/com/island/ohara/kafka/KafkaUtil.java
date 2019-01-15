package com.island.ohara.kafka;

import com.island.ohara.common.data.Serializer;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Make the wrap from kafka components.
 *
 * <p>Every method with remote call need to overload in Default Timeout
 */
public class KafkaUtil {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(20);

  /**
   * Used to convert ohara row to byte array. It is a private class since ohara producer will
   * instantiate one and pass it to kafka producer. Hence, no dynamical call will happen in kafka
   * producer. The access exception won't be caused.
   *
   * @param serializer ohara serializer
   * @tparam T object type
   * @return a wrapper from kafka serializer
   */
  public static <T> org.apache.kafka.common.serialization.Serializer<T> wrapSerializer(
      Serializer<T> serializer) {
    return new org.apache.kafka.common.serialization.Serializer<T>() {

      @Override
      public void configure(Map<String, ?> configs, boolean isKey) {
        // do nothing
      }

      @Override
      public byte[] serialize(String topic, T data) {
        return data == null ? null : serializer.to(data);
      }

      @Override
      public void close() {
        // do nothing
      }
    };
  }

  /**
   * Used to convert byte array to ohara row. It is a private class since ohara consumer will
   * instantiate one and pass it to kafka consumer. Hence, no dynamical call will happen in kafka
   * consumer. The access exception won't be caused.
   *
   * @param serializer ohara serializer
   * @tparam T object type
   * @return a wrapper from kafka deserializer
   */
  public static <T> Deserializer<T> wrapDeserializer(Serializer<T> serializer) {
    return new org.apache.kafka.common.serialization.Deserializer<T>() {

      @Override
      public void configure(Map<String, ?> configs, boolean isKey) {
        // do nothing
      }

      @Override
      public T deserialize(String topic, byte[] data) {
        return data == null ? null : serializer.from(data);
      }

      @Override
      public void close() {
        // do nothing
      }
    };
  }
  /**
   * check whether the specified topic exist
   *
   * @param brokersConnProps the location from kafka brokersConnProps
   * @param topicName topic nameHDFSStorage
   * @return true if the topic exist. Otherwise, false
   */
  public static boolean exist(String brokersConnProps, String topicName, Duration timeout) {
    try (KafkaClient client = KafkaClient.of(brokersConnProps)) {
      return client.exist(topicName, timeout);
    }
  }

  public static boolean exist(String brokersConnProps, String topicName) {
    return exist(brokersConnProps, topicName, DEFAULT_TIMEOUT);
  }

  public static TopicDescription topicDescription(
      String brokersConnProps, String topicName, Duration timeout) {
    try (KafkaClient client = KafkaClient.of(brokersConnProps)) {
      return client.topicDescription(topicName, timeout);
    }
  }

  public static TopicDescription topicDescription(String brokersConnProps, String topicName) {
    return topicDescription(brokersConnProps, topicName, DEFAULT_TIMEOUT);
  }
  /**
   * Increate the number from partitions. This method check the number before doing the alter. If
   * the number is equal to the previous setting, nothing will happen; Decreasing the number is not
   * allowed and it will cause an IllegalArgumentException. Otherwise, this method use kafka
   * AdminClient to send the request to increase the number from partitions.
   *
   * @param brokersConnProps brokersConnProps information
   * @param topicName topic name
   * @param numberOfPartitions if this number is s
   */
  public static void addPartitions(
      String brokersConnProps, String topicName, int numberOfPartitions, Duration timeout) {
    try (KafkaClient client = KafkaClient.of(brokersConnProps)) {
      client.addPartitions(topicName, numberOfPartitions, timeout);
    }
  }

  public static void addPartitions(
      String brokersConnProps, String topicName, int numberOfPartitions) {
    addPartitions(brokersConnProps, topicName, numberOfPartitions, DEFAULT_TIMEOUT);
  }

  public static void createTopic(
      String brokersConnProps,
      String topicName,
      int numberOfPartitions,
      short numberOfReplications,
      Map<String, String> options,
      Duration timeout) {
    try (KafkaClient client = KafkaClient.of(brokersConnProps)) {
      client
          .topicCreator()
          .timeout(timeout)
          .numberOfPartitions(numberOfPartitions)
          .numberOfReplications(numberOfReplications)
          .options(options)
          .create(topicName);
    }
  }

  public static void createTopic(
      String brokersConnProps,
      String topicName,
      int numberOfPartitions,
      short numberOfReplications,
      Map<String, String> options) {
    createTopic(
        brokersConnProps,
        topicName,
        numberOfPartitions,
        numberOfReplications,
        options,
        DEFAULT_TIMEOUT);
  }

  public static void createTopic(
      String brokersConnProps,
      String topicName,
      int numberOfPartitions,
      short numberOfReplications) {
    createTopic(
        brokersConnProps,
        topicName,
        numberOfPartitions,
        numberOfReplications,
        Collections.emptyMap());
  }

  public static void deleteTopic(String brokersConnProps, String topicName, Duration timeout) {
    try (KafkaClient client = KafkaClient.of(brokersConnProps)) {
      client.deleteTopic(topicName, timeout);
    }
  }

  public static void deleteTopic(String brokersConnProps, String topicName) {
    deleteTopic(brokersConnProps, topicName, DEFAULT_TIMEOUT);
  }
}
