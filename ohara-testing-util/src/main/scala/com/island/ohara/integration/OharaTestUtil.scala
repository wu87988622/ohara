package com.island.ohara.integration

import java.util
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue, TimeUnit}
import java.util.{Properties, Random}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import com.island.ohara.io.CloseOnce
import com.typesafe.scalalogging.Logger
import kafka.server.KafkaServer
import org.apache.hadoop.fs.FileSystem
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.connect.runtime.Worker
import org.apache.kafka.connect.runtime.rest.RestServer

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
  * 2) get the configured properties
  * val props = testUtil.properties
  * 3) instantiate your producer or consumer
  * val producer = new KafkaProducer[Array[Byte], Array[Byte]](testUtil.properties, new ByteArraySerializer, new ByteArraySerializer)
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
  private[this] lazy val logger = Logger(getClass.getName)
  @volatile private[this] var stopConsumer = false
  private[this] val consumerThreads = new ArrayBuffer[Future[_]]()
  private[this] val zk = new LocalZk()
  private[this] val localBrokerCluster = new LocalKafkaBrokers(zk.connection, ports(brokerCount))
  private[this] val localWorkerCluster = new LocalKafkaWorkers(localBrokerCluster.brokersString, ports(workerCount))
  private[this] val localHDFSCluster = new LocalHDFS(dataNodeCount)

  private[this] def ports(brokers: Int): Seq[Int] = for (_ <- 0 until brokers) yield -1

  /**
    * OharaTestUtil use akka http to submit the GET and POST requests. Since both of ActorSystem and ActorMaterializer are
    * heavyweight structure, creating and putting them in OharaTestUtil is more effective.
    */
  private[this] implicit val actorSystem = ActorSystem("OharaTestUtil-system")

  /**
    * The actorSystem is declared as implicit so it will be used in constructing the ActorMaterializer
    */
  private[this] implicit val actorMaterializer = ActorMaterializer()

  /**
    * Copy the cluster connection information, which includes broker and zookeeper, to an new properties.
    *
    * @return a Properties with brokers info and zookeeper info
    */
  def properties: Properties = localBrokerCluster.properties

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
    CloseOnce.doClose(AdminClient.create(properties))(admin =>
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
  def exist(topic: String): Boolean = CloseOnce.doClose(AdminClient.create(properties))(admin =>
    admin.listTopics().names().thenApply(_.stream().anyMatch(_.equals(topic))).get())

  import scala.collection.JavaConverters._

  /**
    * topic name and partition infos
    *
    * @param topic topic name
    * @return a pair of topic name and partition number
    */
  def partitions(topic: String): (String, Array[Int]) = CloseOnce.doClose(AdminClient.create(properties)) { admin =>
    {
      val desc = admin.describeTopics(util.Arrays.asList(topic)).all().get().get(topic)
      (desc.name(), desc.partitions().asScala.map(_.partition()).toArray)
    }
  }

  /**
    * Run a consumer with specified deserializer, and all received data will be stored to queue.
    *
    * @param topic           topic to subscribe
    * @param keySerializer   key serializer
    * @param valueSerializer value serializer
    * @tparam K type of key
    * @tparam V type of value
    * @return a pair of blocking queue storing the data of key and value
    */
  def run[K, V](topic: String,
                keySerializer: Deserializer[K],
                valueSerializer: Deserializer[V]): (BlockingQueue[K], BlockingQueue[V]) = {
    val props = properties
    props.put("group.id", s"console-consumer-${new Random().nextInt(100000)}")
    val consumer = new KafkaConsumer[K, V](props, keySerializer, valueSerializer)
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
    * @param seekToBegin true if you want to seek to the begin of subscribed topics
    * @tparam K key type
    * @tparam V value type
    * @return a pair of blocking queue storing the data of key and value
    */
  def run[K, V](consumer: KafkaConsumer[K, V],
                pollTimeout: Int = 1000,
                seekToBegin: Boolean = true): (BlockingQueue[K], BlockingQueue[V]) = {
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
        if (seekToBegin) {
          updateQueue(consumer.poll(0))
          consumer.seekToBeginning(consumer.assignment())
        }
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
  def runningConnectors(): String = request("connectors")

  /**
    * Send the Get request to list the available connectors
    *
    * @return http response in json format
    */
  def availableConnectors(): String = request("connector-plugins")

  /**
    * Send the Get request to list the available connectors
    *
    * @param jsonString json payload
    * @return http response in json format
    */
  def startConnector(jsonString: String): (Int, String) = {
    request("connectors", jsonString)
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
  private[this] def request(cmd: String): String = {
    val url = localWorkerCluster.pickRandomRestServer().advertisedUrl().toString + cmd
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = url))
    Await
      .result(responseFuture.flatMap(res => res.entity.toStrict(5 seconds).map(_.data.decodeString("UTF-8"))), 5 second)
  }

  /**
    * POST to kafka connectors
    *
    * @param cmd      command
    * @param jsonBody payload
    * @return response content
    */
  private[this] def request(cmd: String, jsonBody: String): (Int, String) = {
    val url = localWorkerCluster.pickRandomRestServer().advertisedUrl().toString + cmd
    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(
        HttpRequest(
          HttpMethods.POST,
          url,
          entity = HttpEntity(ContentTypes.`application/json`, jsonBody.getBytes("UTF-8"))
        )
      )
    Await.result(
      responseFuture.flatMap(res =>
        res.entity.toStrict(10 seconds).map(strict => (res._1.intValue(), strict.data.decodeString("UTF-8")))),
      10 second)
  }

  override protected def doClose(): Unit = {
    stopConsumer = true
    actorMaterializer.shutdown()
    actorSystem.terminate()
    consumerThreads.foreach(Await.result(_, 1 minute))
    consumerThreads.clear()
    localWorkerCluster.close()
    localBrokerCluster.close()
    localHDFSCluster.close()
    zk.close()
  }
}
