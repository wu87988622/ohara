package com.island.ohara.integration

import java.util
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue, TimeUnit}
import java.util.{Objects, Random}

import com.island.ohara.config.{OharaConfig, OharaJson}
import com.island.ohara.io.CloseOnce
import com.island.ohara.rest.{RestClient, RestResponse}
import kafka.server.KafkaServer
import org.apache.hadoop.fs.FileSystem
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.connect.converters.ByteArrayConverter
import org.apache.kafka.connect.runtime.rest.RestServer
import org.apache.kafka.connect.runtime.{ConnectorConfig, Worker, WorkerConfig}
import org.apache.kafka.connect.sink.SinkConnector
import org.apache.kafka.connect.source.SourceConnector

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

/**
  * This class create a kafka services having 1 zk instance and 1 broker default. Also, this class have many helper methods to make
  * test more friendly.
  *
  * How to use this class:
  * 1) create the OharaTestUtil with 1 broker (you can assign arbitrary number of brokers)
  * val testUtil = new OharaTestUtil(1)
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
  * @param brokerCount brokers count
  */
//TODO The dataNodeCount doesn't implement at present
class OharaTestUtil(brokerCount: Int = 1, workerCount: Int = 1, dataNodeCount: Int = 1) extends CloseOnce {
  @volatile private[this] var stopConsumer = false
  private[this] val consumerThreads = new ArrayBuffer[Future[_]]()
  private[this] val zk = new LocalZk()
  private[this] val localBrokerCluster = new LocalKafkaBrokers(zk.connection, ports(brokerCount))
  private[this] val localWorkerCluster = new LocalKafkaWorkers(localBrokerCluster.brokersString, ports(workerCount))
  private[this] val localHDFSCluster = OharaTestUtil.localHDFS(dataNodeCount)

  private[this] def ports(brokers: Int): Seq[Int] = for (_ <- 0 until brokers) yield -1

  private[this] val restClient = RestClient()

  /**
    * Generate the basic config. The config is composed of following setting.
    * 1) bootstrap.servers
    * @return a basic config including the brokers information
    */
  def config: OharaConfig = localBrokerCluster.config

  /**
    * Generate a config for kafka producer. The config is composed of following setting.
    * 1) bootstrap.servers
    * @return a config used to instantiate kafka producer
    */
  def producerConfig: OharaConfig = localBrokerCluster.producerConfig

  /**
    * Generate a config for kafka consumer. The config is composed of following setting.
    * 1) bootstrap.servers
    * 2) group.id -> a arbitrary string
    * 3) auto.offset.reset -> earliest
    * @return a config used to instantiate kafka consumer
    */
  def consumerConfig: OharaConfig = localBrokerCluster.consumerConfig

  /**
    * @return zookeeper connection used to create zk services
    */
  def zkConnection: String = zk.connection

  /**
    * @return a list of running brokers
    */
  def kafkaBrokers: Seq[KafkaServer] = localBrokerCluster.brokers

  /**
    * @return a list of running brokers
    */
  def kafkaWorkers: Seq[Worker] = localWorkerCluster.workers

  /**
    * @return a list of running brokers
    */
  def kafkaRestServers: Seq[RestServer] = localWorkerCluster.restServers

  /**
    * Exposing the brokers connection. This list should be in the form <code>host1:port1,host2:port2,...</code>.
    *
    * @return brokers connection information
    */
  def brokersString: String = localBrokerCluster.brokersString

  import scala.concurrent.duration._

