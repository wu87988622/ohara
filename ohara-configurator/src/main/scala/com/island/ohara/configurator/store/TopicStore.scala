package com.island.ohara.configurator.store

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, Executors, TimeUnit}

import com.island.ohara.configurator.store.Consistency._
import com.island.ohara.io.{CloseOnce, UuidUtil}
import com.island.ohara.kafka.{Consumer, Header, KafkaClient, Producer}
import com.island.ohara.serialization.Serializer
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.errors.WakeupException

import scala.concurrent._
import scala.concurrent.duration.{Duration, _}
import scala.util.{Failure, Success}

/**
  * This class implements the Store through the kafka topic. The config passed to this class will be added with All data persist in the kafka topic.
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
private class TopicStore[K, V](
  brokers: String,
  topicName: String,
  numberOfPartitions: Int,
  numberOfReplications: Short,
  pollTimeout: Duration,
  initializationTimeout: Duration,
  topicOptions: Map[String, String])(implicit keySerializer: Serializer[K], valueSerializer: Serializer[V])
    extends Store[K, V]
    with CloseOnce {

  private[this] val log = Logger(classOf[TopicStore[_, _]])

  /**
    * The ohara configurator is a distributed services. Hence, we need a uuid for each configurator in order to distinguish the records.
    * TODO: make sure this uuid is unique in a distributed cluster. by chia
    */
  val uuid: String = UuidUtil.uuid()

  implicit val executor: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

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
    Consumer.builder().topicName(topicName).brokers(brokers).offsetFromBegin().groupId(uuid).build[K, V]
  }

  private[this] val producer = newOrClose {
    Producer.builder().brokers(brokers).build[K, V]
  }

  private[this] val consumerNotifier = new Object
  private[this] val commitResult = new ConcurrentHashMap[String, Promise[Option[V]]]()
  private[this] val cache: Store[K, V] = newOrClose(Store.inMemory(keySerializer, valueSerializer))

  /**
    * true if poller haven't grab any data recently.
    */
  private[this] val poller = Future[Unit] {
    try {
      while (!this.isClosed) {
        try {
          val records = consumer.poll(pollTimeout)
          records
            .withFilter(_.topic == topicName)
            .withFilter(_.headers.size == 1)
            .foreach(record => {
              record.key.foreach(k => {
                val index = record.headers.head.key
                // only handle the value from this topic store
                val p: Promise[Option[V]] = if (index.startsWith(uuid)) commitResult.remove(index) else null
                // If no value exist, remove the key. Otherwise, update the value mapped to the key
                record.value.fold(cache.remove(k, Consistency.NONE))(cache.update(k, _, Consistency.NONE)).onComplete {
                  case Success(previous) =>
                    if (p != null) {
                      p.success(previous)
                      consumerNotifier.synchronized {
                        consumerNotifier.notifyAll()
                      }
                    }
                  case Failure(exception) =>
                    if (p != null) {
                      p.failure(exception)
                      consumerNotifier.synchronized {
                        consumerNotifier.notifyAll()
                      }
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
    } finally TopicStore.this.close()
  }

  private[this] def createHeader() = Header(s"$uuid-${headerIndexer.getAndIncrement().toString}", Array.emptyByteArray)
  private[this] def send(key: K, value: V, sync: Consistency): Future[Option[V]] = {
    val header = createHeader()
    val promise = Promise[Option[V]]
    sync match {
      case STRICT =>
        producer.sender().header(header).key(key).value(value).send(topicName)
        commitResult.put(header.key, promise)
      case WEAK =>
        producer
          .sender()
          .header(header)
          .key(key)
          .value(value)
          .send(
            topicName, {
              case Left(e) => promise.failure(e)
              // it is ok to call blocking method since the cache is in-memory.
              case Right(_) => promise.success(Await.result(cache.get(key), 5 seconds))
            }
          )
      case NONE =>
        producer.sender().header(header).key(key).value(value).send(topicName)
        // it is ok to call blocking method since the cache is in-memory.
        promise.success(Await.result(cache.get(key), 5 seconds))
    }
    promise.future
  }

  override def update(key: K, value: V, sync: Consistency): Future[Option[V]] = send(key, value, sync)

  override def get(key: K): Future[Option[V]] = cache.get(key)

  override def remove(key: K, sync: Consistency): Future[Option[V]] = send(key, null.asInstanceOf[V], sync)

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

  override def exist(key: K): Future[Boolean] = cache.exist(key)
}
