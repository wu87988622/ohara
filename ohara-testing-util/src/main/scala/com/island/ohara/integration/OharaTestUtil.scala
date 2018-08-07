package com.island.ohara.integration

import java.util
import java.util.Random
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue, TimeUnit}

import com.island.ohara.config.{OharaConfig, OharaJson}
import com.island.ohara.io.CloseOnce
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.rest.{RestClient, RestResponse}
import kafka.server.KafkaServer
import org.apache.hadoop.fs.FileSystem
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.connect.runtime.Worker
import org.apache.kafka.connect.runtime.rest.RestServer

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * This class create a kafka services having 1 zk instance and 1 broker default. Also, this class have many helper methods to make
  * test more friendly.
  *
  * How to use this class:
  * 1) create the OharaTestUtil with 1 broker (you can assign arbitrary number of brokers)
  * val testUtil = OharaTestUtil.localBrokers(1)
  * 2) get the basic|producer|consumer OharaConfiguration
  * val config = testUtil.producerConfig
  * 3) instantiate your producer or consumer
  * val producer = new KafkaProducer<Array<Byte>, Array<Byte>>(config, new ByteArraySerializer, new ByteArraySerializer)
  * 4) do what you want for your producer and consumer
  * ...
  * 5) close OharaTestUtil
  * testUtil.close()
  *
  * see TestOharaTestUtil for more examples
  * NOTED: the close() will shutdown all services including the passed consumers (see run())
  *
  */
class OharaTestUtil private[integration] (componentBox: ComponentBox) extends CloseOnce {
  @volatile private[this] var stopConsumer = false
  private[this] val consumerThreads = new ArrayBuffer[Future[_]]()
  private[this] val restClient = RestClient()

  /**
    * Generate the basic config. The config is composed of following setting.
    * 1) bootstrap.servers
    *
    * @return a basic config including the brokers information
    */
  def config: OharaConfig = componentBox.brokers.config

  /**
    * Generate a config for kafka producer. The config is composed of following setting.
    * 1) bootstrap.servers
    *
    * @return a config used to instantiate kafka producer
    */
  def producerConfig: OharaConfig = componentBox.brokers.producerConfig

  /**
    * Generate a config for kafka consumer. The config is composed of following setting.
    * 1) bootstrap.servers
    * 2) group.id -> a arbitrary string
    * 3) auto.offset.reset -> earliest
    *
    * @return a config used to instantiate kafka consumer
    */
  def consumerConfig: OharaConfig = componentBox.brokers.consumerConfig

  /**
    * @return zookeeper connection used to create zk services
    */
  def zkConnection: String = componentBox.zookeeper.connection

  /**
    * @return a list of running brokers
    */
  def kafkaBrokers: Seq[KafkaServer] = componentBox.brokers.brokers

  /**
    * @return a list of running brokers
    */
  def kafkaWorkers: Seq[Worker] = componentBox.workers.workers

  /**
    * @return a list of running brokers
    */
  def kafkaRestServers: Seq[RestServer] = componentBox.workers.restServers

  /**
    * Exposing the brokers connection. This list should be in the form <code>host1:port1,host2:port2,...</code>.
    *
    * @return brokers connection information
    */
  def brokersString: String = componentBox.brokers.brokersString

  /**
    * Exposing the workers connection. This list should be in the form <code>host1:port1,host2:port2,...</code>.
    *
    * @return workers connection information
    */
  def workersString: String = componentBox.workers.workersString

  import scala.concurrent.duration._

  /**
    * Create the topic and wait the procedure to succeed
    *
    * @param topic topic name
    */
  def createTopic(topic: String): Unit = {
    CloseOnce.doClose(AdminClient.create(config.toProperties))(admin =>
      admin.createTopics(util.Arrays.asList(new NewTopic(topic, 1, 1))))
    if (!OharaTestUtil.await(() => exist(topic), 10 second))
      throw new IllegalStateException(
        s"$topic isn't created successfully after 10 seconds. Perhaps we should increase the wait time?")
  }

