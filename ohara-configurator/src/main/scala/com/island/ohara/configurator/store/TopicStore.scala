package com.island.ohara.configurator.store

import java.util
import java.util.Properties
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, Executors, TimeUnit}

import com.island.ohara.config.UuidUtil
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.io.CloseOnce
import com.island.ohara.kafka.KafkaUtil
import com.island.ohara.serialization.Serializer
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, OffsetResetStrategy}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.header.Header

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * This class implements the OStroe through the kafka topic. The config passed to this class will be added with All data persist in the kafka topic.
  * In order to reduce the seek to topic, this class have a local cache to store the data polled from kafak consumer. The data stored to this class
  * will be sync to kafka topic. It means both update and remove methods invoke the kafka operations.
  *
  * NOTED: This class require the kafka serializer and deserializer. They are used to convert the object to byte array.
  * Without the requried configs, you will fail to instantiate this class
  *
  * NOTED: There are two kind of execution order in this class. 1) the caller order happening in update/remove. 2) data commit order. We take later
  * to complete the consistent order. For example, there are two callers A) and B). A call the update before B. However, the data updated by
  * B is committed before A. So the TopicStore#update will return the data of B to A.
  *
  * NOTED: Since we view the topic as persistent storage, this class will reset the offset to the beginner to load all data from topic. If
  * you try to change the offset, the side-effect is that the data may be loss.
  *
  * NOTED: this class is NOT designed to update-heavy pattern since each update to this class will be sync to kafka topic AND must
  * wait the response from kafka topic. Hence, the latency MAY be vary large.
  *
  * NOTED: this class is exposed as package-private for testing.
  *
  * @tparam K key type
  * @tparam V value type
  */
