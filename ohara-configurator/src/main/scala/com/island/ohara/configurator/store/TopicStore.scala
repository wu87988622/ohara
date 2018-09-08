package com.island.ohara.configurator.store

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, Executors, TimeUnit}

import com.island.ohara.io.{CloseOnce, UuidUtil}
import com.island.ohara.kafka.{Consumer, Header, KafkaClient, Producer}
import com.island.ohara.serialization.Serializer
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.errors.WakeupException

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}

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

  private[this] val log = Logger(classOf[TopicStore[_, _]])

  /**
    * The ohara configurator is a distributed services. Hence, we need a uuid for each configurator in order to distinguish the records.
    * TODO: make sure this uuid is unique in a distributed cluster. by chia
    */
  val uuid: String = UuidUtil.uuid()

  implicit val executor: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  /**
    * Used to sort the change to topic. We shouldn't worry about the overflow since it is not update-heavy.
    */
  private[this] val headerIndexer = new AtomicLong(0)
  private[this] val logger = Logger(getClass.getName)

  if (topicOptions.get(TopicConfig.CLEANUP_POLICY_CONFIG).fold(false)(_ != TopicConfig.CLEANUP_POLICY_COMPACT))
    throw new IllegalArgumentException(
      s"The topic store require the ${TopicConfig.CLEANUP_POLICY_CONFIG}=${TopicConfig.CLEANUP_POLICY_COMPACT}")

  log.info(
    s"start to initialize the topic:$topicName partitions:$numberOfPartitions replications:$numberOfReplications")

  /**
    * Initialize the topic
    */
  CloseOnce.doClose(KafkaClient(brokers)) { client =>
    if (!client.exist(topicName))
      client
        .topicCreator()
        .numberOfPartitions(numberOfPartitions)
        .numberOfReplications(numberOfReplications)
        .options(topicOptions)
        // enable kafka save the latest message for each key
        .compacted()
        .timeout(initializationTimeout)
        .create(topicName)
  }

  log.info(
    s"succeed to initialize the topic:$topicName partitions:$numberOfPartitions replications:$numberOfReplications")

  private[this] val consumer = newOrClose {
    Consumer
      .builder(keySerializer, valueSerializer)
      .topicName(topicName)
      .brokers(brokers)
      .offsetFromBegin()
      .groupId(uuid)
      .build()
  }

  private[this] val producer = newOrClose {
    Producer.builder(keySerializer, valueSerializer).brokers(brokers).build()
  }

  private[this] val updateLock = new Object
  private[this] val commitResult = new ConcurrentHashMap[String, Option[V]]()
  private[this] val cache = newOrClose(Store.inMemory(keySerializer, valueSerializer))

  /**
    * true if poller haven't grab any data recently.
    */
  private[this] val initializeConsumerLatch = new CountDownLatch(1)
  private[this] val poller = Future[Unit] {
    try {
      while (!this.isClosed) {
        try {
          val records = consumer.poll(pollTimeout)
          initializeConsumerLatch.countDown()
          records
            .filter(_.topic == topicName)
            .foreach(record => {
              if (record.headers.size != 1) throw new IllegalArgumentException(s"The number of header should be 1")
              record.key.foreach(k => {
                // If no value exist, remove the key. Otherwise, update the value mapped to the key
                val previous: Option[V] = record.value.fold(cache.remove(k))(cache.update(k, _))

                val index = record.headers.head.key
                // response the update sent by this topic store
                if (index.startsWith(uuid)) {
                  commitResult.put(index, previous)
                  updateLock.synchronized {
                    updateLock.notifyAll()
                  }
                }
              })
            })
        } catch {
          case _: WakeupException => logger.debug("interrupted by ourself")
        }
      }
    } catch {
      case e: Throwable => logger.error("failure when running the poller", e)
    } finally {
      initializeConsumerLatch.countDown()
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
    val header = Header(s"$uuid-${headerIndexer.getAndIncrement().toString}", Array.emptyByteArray)
    producer.sender().header(header).key(key).value(value).send(topicName)
    producer.flush()
    header.key
  }

  override def take(timeout: Duration): Option[(K, V)] = {
    // first - remove the element from cache
    val rval = cache.take(timeout)
    // second - remove the element from kafka topic
    rval.foreach {
      case (k, _) => remove(k)
    }
    rval
  }

  override def clear(): Unit = cache.iterator.map(_._1).foreach(remove)
}
