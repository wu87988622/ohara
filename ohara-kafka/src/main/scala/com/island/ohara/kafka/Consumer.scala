package com.island.ohara.kafka
import java.util.concurrent.atomic.AtomicInteger
import java.util.{Objects, Properties}

import com.island.ohara.io.CloseOnce
import com.island.ohara.serialization.Serializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, OffsetResetStrategy}

import scala.concurrent.duration.Duration

/**
  * a simple scala wrap of kafka consumer.
  * @tparam K key type
  * @tparam V value type
  */
trait Consumer[K, V] extends CloseOnce {

  /**
    * poll the data from subscribed topics
    * @param timeout waiting time
    * @return records
    */
  def poll(timeout: Duration): Seq[ConsumerRecord[K, V]]

  /**
    * @return the topic names subscribed by this consumer
    */
  def subscription(): Set[String]

  /**
    * break the poll right now.
    */
  def wakeup(): Unit
}

object Consumer {
  private[kafka] val CONSUMER_ID = new AtomicInteger(0)

  def builder[K, V](keySerializer: Serializer[K], valueSerializer: Serializer[V]): ConsumerBuilder[K, V] =
    new ConsumerBuilder[K, V](keySerializer, valueSerializer)
}

class ConsumerBuilder[K, V](val keySerializer: Serializer[K], val valueSerializer: Serializer[V]) {
  private[this] var fromBegin = false
  private[this] var topicNames: Seq[String] = _
  private[this] var groupId: String = s"ohara-consumer-${Consumer.CONSUMER_ID.getAndIncrement().toString}"
  private[this] var brokers: String = _

  /**
    * @param fromBegin true if you want to receive all un-deleted message from subscribed topics
    * @return this builder
    */
  def fromBegin(fromBegin: Boolean): this.type = {
    this.fromBegin = fromBegin
    this
  }

  /**
    * @param topicName the topic you want to subscribe
    * @return this builder
    */
  def topicName(topicName: String): this.type = {
    this.topicNames = Seq(topicName)
    this
  }

  /**
    * @param topicName the topics you want to subscribe
    * @return this builder
    */
  def topicNames(topicNames: Seq[String]): this.type = {
    this.topicNames = topicNames
    this
  }

  def groupId(groupId: String): this.type = {
    this.groupId = groupId
    this
  }

  def brokers(brokers: String): this.type = {
    this.brokers = brokers
    this
  }

  def build(): Consumer[K, V] = {
    Objects.requireNonNull(topicNames)
    Objects.requireNonNull(groupId)
    Objects.requireNonNull(brokers)
    innerBuild
  }

  protected def innerBuild(): Consumer[K, V] = new Consumer[K, V] {
    private[this] val consumerConfig = {
      val props = new Properties()
      props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
      props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
      props.put(
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
        if (fromBegin) OffsetResetStrategy.EARLIEST.name.toLowerCase
        else OffsetResetStrategy.LATEST.name.toLowerCase
      )
      props
    }
    private[this] val kafkaConsumer = new KafkaConsumer[K, V](consumerConfig,
                                                              KafkaUtil.wrapDeserializer(keySerializer),
                                                              KafkaUtil.wrapDeserializer(valueSerializer))
    import scala.collection.JavaConverters._
    override def poll(timeout: Duration): Seq[ConsumerRecord[K, V]] = {
      if (subscription().isEmpty) kafkaConsumer.subscribe(topicNames.asJava)
      val r = kafkaConsumer.poll(timeout.toMillis)
      if (r == null || r.isEmpty) Seq.empty
      else
        r.iterator()
          .asScala
          .map(cr =>
            ConsumerRecord(
              cr.topic(),
              Option(cr.headers())
                .map(headers => headers.asScala.map(header => Header(header.key(), header.value())).toSeq)
                .getOrElse(Seq.empty),
              Option(cr.key()),
              Option(cr.value())
          ))
          .toList
    }

    override protected def doClose(): Unit = kafkaConsumer.close()
    override def subscription(): Set[String] = kafkaConsumer.subscription().asScala.toSet

    override def wakeup(): Unit = kafkaConsumer.wakeup()
  }
}

case class Header(key: String, value: Array[Byte])

/**
  * a scala wrap of kafka's consumer record.
  * @param topic topic name
  * @param key key (nullable)
  * @param value value
  * @tparam K key type
  * @tparam V value type
  */
case class ConsumerRecord[K, V](topic: String, headers: Seq[Header], key: Option[K], value: Option[V])
