package com.island.ohara.kafka;

import com.island.ohara.common.data.Serializer;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * a simple wrap from kafka producer.
 *
 * @tparam K key type
 * @tparam V value type
 */
public final class ProducerBuilder {
  private String brokers;
  private short numberOfAcks;

  public ProducerBuilder brokers(String brokers) {
    this.brokers = brokers;
    return this;
  }

  public ProducerBuilder noAcks() {
    this.numberOfAcks = 0;
    return this;
  }

  public ProducerBuilder allAcks() {
    this.numberOfAcks = -1;
    return this;
  }

  // TODO: 2018/11/19  is threr more good way to do implicit in java
  public <K, V> Producer<K, V> build(Serializer<K> keySerializer, Serializer<V> valueSerializer) {
    Objects.requireNonNull(brokers);
    return new Producer<K, V>() {

      private Properties getProducerConfig() {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(ProducerConfig.ACKS_CONFIG, String.valueOf(numberOfAcks));
        return props;
      }

      private KafkaProducer producer = getProducer();

      private KafkaProducer getProducer() {
        return new KafkaProducer<K, V>(
            getProducerConfig(),
            KafkaUtil.wrapSerializer(keySerializer),
            KafkaUtil.wrapSerializer(valueSerializer));
      }

      @Override
      public Sender<K, V> sender() {
        return new Sender<K, V>() {

          @Override
          protected void doSend(String topic, Handler<RecordMetadata> handler) {
            ProducerRecord<K, V> record =
                new ProducerRecord<K, V>(
                    topic,
                    partition().map(x -> new Integer(x)).orElse(null),
                    timestamp().map(x -> new Long(x)).orElse(null),
                    key().orElse(null),
                    value().orElse(null),
                    headers().stream().map(x -> toKafkaHeader(x)).collect(Collectors.toList()));

            producer.send(
                record,
                new Callback() {
                  @Override
                  public void onCompletion(
                      org.apache.kafka.clients.producer.RecordMetadata metadata,
                      Exception exception) {
                    if (metadata == null && exception == null)
                      handler.doException(
                          new IllegalStateException(
                              "no meta and exception from kafka producer...It should be impossible"));
                    if (metadata != null && exception != null)
                      handler.doException(
                          new IllegalStateException(
                              "Both meta and exception from kafka producer...It should be impossible"));
                    if (metadata != null)
                      handler.doHandle(
                          new RecordMetadata(
                              metadata.topic(),
                              metadata.partition(),
                              metadata.offset(),
                              metadata.timestamp(),
                              metadata.serializedKeySize(),
                              metadata.serializedValueSize()));
                    if (exception != null) handler.doException(exception);
                  }
                });
          }
        };
      }

      @Override
      public void flush() {
        producer.flush();
      }

      @Override
      public void close() {
        producer.close();
      }
    };
  }

  private org.apache.kafka.common.header.Header toKafkaHeader(Header header) {
    return new KafkaHeader(header);
  }

  private class KafkaHeader implements org.apache.kafka.common.header.Header {

    private Header header;

    public KafkaHeader(Header header) {
      this.header = header;
    }

    @Override
    public String key() {
      return header.key();
    }

    @Override
    public byte[] value() {
      return header.value();
    }
  }
}