  /**
    * @param topic topic name
    * @return true if the topic exists
    */
  def exist(topic: String): Boolean = CloseOnce.doClose(AdminClient.create(config.toProperties))(admin =>
    admin.listTopics().names().thenApply(_.stream().anyMatch(_.equals(topic))).get())

  import scala.collection.JavaConverters._

  /**
    * topic name and partition infos
    *
    * @param topic topic name
    * @return a pair of topic name and partition number
    */
  def partitions(topic: String): (String, Array[Int]) = CloseOnce.doClose(AdminClient.create(config.toProperties)) {
    admin =>
      {
        val desc = admin.describeTopics(util.Arrays.asList(topic)).all().get().get(topic)
        (desc.name(), desc.partitions().asScala.map(_.partition()).toArray)
      }
  }

  /**
    * Run a consumer with specified deserializer, and all received data will be stored to queue.
    *
    * @param topic           topic to subscribe
    * @param seekToBegin true if you want to reset the offset
    * @param keySerializer   key serializer
    * @param valueSerializer value serializer
    * @tparam K type of key
    * @tparam V type of value
    * @return a pair of blocking queue storing the data of key and value
    */
  def run[K, V](topic: String,
                seekToBegin: Boolean,
                keySerializer: Deserializer[K],
                valueSerializer: Deserializer[V]): (BlockingQueue[K], BlockingQueue[V]) = {
    val config = this.config
    config.set(ConsumerConfig.GROUP_ID_CONFIG, s"console-consumer-${new Random().nextInt(100000)}")
    if (seekToBegin) config.set(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.name.toLowerCase)
    else config.set(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.LATEST.name.toLowerCase)
    val consumer = new KafkaConsumer[K, V](config.toProperties, keySerializer, valueSerializer)
    consumer.subscribe(util.Arrays.asList(topic))
    run(consumer)
  }

  /**
    * Run a existed consumer with specified deserializer, and all received data will be stored to queue.
    * NOTED: kafka consumer is not thread-safe so please don't use the consumer after passing it to this method. The passed consumers
    * will be closed automatically.
    *
    * @param consumer    the consumer to run background
    * @param pollTimeout timeout to poll
    * @tparam K key type
    * @tparam V value type
    * @return a pair of blocking queue storing the data of key and value
    */
  def run[K, V](consumer: KafkaConsumer[K, V], pollTimeout: Int = 1000): (BlockingQueue[K], BlockingQueue[V]) = {
    val keyQueue = new LinkedBlockingQueue[K](100)
    val valueQueue = new LinkedBlockingQueue[V](100)
    val consumerThread = Future {
      try {
        while (!stopConsumer) {
          val records = consumer.poll(pollTimeout)
          if (records != null) {
            records.forEach((record: ConsumerRecord[K, V]) => {
              if (record.key != null) keyQueue.put(record.key)
              if (record.value != null) valueQueue.put(record.value)
            })
          }
        }
      } finally consumer.close()
    }
    consumerThreads += consumerThread
    (keyQueue, valueQueue)
  }

  /**
    * Send the Get request to list the running connectors
    *
    * @return http response in json format
    */
  def runningConnectors(): RestResponse = request("connectors")

  /**
    * Send the Get request to list the available connectors
    *
    * @return http response in json format
    */
  def availableConnectors(): RestResponse = request("connector-plugins")

  /**
    * @return the public address and port of a worker picked by random
    */
  def pickWorkerAddress(): (String, Int) = {
    val s = componentBox.workers.pickRandomRestServer().advertisedUrl()
    (s.getHost, s.getPort)
  }

  /**
    * @return the public address and port of a broker picked by random
    */
  def pickBrokerAddress(): (String, Int) = {
    val s = componentBox.workers.pickRandomRestServer().advertisedUrl()
    (s.getHost, s.getPort)
  }

