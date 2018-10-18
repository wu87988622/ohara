package com.island.ohara.integration

import java.io.File
import java.util
import java.util.Properties
import java.util.concurrent.TimeUnit

import com.island.ohara.client.ConnectorClient
import com.island.ohara.io.CloseOnce
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.util.SystemUtil
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.{AdminClient, NewTopic}
import org.apache.kafka.common.KafkaFuture

import scala.concurrent.duration._

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
  private[this] var localDb: LocalDataBase = _
  private[this] var _connectorClient: ConnectorClient = _
  private[this] var localFtpServer: FtpServer = _

  /**
    * NOTED: DON'T close this object since we share the same object with other threads.
    */
  private[this] var localFs: FileSystem = _
  private[this] var _tmpDirectory: File = _

  /**
    * Exposing the brokers connection. This list should be in the form <code>host1:port1,host2:port2,...</code>.
    *
    * @return brokers connection information
    */
  def brokers: String = componentBox.brokerCluster.brokers

  /**
    * Exposing the workers connection. This list should be in the form <code>host1:port1,host2:port2,...</code>.
    *
    * @return workers connection information
    */
  def workers: String = componentBox.workerCluster.workers

  import scala.concurrent.duration._

  private[this] def kafkaAdmin(): AdminClient = {
    val props = new Properties()
    props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
    AdminClient.create(props)
  }

  /**
    * Create the topic and wait the procedure to succeed
    *
    * @param topic topic name
    */
  def createTopic(topic: String): Unit = {
    CloseOnce.doClose(kafkaAdmin())(admin => admin.createTopics(util.Arrays.asList(new NewTopic(topic, 1, 1))))
    if (!OharaTestUtil.await(() => exist(topic), 10 second))
      throw new IllegalStateException(
        s"$topic isn't created successfully after 10 seconds. Perhaps we should increase the wait time?")
  }

  /**
    * @param topic topic name
    * @return true if the topic exists
    */
  def exist(topic: String): Boolean = CloseOnce.doClose(kafkaAdmin())(
    admin =>
      admin
        .listTopics()
        .names()
        .thenApply(new KafkaFuture.Function[util.Set[String], Boolean] {
          override def apply(a: util.Set[String]): Boolean = a.contains(topic)
        })
        .get())

  import scala.collection.JavaConverters._

  /**
    * topic name and partition infos
    *
    * @param topic topic name
    * @return a pair of topic name and partition number
    */
  def partitions(topic: String): (String, Array[Int]) = CloseOnce.doClose(kafkaAdmin()) { admin =>
    {
      val desc = admin.describeTopics(util.Arrays.asList(topic)).all().get().get(topic)
      (desc.name(), desc.partitions().asScala.map(_.partition()).toArray)
    }
  }

  def connectorClient: ConnectorClient = {
    // throw exception if there is no worker cluster
    workers
    if (_connectorClient == null) _connectorClient = ConnectorClient(workers)
    _connectorClient
  }

  /**
    * Get to HDFS FileSystem
    *
    * @return
    */
  def fileSystem: FileSystem = {
    if (localFs == null) localFs = FileSystem.getLocal(new Configuration())
    localFs
  }

  /**
    *Get to temp dir path
    *
    * @return
    */
  def tmpDirectory: String = {
    if (_tmpDirectory == null) _tmpDirectory = createTempDir(this.getClass.getSimpleName)
    _tmpDirectory.getAbsolutePath
  }

  def dataBase: LocalDataBase = {
    if (localDb == null) localDb = LocalDataBase.mysql()
    localDb
  }

  def ftpServer: FtpServer = {
    if (localFtpServer == null) localFtpServer = FtpServer()
    localFtpServer
  }

  override protected def doClose(): Unit = {
    CloseOnce.close(_connectorClient)
    componentBox.close()
    CloseOnce.close(localDb)
    CloseOnce.close(localFtpServer)
    if (_tmpDirectory != null) deleteFile(_tmpDirectory)
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
  def await(f: () => Boolean, d: Duration, freq: Duration = 500 millis, useException: Boolean = true): Boolean = {
    val startTs = SystemUtil.current()
    while (d.toMillis >= (System.currentTimeMillis() - startTs)) {
      if (f()) return true
      else TimeUnit.MILLISECONDS.sleep(freq.toMillis)
    }
    if (useException) throw new IllegalStateException("timeout") else false
  }

  /**
    * Create a test util with multi-brokers.
    * NOTED: don't call the worker and hdfs service. otherwise you will get exception
    *
    * @param numberOfBrokers the number of brokers you want to run locally
    * @return a test util
    */
  def localBrokers(numberOfBrokers: Int) = new OharaTestUtil(new ComponentBox(numberOfBrokers, -1))

  /**
    * Create a test util with multi-brokers and multi-workers.
    * NOTED: don't call the hdfs service. otherwise you will get exception
    *
    * @param numberOfBrokers the number of brokers you want to run locally
    * @param numberOfWorkers the number of workers you want to run locally
    * @return a test util
    */
  def localWorkers(numberOfBrokers: Int, numberOfWorkers: Int) = new OharaTestUtil(
    new ComponentBox(numberOfBrokers, numberOfWorkers))

  /**
    * Create a test util with local file system.
    * NOTED: don't call the workers and brokers service. otherwise you will get exception
    *
    * @return a test util
    */
  def localHDFS(): OharaTestUtil = new OharaTestUtil(new ComponentBox(-1, -1))

  val HELP_KEY = "--help"
  val TTL_KEY = "--ttl"
  val USAGE = s"[Usage] $TTL_KEY"

  def main(args: Array[String]): Unit = {
    if (args.length == 1 && args(0) == HELP_KEY) {
      println(USAGE)
      return
    }
    if (args.length % 2 != 0) throw new IllegalArgumentException(USAGE)
    var ttl = 9999
    args.sliding(2, 2).foreach {
      case Array(TTL_KEY, value) => ttl = value.toInt
      case _                     => throw new IllegalArgumentException(USAGE)
    }
    doClose(OharaTestUtil.localWorkers(3, 3)) { util =>
      println("wait for the mini kafka cluster")
      TimeUnit.SECONDS.sleep(5)
      println(s"Succeed to run the mini brokers: ${util.brokers} and workers:${util.workers}")
      println(
        s"enter ctrl+c to terminate the mini broker cluster (or the cluster will be terminated after $ttl seconds")
      TimeUnit.SECONDS.sleep(ttl)
    }
  }
}

private[integration] class ComponentBox(numberOfBrokers: Int, numberOfWorkers: Int) extends CloseOnce {
  private[this] def ports(brokers: Int): Seq[Int] = for (_ <- 0 until brokers) yield -1
  private[this] val zk = if (numberOfBrokers > 0) newOrClose(new LocalZk()) else null
  private[this] val localBrokerCluster =
    if (numberOfBrokers > 0) newOrClose(new LocalKafkaBrokers(zk.connection, ports(numberOfBrokers))) else null
  private[this] val localWorkerCluster =
    if (numberOfWorkers > 0) newOrClose(new LocalKafkaWorkers(localBrokerCluster.brokers, ports(numberOfWorkers)))
    else null

  def brokerCluster: LocalKafkaBrokers = require(localBrokerCluster, "You haven't started brokers")
  def workerCluster: LocalKafkaWorkers = require(localWorkerCluster, "You haven't started workers")

  private[this] def require[T](obj: T, message: String) =
    if (obj == null) throw new NullPointerException(message) else obj
  override protected def doClose(): Unit = {
    CloseOnce.close(localWorkerCluster)
    CloseOnce.close(localBrokerCluster)
    CloseOnce.close(zk)
  }
}
