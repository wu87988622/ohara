package com.island.ohara.integration

import java.io.File
import java.util.concurrent.TimeUnit

import com.island.ohara.config.{OharaConfig, UuidUtil}
import com.island.ohara.io.CloseOnce
import com.island.ohara.io.CloseOnce.doClose
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
  val brokers = new Array[KafkaServer](ports.size)
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
      brokers.update(index, broker)
      logDirs.update(index, logDir)
      broker.boundPort(new ListenerName("PLAINTEXT"))
    }
  }

  val brokersString: String = validPorts.map("localhost:" + _).mkString(",")
  logger.info(s"ports used in LocalKafkaBrokers are ${brokersString}")

  /**
    * Generate the basic config. The config is composed of following setting.
    * 1) bootstrap.servers
    *
    * @return a basic config including the brokers information
    */
  def config: OharaConfig = {
    val config = OharaConfig()
    config.set(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersString)
    config
  }

  /**
    * Generate a config for kafka producer. The config is composed of following setting.
    * 1) bootstrap.servers
    *
    * @return a config used to instantiate kafka producer
    */
  def producerConfig: OharaConfig = {
    val config = OharaConfig()
    config.set(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersString)
    config
  }

  /**
    * Generate a config for kafka consumer. The config is composed of following setting.
    * 1) bootstrap.servers
    * 2) group.id -> a arbitrary string
    * 3) auto.offset.reset -> earliest
    *
    * @return a config used to instantiate kafka consumer
    */
  def consumerConfig: OharaConfig = {
    val config = OharaConfig()
    config.set(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersString)
    config.set(ConsumerConfig.GROUP_ID_CONFIG, UuidUtil.uuid())
    config.set(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.name.toLowerCase)
    config
  }

  override def toString: String = {
    val sb = new StringBuilder("LocalKafka{")
    sb.append("brokerList='").append(brokersString).append('\'')
    sb.append('}')
    sb.toString
  }

  override protected def doClose(): Unit = {
    brokers.foreach(s => {
      s.shutdown()
      s.awaitShutdown()
    })
    logDirs.foreach(deleteFile(_))
  }
}