  /**
    * @return a source connector builder.
    */
  def sourceConnectorCreator(): SourceConnectorCreator = (cmd: String, body: OharaJson) => {
    val (host, port) = pickWorkerAddress()
    restClient.post(host, port, cmd, body)
  }

  /**
    * @return a sink connector builder.
    */
  def sinkConnectorCreator(): SinkConnectorCreator = (cmd: String, body: OharaJson) => {
    val (host, port) = pickWorkerAddress()
    restClient.post(host, port, cmd, body)
  }

  /**
    * Get to HDFS FileSystem
    *
    * @return
    */
  def fileSystem(): FileSystem = componentBox.hdfs.fileSystem()

  /**
    *Get to temp dir path
    *
    * @return
    */
  def tmpDirectory(): String = componentBox.hdfs.tmpDirectory()

  /**
    * GET to kafka connectors
    *
    * @param cmd command
    * @return response content
    */
  private[this] def request(cmd: String): RestResponse = {
    val (host, port) = pickWorkerAddress()
    restClient.get(host, port, cmd)
  }

  override protected def doClose(): Unit = {
    stopConsumer = true
    CloseOnce.close(restClient)
    CloseOnce.release(() => consumerThreads.foreach(Await.result(_, 1 minute)))
    consumerThreads.clear()
    componentBox.close()
  }

}

object OharaTestUtil {

  /**
    * helper method. Loop the specified method until timeout or get true from method
    *
    * @param f            function
    * @param d            duration
    * @param freq         frequency to call the method
    * @param useException true make this method throw exception after timeout.
    * @return false if timeout and (useException = true). Otherwise, the return value is true
    */
  def await(f: () => Boolean, d: Duration, freq: Int = 100, useException: Boolean = true): Boolean = {
    val startTs = System.currentTimeMillis()
    while (d.toMillis >= (System.currentTimeMillis() - startTs)) {
      if (f()) return true
      else TimeUnit.MILLISECONDS.sleep(freq)
    }
    if (useException) throw new IllegalStateException("timeout") else false
  }

  def builder = new OharaTestUtilBuilder()

  /**
    * Create a test util with multi-brokers.
    * NOTED: don't call the worker and hdfs service. otherwise you will get exception
    *
    * @param numberOfBrokers the number of brokers you want to run locally
    * @return a test util
    */
  def localBrokers(numberOfBrokers: Int) = new OharaTestUtil(new ComponentBox(numberOfBrokers, -1, -1))

  /**
    * Create a test util with multi-brokers and multi-workers.
    * NOTED: don't call the hdfs service. otherwise you will get exception
    *
    * @param numberOfBrokers the number of brokers you want to run locally
    * @param numberOfWorkers the number of workers you want to run locally
    * @return a test util
    */
  def localWorkers(numberOfBrokers: Int, numberOfWorkers: Int) = new OharaTestUtil(
    new ComponentBox(numberOfBrokers, numberOfWorkers, -1))

  /**
    * Create a test util with single namenode and multi-datanode
    * NOTED: don't call the workers and brokers service. otherwise you will get exception
    *
    * @param numOfNode the number of data nodes you want to run locally
    * @return a test util
    */
  def localHDFS(numOfNode: Int): OharaTestUtil = new OharaTestUtil(new ComponentBox(-1, -1, numOfNode))

  val HELP_KEY = "--help"
  val TTL_KEY = "--ttl"
  val USAGE = s"[Usage] $TTL_KEY"