  /**
    * Create the topic and wait the procedure to succeed
    *
    * @param topic topic name
    */
  def createTopic(topic: String): Unit = {
    CloseOnce.doClose(AdminClient.create(config.toProperties))(admin =>
      admin.createTopics(util.Arrays.asList(new NewTopic(topic, 1, 1))))
    if (!await(() => exist(topic), 10 second))
      throw new IllegalStateException(
        s"$topic isn't created successfully after 10 seconds. Perhaps we should increase the wait time?")
  }

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
    if (seekToBegin) config.set(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    else config.set(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
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
      def updateQueue = (records: ConsumerRecords[K, V]) => {
        if (records != null) {
          records.forEach((record: ConsumerRecord[K, V]) => {
            keyQueue.put(record.key)
            valueQueue.put(record.value)
          })
        }
      }

      try {
        while (!stopConsumer) {
          updateQueue(consumer.poll(pollTimeout))
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
    * @return a source connector builder.
    */
  def sourceConnectorCreator() = new SourceConnectorCreator() {
    private[this] var name: String = null
    private[this] var clz: Class[_ <: SourceConnector] = null
    private[this] var topicName: String = null
    private[this] var taskMax: Int = -1
    private[this] var config: Map[String, String] = null
    private[this] var _disableConverter: Boolean = false
    override def name(name: String): SourceConnectorCreator = { this.name = name; this }
    override def connectorClass(clz: Class[_ <: SourceConnector]): SourceConnectorCreator = { this.clz = clz; this }
    override def topic(topicName: String): SourceConnectorCreator = { this.topicName = topicName; this }
    override def taskNumber(taskMax: Int): SourceConnectorCreator = { this.taskMax = taskMax; this }
    override def config(config: Map[String, String]): SourceConnectorCreator = { this.config = config; this }
    private[this] def checkArgument(): Unit = {
      Objects.requireNonNull(name)
      Objects.requireNonNull(clz)
      Objects.requireNonNull(topicName)
      if (taskMax <= 0) throw new IllegalArgumentException(s"taskMax should be bigger than zero, current:$taskMax")
    }

    override def run(): RestResponse = {
      checkArgument()
      val request = OharaConfig()
      val connectConfig = new mutable.HashMap[String, String]
      if (config != null) connectConfig ++= config
      request.set("name", name)
      connectConfig.put(ConnectorConfig.CONNECTOR_CLASS_CONFIG, clz.getName)
      connectConfig.put("topic", topicName)
      connectConfig.put(ConnectorConfig.TASKS_MAX_CONFIG, taskMax.toString)
      if (_disableConverter) {
        connectConfig.put(WorkerConfig.KEY_CONVERTER_CLASS_CONFIG, classOf[ByteArrayConverter].getName)
        connectConfig.put(WorkerConfig.VALUE_CONVERTER_CLASS_CONFIG, classOf[ByteArrayConverter].getName)
      }
      request.set("config", connectConfig.toMap)
      requestToConnector("connectors", request.toJson.toString)
    }

    override def disableConverter: SourceConnectorCreator = { _disableConverter = true; this }

  }

  /**
    * @return a sink connector builder.
    */
  def sinkConnectorCreator() = new SinkConnectorCreator() {
    private[this] var name: String = null
    private[this] var clz: Class[_ <: SinkConnector] = null
    private[this] var topicNames: Seq[String] = null
    private[this] var taskMax: Int = -1
    private[this] var config: Map[String, String] = null
    private[this] var _disableConverter: Boolean = false
    override def name(name: String): SinkConnectorCreator = { this.name = name; this }
    override def connectorClass(clz: Class[_ <: SinkConnector]): SinkConnectorCreator = { this.clz = clz; this }
    override def topics(topicNames: Seq[String]): SinkConnectorCreator = { this.topicNames = topicNames; this }
    override def taskNumber(taskMax: Int): SinkConnectorCreator = { this.taskMax = taskMax; this }
    override def config(config: Map[String, String]): SinkConnectorCreator = { this.config = config; this }
    private[this] def checkArgument(): Unit = {
      Objects.requireNonNull(name)
      Objects.requireNonNull(clz)
      Objects.requireNonNull(topicNames)
      if (topicNames.isEmpty) throw new IllegalArgumentException(s"You must specify 1+ topic names")
      if (taskMax <= 0) throw new IllegalArgumentException(s"taskMax should be bigger than zero, current:$taskMax")
    }
    override def run(): RestResponse = {
      checkArgument()
      val request = OharaConfig()
      val connectConfig = new mutable.HashMap[String, String]
      if (config != null) connectConfig ++= config
      request.set("name", name)
      connectConfig.put(ConnectorConfig.CONNECTOR_CLASS_CONFIG, clz.getName)
      val topicString = StringBuilder.newBuilder
      topicNames.foreach(topicString.append(_).append(","))
      connectConfig.put("topics", topicString.substring(0, topicString.length - 1))
      connectConfig.put(ConnectorConfig.TASKS_MAX_CONFIG, taskMax.toString)
      if (_disableConverter) {
        connectConfig.put(WorkerConfig.KEY_CONVERTER_CLASS_CONFIG, classOf[ByteArrayConverter].getName)
        connectConfig.put(WorkerConfig.VALUE_CONVERTER_CLASS_CONFIG, classOf[ByteArrayConverter].getName)
      }
      request.set("config", connectConfig.toMap)
      requestToConnector("connectors", request.toJson.toString)
    }

    override def disableConverter: SinkConnectorCreator = { _disableConverter = true; this }
  }

  /**
    * Get to HDFS FileSystem
    *
    * @return
    */
  def hdfsFileSystem(): FileSystem = localHDFSCluster.fileSystem()

  /**
    *Get to temp dir path
    *
    * @return
    */
  def hdfsTempDir(): String = localHDFSCluster.tmpDirPath()

  /**
    * GET to kafka connectors
    *
    * @param cmd command
    * @return response content
    */
  private[this] def request(cmd: String): RestResponse = {
    val url = localWorkerCluster.pickRandomRestServer().advertisedUrl()
    restClient.get(url.getHost, url.getPort, cmd)
  }

  /**
    * POST to kafka connectors
    *
    * @param cmd      command
    * @param jsonBody payload
    * @return response content
    */
  private[this] def requestToConnector(cmd: String, jsonBody: String): RestResponse = {
    val url = localWorkerCluster.pickRandomRestServer().advertisedUrl()
    restClient.post(url.getHost, url.getPort, cmd, OharaJson(jsonBody))
  }

  override protected def doClose(): Unit = {
    stopConsumer = true
    restClient.close()
    consumerThreads.foreach(Await.result(_, 1 minute))
    consumerThreads.clear()
    localWorkerCluster.close()
    localBrokerCluster.close()
    localHDFSCluster.close()
    zk.close()
  }
}

object OharaTestUtil {
  def localHDFS(numOfNode: Int): LocalHDFS = LocalHDFS(numOfNode)
}
