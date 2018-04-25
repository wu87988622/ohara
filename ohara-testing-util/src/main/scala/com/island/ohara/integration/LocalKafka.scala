package com.island.ohara.integration

import java.io.File
import java.util.Properties

import com.island.ohara.io.CloseOnce
import kafka.server.{KafkaConfig, KafkaServer}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.utils.SystemTime

import scala.collection.mutable.ArrayBuffer

/**
  * Mini brokers service. The log dir is under {TMP}/kafka-local/ and the hostname is "localhost".
  *
  * @param zkConnection   zookeeper connection.
  * @param _ports         the port to bind. default is a random number
  * @param baseProperties the properties passed to brokers
  */
private class LocalKafka(zkConnection: String, _ports: Seq[Int] = Array[Int](-1), baseProperties: Properties = new Properties) extends CloseOnce {
  private[this] val ports = resolvePorts(_ports)
  private[this] val brokersString = brokerList(ports)
  private[this] val brokers = new ArrayBuffer[KafkaServer](ports.size)
  private[this] val logDirs = new ArrayBuffer[File](ports.size)
  ports.zipWithIndex.foreach { case (port: Int, index: Int) => {
    val logDir = createTempDir("kafka-local")
    val properties = new Properties()
    properties.put(KafkaConfig.OffsetsTopicPartitionsProp, 1.toString)
    properties.put(KafkaConfig.OffsetsTopicReplicationFactorProp, 1.toString)
    properties.put("zookeeper.connect", zkConnection)
    properties.put("broker.id", String.valueOf(index + 1))
    properties.put("listeners", "PLAINTEXT://:" + port)
    properties.put("log.dir", logDir.getAbsolutePath())
    properties.put("log.flush.interval.messages", String.valueOf(1))
    properties.putAll(baseProperties)
    val broker = startBroker(properties)
    brokers += broker
    logDirs += logDir
  }
  }

  def kafkaBrokers: Seq[KafkaServer] = brokers

  def kafkaLogFolder: Seq[File] = logDirs

  private[this] def resolvePorts(ports: Seq[Int]): Seq[Int] = ports.map((port: Int) => if (port <= 0) availablePort else port)

  private[this] def brokerList(ports: Seq[Int]): String = {
    val sb = new StringBuilder
    for (port <- ports) {
      if (sb.length > 0) sb.append(",")
      sb.append("localhost:").append(port)
    }
    sb.toString
  }

  private[this] def startBroker(props: Properties): KafkaServer = {
    val server = new KafkaServer(new KafkaConfig(props), new SystemTime)
    server.startup
    server
  }

  def properties: Properties = {
    val props = new Properties
    props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersString)
    props.put("metadata.broker.list", brokersString)
    props.put("zookeeper.connect", zkConnection)
    props
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