  def main(args: Array[String]): Unit = {
    if (args.length == 1 && args(0).equals(HELP_KEY)) {
      println(USAGE)
      return
    }
    if (args.size % 2 != 0) throw new IllegalArgumentException(USAGE)
    var ttl = 9999
    args.sliding(2, 2).foreach {
      case Array(TTL_KEY, value) => ttl = value.toInt
      case _                     => throw new IllegalArgumentException(USAGE)
    }
    doClose(OharaTestUtil.localWorkers(3, 3)) { util =>
      println("wait for the mini kafka cluster")
      TimeUnit.SECONDS.sleep(5)
      println(s"Succeed to run the mini brokers: ${util.brokersString} and workers:${util.workersString}")
      println(
        s"enter ctrl+c to terminate the mini broker cluster (or the cluster will be terminated after ${ttl} seconds")
      TimeUnit.SECONDS.sleep(ttl)
    }
  }
}

private[integration] class ComponentBox(numberOfBrokers: Int, numberOfWorkers: Int, numberOfDataNodes: Int)
    extends CloseOnce {
  private[this] def ports(brokers: Int): Seq[Int] = for (_ <- 0 until brokers) yield -1
  private[this] val zk = if (numberOfBrokers > 0) newOrClose(new LocalZk()) else null
  private[this] val localBrokerCluster =
    if (numberOfBrokers > 0) newOrClose(new LocalKafkaBrokers(zk.connection, ports(numberOfBrokers))) else null
  private[this] val localWorkerCluster =
    if (numberOfWorkers > 0) newOrClose(new LocalKafkaWorkers(localBrokerCluster.brokersString, ports(numberOfWorkers)))
    else null
  private[this] val localHDFSCluster = if (numberOfDataNodes > 0) newOrClose(new LocalHDFS(numberOfDataNodes)) else null

  def zookeeper: LocalZk = require(zk, "You haven't started zookeeper")
  def brokers: LocalKafkaBrokers = require(localBrokerCluster, "You haven't started brokers")
  def workers: LocalKafkaWorkers = require(localWorkerCluster, "You haven't started workers")
  def hdfs: LocalHDFS = require(localHDFSCluster, "You haven't started hdfs")

  private[this] def require[T](obj: T, message: String) =
    if (obj == null) throw new NullPointerException(message) else obj
  override protected def doClose(): Unit = {
    CloseOnce.close(localWorkerCluster)
    CloseOnce.close(localBrokerCluster)
    CloseOnce.close(localHDFSCluster)
    CloseOnce.close(zk)
  }
}

/**
  * As we integrate more services into test util, the constructor of test util will get more complicated.
  * This builder helps us to add services and it handle the dependency between services.
  */
class OharaTestUtilBuilder private[integration] {
  private[this] var numberOfBrokers: Option[Int] = Some(3)
  private[this] var numberOfWorkers: Option[Int] = Some(0)
  private[this] var numberOfDataNodes: Option[Int] = Some(0)

  private[this] def validate(number: Int): Int = if (number <= 0)
    throw new IllegalArgumentException(s"the number:$number should be bigger than zero")
  else number

  /**
    * @param numberOfBrokers the number of brokers you want to run
    * @return this builder
    */
  def numberOfBrokers(numberOfBrokers: Int): OharaTestUtilBuilder = {
    this.numberOfBrokers = Some(validate(numberOfBrokers))
    this
  }

  /**
    * @param numberOfWorkers the number of workers you want to run
    * @return this builder
    */
  def numberOfWorkers(numberOfWorkers: Int): OharaTestUtilBuilder = {
    this.numberOfWorkers = Some(validate(numberOfWorkers))
    // We can't run the workers without brokers
    if (numberOfBrokers.isEmpty) numberOfBrokers(1)
    this
  }

  /**
    * @param numberOfDatanodes the number of data node you want to run
    * @return this builder
    */
  def numberOfDataNodes(numberOfDatanodes: Int): OharaTestUtilBuilder = {
    this.numberOfDataNodes = Some(validate(numberOfDatanodes))
    this
  }

  /**
    * @return a test util with specified services
    */
  def build() = new OharaTestUtil(new ComponentBox(numberOfBrokers.get, numberOfWorkers.get, numberOfDataNodes.get))
}
