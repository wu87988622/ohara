package com.island.ohara.integration

import java.io.File
import java.util.concurrent.TimeUnit

import com.island.ohara.client.ConnectorClient
import com.island.ohara.io.CloseOnce
import com.island.ohara.io.CloseOnce.doClose
import com.island.ohara.util.SystemUtil
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem

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
class OharaTestUtil private[integration] (zk: Zookeepers, brokers: Brokers, workers: Workers) extends CloseOnce {
  private[this] var localDb: Database = _
  private[this] var _connectorClient: ConnectorClient = _
  private[this] var localFtpServer: FtpServer = _
  private[this] var _tmpDirectory: File = _

  /**
    * Exposing the brokers connection. This list should be in the form <code>host1:port1,host2:port2,...</code>.
    *
    * @return brokers connection information
    */
  def brokersConnProps: String = brokers.connectionProps

  /**
    * Exposing the workers connection. This list should be in the form <code>host1:port1,host2:port2,...</code>.
    *
    * @return workers connection information
    */
  def workersConnProps: String = workers.connectionProps

  def connectorClient: ConnectorClient = {
    // throw exception if there is no worker cluster
    if (_connectorClient == null) _connectorClient = ConnectorClient(workers.connectionProps)
    _connectorClient
  }

  /**
    * Get to HDFS FileSystem
    *
    * @return
    */
  def fileSystem: FileSystem = FileSystem.get(new Configuration())

  /**
    *Get to temp dir path
    *
    * @return
    */
  def tmpDirectory: String = {
    if (_tmpDirectory == null) _tmpDirectory = createTempDir(this.getClass.getSimpleName)
    _tmpDirectory.getAbsolutePath
  }

  def dataBase: Database = {
    if (localDb == null) localDb = Database()
    localDb
  }

  def ftpServer: FtpServer = {
    if (localFtpServer == null) localFtpServer = FtpServer()
    localFtpServer
  }

  override protected def doClose(): Unit = {
    CloseOnce.close(_connectorClient)
    CloseOnce.close(localDb)
    CloseOnce.close(localFtpServer)
    if (_tmpDirectory != null) deleteFile(_tmpDirectory)
    CloseOnce.close(workers)
    CloseOnce.close(brokers)
    CloseOnce.close(zk)
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
    * @return a test util
    */
  def brokers(): OharaTestUtil = {
    var zk: Zookeepers = null
    val brokers = Brokers {
      if (zk == null) zk = Zookeepers()
      zk
    }
    new OharaTestUtil(zk, brokers, null)
  }

  /**
    * Create a test util with multi-brokers and multi-workers.
    * NOTED: don't call the hdfs service. otherwise you will get exception
    *
    * @return a test util
    */
  def workers(): OharaTestUtil = {
    var zk: Zookeepers = null
    var brokers: Brokers = null
    val workers = Workers {
      if (brokers == null) brokers = Brokers {
        if (zk == null) zk = Zookeepers()
        zk
      }
      brokers
    }
    new OharaTestUtil(zk, brokers, workers)
  }

  /**
    * Create a test util with local file system.
    * NOTED: don't call the workers and brokers service. otherwise you will get exception
    *
    * @return a test util
    */
  def localHDFS(): OharaTestUtil = new OharaTestUtil(null, null, null)

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
    doClose(OharaTestUtil.workers()) { util =>
      println("wait for the mini kafka cluster")
      TimeUnit.SECONDS.sleep(5)
      println(s"Succeed to run the mini brokers: ${util.brokersConnProps} and workers:${util.workersConnProps}")
      println(
        s"enter ctrl+c to terminate the mini broker cluster (or the cluster will be terminated after $ttl seconds")
      TimeUnit.SECONDS.sleep(ttl)
    }
  }
}
