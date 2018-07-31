package com.island.ohara.kafka

import java.util.Properties

import com.island.ohara.data.Row
import com.island.ohara.serialization.RowSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serializer

/**
  * A warp to Kafka producer. The main change is the generic type V is pined at Row type. The conversion between Row and
  * byte array is covered. Ohara user shouldn't care about the serialization of ohara row.
  *
  * TODO: wrap the generic type K. by chia
  * TODO: wrap the config. by chia
  *
  * @param config        kafka config
  * @param keySerializer key serializer
  * @tparam K key type
  */
class RowProducer[K](config: Properties, keySerializer: Serializer[K] = null)
    extends KafkaProducer[K, Row](config, keySerializer, KafkaUtil.wrapSerializer(RowSerializer))
