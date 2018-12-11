package com.island.ohara.configurator.store

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, Executors, TimeUnit}

import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{CloseOnce, CommonUtil}
import com.island.ohara.configurator.store.Consistency._
import com.island.ohara.kafka._
import com.typesafe.scalalogging.Logger
import org.apache.kafka.common.errors.{TopicExistsException, WakeupException}

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration.{Duration, _}
import scala.util.{Failure, Success}

private object TopicStore {
  val NUMBER_OF_PARTITIONS: Int = 1
  val NUMBER_OF_REPLICATIONS: Short = 1
  val TIMEOUT: Duration = 30 seconds
  val LOG = Logger(getClass.getName)

  def apply[K, V](brokers: String, topicName: String, pollTimeout: Duration)(
    implicit keySerializer: Serializer[K],
    valueSerializer: Serializer[V]): TopicStore[K, V] = {
    val client = KafkaClient.of(brokers)
    try if (!client.exist(topicName))
      client
        .topicCreator()
        .numberOfPartitions(TopicStore.NUMBER_OF_PARTITIONS)
        .numberOfReplications(TopicStore.NUMBER_OF_REPLICATIONS)
        // enable kafka save the latest message for each key
        .compacted()
        .timeout(java.time.Duration.ofNanos(TopicStore.TIMEOUT.toNanos))
        .create(topicName)
    catch {
      case e: ExecutionException =>
        e.getCause match {
          case _: TopicExistsException => LOG.error(s"$topicName exists but we didn't notice this fact")
          case _                       => if (e.getCause == null) throw e else throw e.getCause
        }
    } finally client.close()

    val uuid = CommonUtil.uuid()
    val producer = Producer.builder().brokers(brokers).build(keySerializer, valueSerializer)

    val consumer =
      try Consumer
        .builder()
        .topicName(topicName)
        .brokers(brokers)
        .offsetFromBegin()
        .groupId(uuid)
        .build(keySerializer, valueSerializer)
      catch {
        case e: Throwable =>
          producer.close()
          throw e
      }
    val cache = try Store.inMemory(keySerializer, valueSerializer)
    catch {
      case e: Throwable =>
        CloseOnce.close(producer)
        CloseOnce.close(consumer)
        throw e
    }
    new TopicStore(topicName, pollTimeout, uuid, producer, consumer, cache)
  }
}

/**
  * This class implements the Store through the kafka topic. The config passed to this class will be added with All data persist in the kafka topic.
  * In order to reduce the seek to topic, this class have a local cache to store the data polled from kafak consumer. The data stored to this class
  * will be sync to kafka topic. It means both update and remove methods invoke the kafka operations.
  *
  * NOTED: This class require the kafka serializer and deserializer. They are used to convert the object to byte array.
  * Without the requried configs, you will fail to instantiate this class
  *
  * NOTED: There are two kind from execution order in this class. 1) the caller order happening in update/remove. 2) data commit order. We take later
  * to complete the consistent order. For example, there are two callers A) and B). A call the update before B. However, the data updated by
  * B is committed before A. So the TopicStore#update will return the data from B to A.
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
private class TopicStore[K, V] private (topicName: String,
                                        pollTimeout: Duration,
                                        // The ohara configurator is a distributed services. Hence, we need a uuid for each configurator in order to distinguish the records.
                                        // TODO: make sure this uuid is unique in a distributed cluster. by chia
                                        uuid: String,
                                        producer: Producer[K, V],
                                        consumer: Consumer[K, V],
                                        cache: Store[K, V])
    extends Store[K, V] {
  import TopicStore._
  implicit val executor: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  /**
    * Used to sort the change to topic. We shouldn't worry about the overflow since it is not update-heavy.
    */
  private[this] val headerIndexer = new AtomicLong(0)

  private[this] val consumerNotifier = new Object
  private[this] val commitResult = new ConcurrentHashMap[String, Promise[Option[V]]]()

  /**
    * true if poller haven't grab any data recently.
    */
  private[this] val poller = Future[Unit] {
    try {
      while (!this.isClosed) {
        try {
          val records = consumer.poll(java.time.Duration.ofNanos(pollTimeout.toNanos))
          records.asScala
            .withFilter(_.topic == topicName)
            .withFilter(_.headers.size == 1)
            .foreach(record => {
              Option(record.key().orElse(null.asInstanceOf[K])).foreach(k => {
                val index = record.headers.get(0).key
                // only handle the value from this topic store
                val p: Promise[Option[V]] = if (index.startsWith(uuid)) commitResult.remove(index) else null
                // If no value exist, remove the key. Otherwise, update the value mapped to the key
                Option(record.value.orElse(null.asInstanceOf[V]))
                  .fold(cache.remove(k, Consistency.NONE))(cache.update(k, _, Consistency.NONE))
                  .onComplete {
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
          case _: WakeupException => LOG.debug("interrupted by ourself")
        }
      }
    } catch {
      case e: Throwable => LOG.error("failure when running the poller", e)
    } finally TopicStore.this.close()
  }

  private[this] def createHeader() =
    new Header(s"$uuid-${headerIndexer.getAndIncrement().toString}", Array.emptyByteArray)
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
            topicName,
            new Sender.Handler[RecordMetadata]() {
              override def doException(e: Exception): Unit = {
                promise.failure(e)
              }

              override def doHandle(o: RecordMetadata): Unit = {
                promise.success(Await.result(cache.get(key), 5 seconds))
              }
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
    if (poller != null) Await.result(poller, 60 seconds)
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
    * @return size from this store
    */
  override def size: Int = cache.size

  override def exist(key: K): Future[Boolean] = cache.exist(key)
}
