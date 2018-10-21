package com.island.ohara.integration
import java.util.Properties

import com.island.ohara.io.{CloseOnce, IoUtil}
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

  def apply(zk: => Zookeepers): Brokers = apply(sys.env.get(BROKER_CONNECTION_PROPS), zk)

  private[integration] def apply(brokers: Option[String], zk: => Zookeepers): Brokers = brokers
    .map { s =>
      new Brokers {
        override def connectionProps: String = s
        override protected def doClose(): Unit = {}
        override def isLocal: Boolean = false
      }
    }
    .getOrElse {
      val brokerAndLogDir = (1 to NUMBER_OF_BROKERS).map { index =>
        val logDir = createTempDir("kafka-local")
        val config = new Properties()
        // reduce the number of partitions and replicas to speedup the mini cluster
        config.setProperty(KafkaConfig.OffsetsTopicPartitionsProp, 1.toString)
        config.setProperty(KafkaConfig.OffsetsTopicReplicationFactorProp, 1.toString)
        config.setProperty(KafkaConfig.ZkConnectProp, zk.connectionProps)
        config.setProperty(KafkaConfig.BrokerIdProp, index.toString)
        config.setProperty(KafkaConfig.ListenersProp, "PLAINTEXT://:0")
        config.setProperty(KafkaConfig.LogDirProp, logDir.getAbsolutePath)
        // increase the timeout in order to avoid ZkTimeoutException
        config.setProperty(KafkaConfig.ZkConnectionTimeoutMsProp, (30 * 1000).toString)
        val broker = new KafkaServer(new KafkaConfig(config), new SystemTime)
        broker.startup()
        (broker, logDir, broker.boundPort(new ListenerName("PLAINTEXT")))
      }

      new Brokers {
        override def connectionProps: String =
          brokerAndLogDir.map(_._3).map(p => s"${IoUtil.hostname}:$p").mkString(",")
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
}
