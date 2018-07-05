package com.island.ohara.configurator

import java.util.concurrent.TimeUnit

import com.island.ohara.configurator.data.{OharaData, OharaDataSerializer}
import com.island.ohara.configurator.kafka.KafkaClient
import com.island.ohara.configurator.store.Store
import com.island.ohara.serialization.StringSerializer
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.Duration

class ConfiguratorBuilder {
  private[this] var uuidGenerator: Option[() => String] = Some(Configurator.DEFAULT_UUID_GENERATOR)
  private[this] var hostname: Option[String] = None
  private[this] var port: Option[Int] = None
  private[this] var store: Option[Store[String, OharaData]] = None
  private[this] var kafkaClient: Option[KafkaClient] = None
  private[this] var initializationTimeout: Option[Duration] = Some(Configurator.DEFAULT_INITIALIZATION_TIMEOUT)
  private[this] var terminationTimeout: Option[Duration] = Some(Configurator.DEFAULT_TERMINATION_TIMEOUT)

  /**
    * set a specified uuid generator.
    * @param generator uuid generator
    * @return this builder
    */
  def uuidGenerator(generator: () => String): ConfiguratorBuilder = {
    uuidGenerator = Some(generator)
    this
  }

  /**
    * set a specified hostname
    * @param hostname used to build the rest server
    * @return this builder
    */
  def hostname(hostname: String): ConfiguratorBuilder = {
    this.hostname = Some(hostname)
    this
  }

  /**
    * set a specified port
    * @param port used to build the rest server
    * @return this builder
    */
  def port(port: Int): ConfiguratorBuilder = {
    this.port = Some(port)
    this
  }

  /**
    * set a specified store used to maintain the ohara data.
    * NOTED: Configurator has responsibility to release this store.
    * @param store used to maintain the ohara data.
    * @return this builder
    */
  def store(store: Store[String, OharaData]): ConfiguratorBuilder = {
    this.store = Some(store)
    this
  }

  def terminationTimeout(terminationTimeout: Duration): ConfiguratorBuilder = {
    this.terminationTimeout = Some(terminationTimeout)
    this
  }

  def initializationTimeout(initializationTimeout: Duration): ConfiguratorBuilder = {
    this.initializationTimeout = Some(initializationTimeout)
    this
  }

  def kafkaClient(kafkaClient: KafkaClient): ConfiguratorBuilder = {
    this.kafkaClient = Some(kafkaClient)
    this
  }

  /**
    * set a mock kafka client to this configurator. a testing-purpose method.
    * @return this builder
    */
  def noCluster: ConfiguratorBuilder = {
    kafkaClient(KafkaClient.EMPTY)
    store(Store.inMemory(StringSerializer, OharaDataSerializer))
  }

  def build(): Configurator = new ConfiguratorImpl(uuidGenerator.get,
                                                   hostname.get,
                                                   port.get,
                                                   store.get,
                                                   kafkaClient.get,
                                                   initializationTimeout.get,
                                                   terminationTimeout.get)
}

object ConfiguratorBuilder {
  private[this] lazy val LOG = Logger(ConfiguratorBuilder.getClass)
  val HELP_KEY = "--help"
  val HOSTNAME_KEY = "--hostname"
  val PORT_KEY = "--port"
  val BROKERS_KEY = "--brokers"
  val TOPIC_KEY = "--topic"
  val PARTITIONS_KEY = "--partitions"
  val REPLICATIONS_KEY = "--replications"
  val USAGE = s"[Usage] $HOSTNAME_KEY $PORT_KEY $BROKERS_KEY $TOPIC_KEY $PARTITIONS_KEY $REPLICATIONS_KEY"

  /**
    * Running a standalone configurator.
    * NOTED: this main is exposed to build.gradle. If you want to move the main out of this class, please update the
    * build.gradle also.
    * @param args the first element is hostname and the second one is port
    */
  def main(args: Array[String]): Unit = {
    if (args.length == 1 && args(0).equals(HELP_KEY)) {
      println(USAGE)
      return
    }
    if (args.size < 2 || args.size % 2 != 0) throw new IllegalArgumentException(USAGE)
    // TODO: make the parse more friendly
    var hostname: Option[String] = Some("localhost")
    var port: Option[Int] = Some(0)
    var brokers: Option[String] = None
    var topicName: Option[String] = None
    var numberOfPartitions: Option[Int] = Some(1)
    var numberOfReplications: Option[Short] = Some(1)
    args.sliding(2, 2).foreach {
      case Array(HOSTNAME_KEY, value)     => hostname = Some(value)
      case Array(PORT_KEY, value)         => port = Some(value.toInt)
      case Array(BROKERS_KEY, value)      => brokers = Some(value)
      case Array(TOPIC_KEY, value)        => topicName = Some(value)
      case Array(PARTITIONS_KEY, value)   => numberOfPartitions = Some(value.toInt)
      case Array(REPLICATIONS_KEY, value) => numberOfReplications = Some(value.toShort)
      case _                              => throw new IllegalArgumentException(USAGE)
    }

    if (brokers.isEmpty ^ topicName.isEmpty)
      throw new IllegalArgumentException(if (brokers.isEmpty) "brokers" else "topic" + " can't be empty")

    val configurator =
      if (brokers.isEmpty) Configurator.builder.noCluster.hostname(hostname.get).port(port.get).build()
      else
        Configurator.builder
          .store(
            Store
              .builder(StringSerializer, OharaDataSerializer)
              .brokers(brokers.get)
              .topicName(topicName.get)
              .numberOfReplications(numberOfReplications.get)
              .numberOfPartitions(numberOfPartitions.get)
              .build())
          .kafkaClient(KafkaClient(brokers.get))
          .hostname(hostname.get)
          .port(port.get)
          .build()
    hasRunningConfigurator = true
    try {
      LOG.info(s"start a configurator built on hostname:${configurator.hostname} and port:${configurator.port}")
      LOG.info("enter ctrl+c to terminate the configurator")
      while (!closeRunningConfigurator) {
        TimeUnit.SECONDS.sleep(2)
        LOG.info(s"Current data size:${configurator.size}")
      }
    } catch {
      case _: InterruptedException => LOG.info("prepare to die")
    } finally {
      hasRunningConfigurator = false
      configurator.close()
    }
  }

  /**
    * visible for testing.
    */
  @volatile private[configurator] var hasRunningConfigurator = false

  /**
    * visible for testing.
    */
  @volatile private[configurator] var closeRunningConfigurator = false
}
