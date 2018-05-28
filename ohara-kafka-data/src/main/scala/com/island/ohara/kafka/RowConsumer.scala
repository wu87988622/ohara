package com.island.ohara.kafka

import java.util.Properties

import com.island.ohara.core.Row
import com.island.ohara.serialization.RowSerializer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.Deserializer

/**
  * A warp to Kafka consumer. The main change is the generic type V is pined at Row type. The conversion between Row and
  * byte array is covered. Ohara user shouldn't care about the serialization of ohara row.
  *
  * TODO: wrap the generic type K. by chia
  * TODO: wrap the config. by chia
  *
  * @param properties      kafka config
  * @param keyDeserializer key deserializer
  * @tparam K key type
  */
class RowConsumer[K](properties: Properties, keyDeserializer: Deserializer[K])
    extends KafkaConsumer[K, Row](properties, keyDeserializer, KafkaUtil.wrapDeserializer(RowSerializer))
