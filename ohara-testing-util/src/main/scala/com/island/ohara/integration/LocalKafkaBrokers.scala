package com.island.ohara.integration

import java.util.Properties

import com.island.ohara.io.{CloseOnce, IoUtil}
import com.typesafe.scalalogging.Logger
import kafka.server.{KafkaConfig, KafkaServer}
import org.apache.kafka.common.network.ListenerName
import org.apache.kafka.common.utils.SystemTime

/**
  * Mini brokers service. The log dir is under {TMP}/kafka-local/ and the hostname is "IoUtil.hostname". The offset topic is configured with
  * no replication and single partition in order to speedup the startup.
  *
  * @param zkConnection   zookeeper connection.
  * @param ports         the port to bind. default is a random number
  * @param baseConfig the properties passed to brokers
  */
class LocalKafkaBrokers private[integration] (zkConnection: String,
                                              ports: Seq[Int],
                                              baseConfig: Map[String, String] = Map.empty)
    extends CloseOnce {
  private[this] val logger: Logger = Logger(classOf[LocalKafkaBrokers])

  private[this] val brokerAndLogDir = ports.zipWithIndex.map {
    case (port: Int, index: Int) =>
      val logDir = createTempDir("kafka-local")
      val config = new Properties()
      // reduce the number of partitions and replicas to speedup the mini cluster
      config.setProperty(KafkaConfig.OffsetsTopicPartitionsProp, 1.toString)
      config.setProperty(KafkaConfig.OffsetsTopicReplicationFactorProp, 1.toString)
      config.setProperty(KafkaConfig.ZkConnectProp, zkConnection)
      config.setProperty(KafkaConfig.BrokerIdProp, String.valueOf(index + 1))
      config.setProperty(KafkaConfig.ListenersProp, "PLAINTEXT://:" + (if (port <= 0) 0 else port))
      config.setProperty(KafkaConfig.LogDirProp, logDir.getAbsolutePath)
      // increase the timeout in order to avoid ZkTimeoutException
      config.setProperty(KafkaConfig.ZkConnectionTimeoutMsProp, (30 * 1000).toString)
      baseConfig.foreach {
        case (k, v) => config.setProperty(k, v)
      }

      val broker = new KafkaServer(new KafkaConfig(config), new SystemTime)
      broker.startup()
      (broker, logDir, broker.boundPort(new ListenerName("PLAINTEXT")))
  }
  val kafkaBrokers: Seq[KafkaServer] = brokerAndLogDir.map(_._1)
  val validPorts: Seq[Int] = brokerAndLogDir.map(_._3)

  val brokers: String = validPorts.map(p => s"${IoUtil.hostname}:$p").mkString(",")
  logger.info(s"ports used in LocalKafkaBrokers are $brokers")

  override def toString: String = {
    val sb = new StringBuilder("LocalKafka{")
    sb.append("brokerList='").append(brokers).append('\'')
    sb.append('}')
    sb.toString
  }

  override protected def doClose(): Unit = {
    kafkaBrokers.foreach(s => {
      s.shutdown()
      s.awaitShutdown()
    })
    brokerAndLogDir.map(_._2).foreach(deleteFile)
  }
}
