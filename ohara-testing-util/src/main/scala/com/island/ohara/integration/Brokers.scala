package com.island.ohara.integration
import java.util.Properties

import com.island.ohara.client.util.CloseOnce
import com.island.ohara.common.util.CommonUtil
import kafka.server.{KafkaConfig, KafkaServer}
import org.apache.kafka.common.network.ListenerName
import org.apache.kafka.common.utils.SystemTime

trait Brokers extends CloseOnce {

  /**
    * @return brokers information. the form is "host_a:port_a,host_b:port_b"
    */
  def connectionProps: String

  /**
    * @return true if this broker cluster is generated locally.
    */
  def isLocal: Boolean
}

object Brokers {

  private[integration] val BROKER_CONNECTION_PROPS: String = "ohara.it.brokers"

  private[integration] val NUMBER_OF_BROKERS = 3

  /**
    * create an embedded broker cluster with specific port
    * @param zk zookeepers
    * @param ports bound ports
    * @return an embedded broker cluster
    */
  def local(zk: => Zookeepers, ports: Seq[Int]): Brokers = {
    val brokerAndLogDir = ports.zipWithIndex.map {
      case (port, index) =>
        val logDir = createTempDir("kafka-local")
        val config = new Properties()
        // reduce the number from partitions and replicas to speedup the mini cluster
        config.setProperty(KafkaConfig.OffsetsTopicPartitionsProp, 1.toString)
        config.setProperty(KafkaConfig.OffsetsTopicReplicationFactorProp, 1.toString)
        config.setProperty(KafkaConfig.ZkConnectProp, zk.connectionProps)
        config.setProperty(KafkaConfig.BrokerIdProp, index.toString)
        config.setProperty(KafkaConfig.ListenersProp, "PLAINTEXT://:" + (if (port <= 0) 0 else port))
        config.setProperty(KafkaConfig.LogDirProp, logDir.getAbsolutePath)
        // increase the timeout in order to avoid ZkTimeoutException
        config.setProperty(KafkaConfig.ZkConnectionTimeoutMsProp, (30 * 1000).toString)
        val broker = new KafkaServer(new KafkaConfig(config), new SystemTime)
        broker.startup()
        (broker, logDir, broker.boundPort(new ListenerName("PLAINTEXT")))
    }

    new Brokers {
      override def connectionProps: String =
        brokerAndLogDir.map(_._3).map(p => s"${CommonUtil.hostname}:$p").mkString(",")
      override protected def doClose(): Unit = {
        brokerAndLogDir
          .map(_._1)
          .foreach(s => {
            s.shutdown()
            s.awaitShutdown()
          })
        brokerAndLogDir.map(_._2).foreach(deleteFile)
      }
      override def isLocal: Boolean = true
    }
  }
  def apply(zk: => Zookeepers): Brokers = apply(sys.env.get(BROKER_CONNECTION_PROPS), zk)

  private[integration] def apply(brokers: Option[String], zk: => Zookeepers): Brokers = brokers
    .map { s =>
      new Brokers {
        override def connectionProps: String = s
        override protected def doClose(): Unit = {}
        override def isLocal: Boolean = false
      }
    }
    .getOrElse(local(zk, Seq.fill(NUMBER_OF_BROKERS)(0)))
}
