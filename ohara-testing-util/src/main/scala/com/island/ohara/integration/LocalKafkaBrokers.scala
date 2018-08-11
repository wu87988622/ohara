package com.island.ohara.integration

import java.io.File

import com.island.ohara.config.{OharaConfig, UuidUtil}
import com.island.ohara.io.CloseOnce
import com.typesafe.scalalogging.Logger
import kafka.server.{KafkaConfig, KafkaServer}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.{ConsumerConfig, OffsetResetStrategy}
import org.apache.kafka.common.network.ListenerName
import org.apache.kafka.common.utils.SystemTime

/**
  * Mini brokers service. The log dir is under {TMP}/kafka-local/ and the hostname is "localhost". The offset topic is configured with
  * no replication and single partition in order to speedup the startup.
  *
  * @param zkConnection   zookeeper connection.
  * @param ports         the port to bind. default is a random number
  * @param baseConfig the properties passed to brokers
  */
class LocalKafkaBrokers private[integration] (zkConnection: String,
                                              ports: Seq[Int],
                                              baseConfig: OharaConfig = OharaConfig())
    extends CloseOnce {
  private[this] val logger = Logger(getClass.getName)
  val kafkaBrokers = new Array[KafkaServer](ports.size)
  val logDirs = new Array[File](ports.size)
  val validPorts = ports.zipWithIndex.map {
    case (port: Int, index: Int) => {
      val logDir = createTempDir("kafka-local")
      val config = OharaConfig()
      // reduce the number of partitions and replicas to speedup the mini cluster
      config.set(KafkaConfig.OffsetsTopicPartitionsProp, 1.toString)
      config.set(KafkaConfig.OffsetsTopicReplicationFactorProp, 1.toString)
      config.set(KafkaConfig.ZkConnectProp, zkConnection)
      config.set(KafkaConfig.BrokerIdProp, String.valueOf(index + 1))
      config.set(KafkaConfig.ListenersProp, "PLAINTEXT://:" + (if (port <= 0) 0 else port))
      config.set(KafkaConfig.LogDirProp, logDir.getAbsolutePath())
      // increase the timeout in order to avoid ZkTimeoutException
      config.set(KafkaConfig.ZkConnectionTimeoutMsProp, (30 * 1000).toString)
      config.load(baseConfig)

      val broker = new KafkaServer(new KafkaConfig(config.toProperties), new SystemTime)
      broker.startup()
      kafkaBrokers.update(index, broker)
      logDirs.update(index, logDir)
      broker.boundPort(new ListenerName("PLAINTEXT"))
    }
  }

  val brokers: String = validPorts.map("localhost:" + _).mkString(",")
  logger.info(s"ports used in LocalKafkaBrokers are ${brokers}")

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
    logDirs.foreach(deleteFile(_))
  }
}