private class TopicStore[K, V](keySerializer: Serializer[K],
                               valueSerializer: Serializer[V],
                               brokers: String,
                               topicName: String,
                               numberOfPartitions: Int,
                               numberOfReplications: Short,
                               pollTimeout: Duration,
                               initializationTimeout: Duration,
                               topicOptions: Map[String, String])
    extends Store[K, V]
    with CloseOnce {

  private[this] val log = Logger(TopicStore.getClass)

  /**
    * The ohara configurator is a distributed services. Hence, we need a uuid for each configurator in order to distinguish the records.
    * TODO: make sure this uuid is unique in a distributed cluster. by chia
    */
  val uuid = UuidUtil.uuid

  implicit val executor =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  /**
    * Used to sort the change to topic. We shouldn't worry about the overflow since it is not update-heavy.
    */
  private[this] val HEADER_INDEX = new AtomicLong(0)
  private[this] val logger = Logger(getClass.getName)

  if (!topicOptions
        .get(TopicConfig.CLEANUP_POLICY_CONFIG)
        .map(_.equals(TopicConfig.CLEANUP_POLICY_COMPACT))
        .getOrElse(true))
    throw new IllegalArgumentException(
      s"The topic store require the ${TopicConfig.CLEANUP_POLICY_CONFIG}=${TopicConfig.CLEANUP_POLICY_COMPACT}")

  log.info(
    s"start to initialize the topic:$topicName partitions:$numberOfPartitions replications:$numberOfReplications")

  /**
    * Initialize the topic
    */
  CloseOnce.doClose(KafkaClient(brokers)) { client =>
    client.topicCreator
      .topicName(topicName)
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      // enable kafka save the latest message for each key
      .topicOptions(topicOptions + (TopicConfig.CLEANUP_POLICY_CONFIG -> TopicConfig.CLEANUP_POLICY_COMPACT))
      .timeout(initializationTimeout)
      .create()
  }

  log.info(
    s"succeed to initialize the topic:$topicName partitions:$numberOfPartitions replications:$numberOfReplications")

  private[this] val consumer = newOrClose {
    val props = new Properties()
    props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.name.toLowerCase)
    props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, uuid)
    new KafkaConsumer[K, V](props,
                            KafkaUtil.wrapDeserializer(keySerializer),
                            KafkaUtil.wrapDeserializer(valueSerializer))
  }

  private[this] val producer = newOrClose {
    val props = new Properties()
    props.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
    new KafkaProducer[K, V](props, KafkaUtil.wrapSerializer(keySerializer), KafkaUtil.wrapSerializer(valueSerializer))
  }
  private[this] val updateLock = new Object
  private[this] val commitResult = new ConcurrentHashMap[String, Option[V]]()
  private[this] val cache = newOrClose(Store.inMemory(keySerializer, valueSerializer))

  /**
    * true if poller haven't grab any data recently.
    */
  private[this] val initializeConsumerLatch = new CountDownLatch(1)
  private[this] val poller = Future[Unit] {
    var firstPoll = true
    try {
      consumer.subscribe(util.Arrays.asList(topicName))
      while (!this.isClosed) {
        try {
          val records = consumer.poll(if (firstPoll) 0 else pollTimeout.toMillis)
          if (firstPoll) initializeConsumerLatch.countDown()
          firstPoll = false
          if (records != null) {
            records
            // TODO: throw exception if there are data from unknown topic? by chia
              .records(topicName)
              .forEach(record => {
                val headers = record.headers().iterator()
                var count = 0
                var index: String = null
                while (headers.hasNext) {
                  if (count == 0) {
                    val key = headers.next().key()
                    // make sure we only store the record to which we sent
                    if (key.startsWith(uuid)) {
                      index = key
                    }
                    count += 1
                  } else throw new IllegalArgumentException(s"The number of header should be 1")
                }
                val previous =
                  if (record.value() == null) cache.remove(record.key()) else cache.update(record.key(), record.value())
                // index != null means this record is sent by this node
                if (index != null) {
                  commitResult.put(index, previous)
                  updateLock.synchronized {
                    updateLock.notifyAll()
                  }
                }
              })
          }
        } catch {
          case _: WakeupException => logger.debug("interrupted by ourself")
        }
      }
    } catch {
      case e: Throwable => logger.error("failure when running the poller", e)
    } finally {
      if (firstPoll) initializeConsumerLatch.countDown()
      TopicStore.this.close()
    }
  }

  if (!initializeConsumerLatch.await(initializationTimeout.toMillis, TimeUnit.MILLISECONDS)) {
    close()
    throw new IllegalArgumentException(s"timeout to initialize the queue")
  }

  override def update(key: K, value: V): Option[V] = waitCallback(send(key, value))

  override def get(key: K): Option[V] = cache.get(key)

  override def remove(key: K): Option[V] = waitCallback(send(key))

  override protected def doClose(): Unit = {
    import scala.concurrent.duration._
    // notify the poller
    if (consumer != null) consumer.wakeup()
    // hardcode
    if (poller != null) CloseOnce.release(() => Await.result(poller, 60 seconds))
    CloseOnce.close(producer)
    CloseOnce.close(consumer)
    CloseOnce.close(cache)
    if (executor != null) {
      executor.shutdownNow()
      executor.awaitTermination(60, TimeUnit.SECONDS)
    }
  }

  override def iterator: Iterator[(K, V)] = cache.iterator

  /**
    * Override the size to provide the efficient implementation
    *
    * @return size of this store
    */
  override def size: Int = cache.size

  private[this] def waitCallback(index: String): Option[V] = {
    while (!commitResult.containsKey(index) && !isClosed) {
      updateLock.synchronized {
        // wait to poll the data from kafka consumer
        updateLock.wait(1000)
      }
    }
    checkClose()
    commitResult.remove(index)
  }

  private[this] def send(key: K, value: V = null.asInstanceOf[V]): String = {
    val header = createHeader(uuid)
    producer.send(new ProducerRecord[K, V](topicName, null, key, value, util.Arrays.asList(header)))
    producer.flush()
    header.key()
  }

  private def createHeader(uuid: String): Header = {
    new Header() {
      private[this] val uuidIndex = uuid + "-" + HEADER_INDEX.getAndIncrement().toString

      override def key(): String = uuidIndex

      /**
        * @return an empty array since we don't use this field
        */
      override def value(): Array[Byte] = TopicStore.EMPTY_ARRAY
    }
  }

  override def take(timeout: Duration): Option[(K, V)] = {
    // first - remove the element from cache
    val rval = cache.take(timeout)
    // second - remove the element from kafka topic
    rval.map {
      case (k, _) => remove(k)
    }
    rval
  }
}

private object TopicStore {

  /**
    * zero array. Used to be the value of header.
    */
  private val EMPTY_ARRAY = new Array[Byte](0)
}
