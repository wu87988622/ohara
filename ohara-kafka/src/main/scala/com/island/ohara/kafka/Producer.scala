package com.island.ohara.kafka
import java.util.{Objects, Properties}

import com.island.ohara.io.CloseOnce
import com.island.ohara.serialization.Serializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}

import scala.concurrent.{Future, Promise}

/**
  * a simple wrap of kafka producer.
  * @tparam K key type
  * @tparam V value type
  */
trait Producer[K, V] extends CloseOnce {

  /**
    * create a sender used to send a record to brokers
    * @return a sender
    */
  def sender(): Sender[K, V]

  /**
    * flush all on-the-flight data.
    */
  def flush(): Unit
}

object Producer {
  def builder(): ProducerBuilder = new ProducerBuilder
}

final class ProducerBuilder {
  private[this] var brokers: String = _
  private[this] var numberOfAcks: Short = 1

  def brokers(brokers: String): ProducerBuilder = {
    this.brokers = brokers
    this
  }

  def noAcks(): ProducerBuilder = {
    this.numberOfAcks = 0
    this
  }

  def allAcks(): ProducerBuilder = {
    this.numberOfAcks = -1
    this
  }

  def build[K, V](implicit keySerializer: Serializer[K], valueSerializer: Serializer[V]): Producer[K, V] = {
    Objects.requireNonNull(brokers)
    new Producer[K, V] {
      private[this] val producerConfig = {
        val props = new Properties()
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
        props.put(ProducerConfig.ACKS_CONFIG, numberOfAcks.toString)
        props
      }
      private[this] val producer = newOrClose(
        new KafkaProducer[K, V](producerConfig,
                                KafkaUtil.wrapSerializer(keySerializer),
                                KafkaUtil.wrapSerializer(valueSerializer)))

      import scala.collection.JavaConverters._
      override def sender(): Sender[K, V] = (request, callback) => {
        val record = new ProducerRecord[K, V](
          request.topic,
          request.partition.map(new Integer(_)).orNull,
          request.timestamp.map(new java.lang.Long(_)).orNull,
          request.key.getOrElse(null.asInstanceOf[K]),
          request.value.getOrElse(null.asInstanceOf[V]),
          request.headers.map(toKafkaHeader).asJava
        )
        producer.send(
          record,
          (metadata: org.apache.kafka.clients.producer.RecordMetadata, exception: Exception) => {
            if (metadata == null && exception == null)
              callback(
                Left(new IllegalStateException("no meta and exception from kafka producer...It should be impossible")))

            if (metadata != null && exception != null)
              callback(
                Left(
                  new IllegalStateException("Both meta and exception from kafka producer...It should be impossible")))

            if (metadata != null)
              callback(
                Right(
                  RecordMetadata(metadata.topic(),
                                 metadata.partition(),
                                 metadata.offset(),
                                 metadata.timestamp(),
                                 metadata.serializedKeySize(),
                                 metadata.serializedValueSize())))
            if (exception != null) callback(Left(exception))
          }
        )
      }

      override def flush(): Unit = producer.flush()

      override protected def doClose(): Unit = producer.close()
    }
  }

  private[this] def toKafkaHeader(header: Header): org.apache.kafka.common.header.Header = new KafkaHeader(header)
  private[this] class KafkaHeader(header: Header) extends org.apache.kafka.common.header.Header {
    override def key(): String = header.key
    override def value(): Array[Byte] = header.value
  }

}

/**
  * a fluent-style sender. kafak.ProducerRecord has many fields and most of them are nullable. It makes kafak.ProducerRecord's
  * constructor complicated. This class has fluent-style methods helping user to fill the fields they have.
  * @tparam K key type
  * @tparam V value type
  */
abstract class Sender[K, V] {
  private[this] var partition: Option[Int] = None
  private[this] var headers: Seq[Header] = Seq.empty
  private[this] var key: Option[K] = None
  private[this] var value: Option[V] = None
  private[this] var timestamp: Option[Long] = None

  def partition(partition: Int): this.type = {
    this.partition = Some(partition)
    this
  }

  def header(header: Header): this.type = {
    this.headers = Seq(header)
    this
  }

  def headers(headers: Seq[Header]): this.type = {
    this.headers = headers
    this
  }

  def key(key: K): this.type = {
    this.key = Option(key)
    this
  }

  def value(value: V): this.type = {
    this.value = Option(value)
    this
  }

  def timestamp(timestamp: Long): this.type = {
    this.timestamp = Some(timestamp)
    this
  }

  /**
    * send the record to brokers with async future
    */
  def send(topic: String): Future[RecordMetadata] = {
    val promise = Promise[RecordMetadata]()
    send(topic, {
      case Left(e)  => promise.failure(e)
      case Right(m) => promise.success(m)
    })
    promise.future
  }

  import Sender._

  /**
    * send the record to brokers with callback
    * @param callback invoked after the record is completed or failed
    */
  def send(topic: String, callback: Either[Throwable, RecordMetadata] => Unit): Unit =
    doSend(Request(topic, partition, headers, key, value, timestamp), callback)

  protected def doSend(request: Request[K, V], callback: Either[Throwable, RecordMetadata] => Unit)
}

object Sender {
  case class Request[K, V](topic: String,
                           partition: Option[Int],
                           headers: Seq[Header],
                           key: Option[K],
                           value: Option[V],
                           timestamp: Option[Long])
}

case class RecordMetadata(topic: String,
                          partition: Int,
                          offset: Long,
                          timestamp: Long,
                          serializedKeySize: Int,
                          serializedValueSize: Int)
