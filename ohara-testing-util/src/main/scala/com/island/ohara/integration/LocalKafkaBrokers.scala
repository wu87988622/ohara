package com.island.ohara.integration

import java.io.File
import java.util.Properties

import com.island.ohara.io.CloseOnce
import kafka.server.{KafkaConfig, KafkaServer}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.utils.SystemTime

/**
  * Mini brokers service. The log dir is under {TMP}/kafka-local/ and the hostname is "localhost". The offset topic is configured with
  * no replication and single partition in order to speedup the startup.
  *
  * @param zkConnection   zookeeper connection.
  * @param _ports         the port to bind. default is a random number
  * @param baseProperties the properties passed to brokers
  */
private class LocalKafkaBrokers(zkConnection: String, _ports: Seq[Int], baseProperties: Properties = new Properties)
    extends CloseOnce {
  private[this] val ports = resolvePorts(_ports)
  val brokersString: String = {
    val sb = new StringBuilder
    for (port <- ports) {
      if (sb.length > 0) sb.append(",")
      sb.append("localhost:").append(port)
    }
    sb.toString
  }
  val brokers = new Array[KafkaServer](ports.size)
  val logDirs = new Array[File](ports.size)
  ports.zipWithIndex.foreach {
    case (port: Int, index: Int) => {
      val logDir = createTempDir("kafka-local")
      val properties = new Properties()
      // reduce the number of partitions and replicas to speedup the mini cluster
      properties.put(KafkaConfig.OffsetsTopicPartitionsProp, 1.toString)
      properties.put(KafkaConfig.OffsetsTopicReplicationFactorProp, 1.toString)
      properties.put(KafkaConfig.ZkConnectProp, zkConnection)
      properties.put(KafkaConfig.BrokerIdProp, String.valueOf(index + 1))
      properties.put(KafkaConfig.ListenersProp, "PLAINTEXT://:" + port)
      properties.put(KafkaConfig.LogDirProp, logDir.getAbsolutePath())
      properties.putAll(baseProperties)

      def startBroker(props: Properties): KafkaServer = {
        val server = new KafkaServer(new KafkaConfig(props), new SystemTime)
        server.startup
        server
      }

      val broker = startBroker(properties)
      brokers.update(index, broker)
      logDirs.update(index, logDir)
    }
  }

  /**
    * A properties is used to create the KafkaProducer or KafkaConsumer. The basic information including brokers info and zk info have
    * been added.
    * @return properties
    */
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
