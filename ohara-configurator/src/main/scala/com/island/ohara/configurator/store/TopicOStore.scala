package com.island.ohara.configurator.store

import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import com.island.ohara.config.{OharaConfig, Property, UuidUtil}
import com.island.ohara.io.CloseOnce
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.header.Header

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

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
  * B is committed before A. So the TopicOStore#update will return the data of B to A.
  *
  * NOTED: Since we view the topic as persistent storage, this class will reset the offset to the beginner to load all data from topic. If
  * you try to change the offset, the side-effect is that the data may be loss.
  *
  * NOTED: this class is NOT designed to update-heavy pattern since each update to this class will be sync to kafka topic AND must
  * wait the response from kafka topic. Hence, the latency MAY be vary large.
  *
  * NOTED: this class is exposed as package-private for testing.
  * @param config configuration
  * @tparam K key type
  * @tparam V value type
  */
private[store] class TopicOStore[K, V](config: OharaConfig) extends OStore[K, V] with CloseOnce {

  /**
    * The ohara configurator is a distributed services. Hence, we need a uuid for each configurator in order to distinuish the records.
    * TODO: make sure this uuid is unique in a distributed cluster. by chia
    */
  val uuid = UuidUtil.uuid()

  /**
    * Used to sort the change to topic. We shouldn't worry about the overflow since it is not a update-heavy.
    */
  private[this] val HEADER_INDEX = new AtomicLong(0)
  private[this] val logger = Logger(getClass.getName)
  val topicName = config.requireString(TopicOStore.TOPIC_NAME)
  val pollTimeout = config.get(TopicOStore.POLL_TIMEOUT)
  config.set(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  // enable kafka save the latest message for each key
  // we use magin string since the constant is located in kafka-core. Importing the whole core project is too expensive.
  config.set("log.cleanup.policy", "compact")
  config.set(ConsumerConfig.GROUP_ID_CONFIG, uuid)

  /**
    * Initialize the topic
    */
  CloseOnce.doClose(AdminClient.create(config.toProperties))(admin => {
    def topicExist: () => Boolean = () => admin.listTopics().names().thenApply(_.contains(topicName)).get()
    if (!topicExist()) {
      admin.createTopics(
        util.Arrays.asList(
          new NewTopic(topicName,
                       config.get(TopicOStore.TOPIC_PARTITION_COUNT),
                       config.get(TopicOStore.TOPIC_REPLICATION_COUNT))))
      val end = System.currentTimeMillis() + config.get(TopicOStore.CREATE_TOPIC_TIMEOUT)
      // wait the topic to be created
      while (!topicExist() && (System.currentTimeMillis() < end)) {
        TimeUnit.SECONDS.sleep(1)
      }
      if (!topicExist()) throw new IllegalArgumentException(s"Failed to create the $topicName")
    }
  })

  private[this] val consumer = new KafkaConsumer[K, V](config.toProperties)
  consumer.subscribe(util.Arrays.asList(topicName))
  private[this] val producer = new KafkaProducer[K, V](config.toProperties)
  private[this] val updateLock = new Object
  private[this] val commitResult = new ConcurrentHashMap[String, Option[V]]()
  private[this] val cache = new MemOStore[K, V](config)

  /**
    * true if poller haven't grab any data recently.
    */
  private[this] val readToEnd = new AtomicBoolean(false)
  private[this] val poller = Future[Long] {
    var messageCount = 0L
    try {
      while (!this.isClosed) {
        try {
          val records = consumer.poll(pollTimeout)
          var haveData = false
          if (records != null) {
            records
            // TODO: throw exception if there are data from unknown topic? by chia
              .records(topicName)
              .forEach(record => {
                val headers = record.headers().iterator()
                var count = 0;
                var index: String = null
                while (headers.hasNext) {
                  if (count == 0) {
                    val key = headers.next().key()
                    // make sure we only store the record to which we sent
                    if (key.startsWith(uuid)) {
                      index = key
                    }
                    count += 1
                  } else {
                    throw new IllegalArgumentException(s"The number of header should be 1")
                  }
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
                haveData = true
              })
          }
          readToEnd.set(!haveData) // We have collected all data from the topic
        } catch {
          case e: WakeupException => logger.debug("interrupted by ourself")
          // TODO: Should we close this class when encountering the error? by chia
          case e: Throwable => logger.error("failure when running the poller", e)
        }
      }
    } finally {
      // we do the wait for initialization of reading topic. cancel the flag to break the wait loop.
      readToEnd.set(true)
      TopicOStore.this.close()
    }
    messageCount
  }

  // wait to grab all data from topic
  while (!isClosed && !readToEnd.get()) {
    TimeUnit.MILLISECONDS.sleep(500)
  }

  override def update(key: K, value: V): Option[V] = waitCallback(send(key, value))

  override def get(key: K): Option[V] = cache.get(key)

  override def remove(key: K): Option[V] = waitCallback(send(key))
  override protected def doClose(): Unit = {
    import scala.concurrent.duration._
    // notify the poller
    consumer.wakeup()
    // hardcode
    Await.result(poller, 60 seconds)
    producer.close()
    consumer.close()
    cache.close()
  }
  override def iterator: Iterator[(K, V)] = cache.iterator

  /**
    * Overrride the size to provide the efficient implementation
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
      override def value(): Array[Byte] = TopicOStore.EMPTY_ARRAY
    }
  }
}

object TopicOStore {

  /**
    * A required config. It dedicate the topic name used to store the data.
    */
  val TOPIC_NAME = "ohara.topicostore.topic.name"
  val TOPIC_PARTITION_COUNT = Property.builder
    .description("The number of partition of backed topic")
    .key("ohara.topicostore.partition.count")
    .build(10)
  val TOPIC_REPLICATION_COUNT = Property.builder
    .description("The number of replication of backed topic")
    .key("ohara.topicostore.replication.count")
    .build(10.toShort)
  val POLL_TIMEOUT = Property.builder
    .description("The time, in milliseconds, spent waiting in poll the kafka consumer")
    .key("ohara.topicostore.poll.timeout")
    .build(5 * 1000)
  val CREATE_TOPIC_TIMEOUT = Property.builder
    .description("The time, in milliseconds, spent waiting in creating the topic")
    .key("ohara.topicostore.create.topic.timeout")
    .build(60 * 1000)

  /**
    * zero array. Used to be the value of header.
    */
  private val EMPTY_ARRAY = new Array[Byte](0)
}
