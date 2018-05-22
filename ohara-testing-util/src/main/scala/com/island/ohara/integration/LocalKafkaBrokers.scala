package com.island.ohara.integration

import java.io.File
import java.util.Properties

import com.island.ohara.config.{OharaConfig, UuidUtil}
import com.island.ohara.io.CloseOnce
import com.typesafe.scalalogging.Logger
import kafka.server.{KafkaConfig, KafkaServer}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.utils.SystemTime

/**
  * Mini brokers service. The log dir is under {TMP}/kafka-local/ and the hostname is "localhost". The offset topic is configured with
  * no replication and single partition in order to speedup the startup.
  *
  * @param zkConnection   zookeeper connection.
  * @param ports         the port to bind. default is a random number
  * @param baseConfig the properties passed to brokers
  */
private class LocalKafkaBrokers(zkConnection: String, ports: Seq[Int], baseConfig: OharaConfig = OharaConfig())
    extends CloseOnce {
  private[this] val logger = Logger(getClass.getName)
  private[this] val validPorts = resolvePorts(ports)
  logger.info(s"ports used in LocalKafkaBrokers are ${validPorts.mkString(",")}")
  val brokersString: String = {
    val sb = new StringBuilder
    for (port <- validPorts) {
      if (sb.length > 0) sb.append(",")
      sb.append("localhost:").append(port)
    }
    sb.toString
  }
  val brokers = new Array[KafkaServer](validPorts.size)
  val logDirs = new Array[File](validPorts.size)
  validPorts.zipWithIndex.foreach {
    case (port: Int, index: Int) => {
      val logDir = createTempDir("kafka-local")
      val config = OharaConfig()
      // reduce the number of partitions and replicas to speedup the mini cluster
      config.set(KafkaConfig.OffsetsTopicPartitionsProp, 1.toString)
      config.set(KafkaConfig.OffsetsTopicReplicationFactorProp, 1.toString)
      config.set(KafkaConfig.ZkConnectProp, zkConnection)
      config.set(KafkaConfig.BrokerIdProp, String.valueOf(index + 1))
      config.set(KafkaConfig.ListenersProp, "PLAINTEXT://:" + port)
      config.set(KafkaConfig.LogDirProp, logDir.getAbsolutePath())
      // increase the timeout in order to avoid ZkTimeoutException
      config.set(KafkaConfig.ZkConnectionTimeoutMsProp, (30 * 1000).toString)
      config.load(baseConfig)

      def startBroker(props: Properties): KafkaServer = {
        val server = new KafkaServer(new KafkaConfig(props), new SystemTime)
        server.startup()
        server
      }

      val broker = startBroker(config.toProperties)
      brokers.update(index, broker)
      logDirs.update(index, logDir)
    }
  }

  /**
    * Generate the basic config. The config is composed of following setting.
    * 1) bootstrap.servers
    * 2) metadata.broker.list
    * 3) zookeeper.connect
    * @return a basic config including the brokers information
    */
  def config: OharaConfig = {
    val config = OharaConfig()
    config.set(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersString)
    config.set("metadata.broker.list", brokersString)
    config.set("zookeeper.connect", zkConnection)
    config
  }

  /**
    * Generate a config for kafka producer. The config is composed of following setting.
    * 1) bootstrap.servers
    * 2) metadata.broker.list
    * 3) zookeeper.connect
    * @return a config used to instantiate kafka producer
    */
  def producerConfig: OharaConfig = {
    val config = OharaConfig()
    config.set(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersString)
    config.set("metadata.broker.list", brokersString)
    config.set("zookeeper.connect", zkConnection)
    config
  }

  /**
    * Generate a config for kafka consumer. The config is composed of following setting.
    * 1) bootstrap.servers
    * 2) metadata.broker.list
    * 3) zookeeper.connect
    * 4) group.id -> a arbitrary string
    * 5) auto.offset.reset -> earliest
    * @return a config used to instantiate kafka consumer
    */
  def consumerConfig: OharaConfig = {
    val config = OharaConfig()
    config.set(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersString)
    config.set("metadata.broker.list", brokersString)
    config.set("zookeeper.connect", zkConnection)
    config.set(ConsumerConfig.GROUP_ID_CONFIG, UuidUtil.uuid())
    config.set(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    config
  }

  override def toString: String = {
    val sb = new StringBuilder("LocalKafka{")
    sb.append("brokerList='").append(brokersString).append('\'')
    sb.append('}')
    sb.toString
  }

  override protected def doClose(): Unit = {
    brokers.foreach(_.shutdown())
    logDirs.foreach(deleteFile(_))
  }
}
