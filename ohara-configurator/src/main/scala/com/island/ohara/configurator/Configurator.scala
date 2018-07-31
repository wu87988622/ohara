package com.island.ohara.configurator

import java.util.concurrent.TimeUnit

import com.island.ohara.config.UuidUtil
import com.island.ohara.configurator.kafka.KafkaClient
import com.island.ohara.configurator.store.{Store, StoreBuilder}
import com.island.ohara.data.{OharaData, OharaDataSerializer, OharaSchema}
import com.island.ohara.io.CloseOnce
import com.island.ohara.serialization.{Serializer, StringSerializer}
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration._

/**
  * Configurator works for controlling the jobs running in Ohara cluster. Configurator hosts a rest server in order
  * to accept the rest request. This way help us to run the Configurator and its user in different process/node.
  *
  * Configurator stores all Ohara data in Ohara Store. Currently the store impl is based on kafka topic.
  * see TopicStore for more details
  */
trait Configurator extends Iterable[OharaData] with CloseOnce {

  /**
    * the hostname of rest server
    */
  val hostname: String

  /**
    * the port of rest server
    */
  val port: Int

  val store: Store[String, OharaData]

  /**
    * @return iteration of schemas managed by configurator
    */
  def schemas: Iterator[OharaSchema]
}

object Configurator {
  def storeBuilder: StoreBuilder[String, OharaData] = Store.builder(Serializer.string, OharaDataSerializer)
  def builder = new ConfiguratorBuilder()

  val DEFAULT_UUID_GENERATOR: () => String = () => UuidUtil.uuid()

  val VERSION = "v0"
  val SCHEMA_PATH = "schemas"
  val DEFAULT_INITIALIZATION_TIMEOUT: Duration = 10 seconds
  val DEFAULT_TERMINATION_TIMEOUT: Duration = 10 seconds
  val TOPIC_PATH = "topics"

  //----------------[main]----------------//
  private[this] lazy val LOG = Logger(Configurator.getClass)
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
    *
    * @param args the first element is hostname and the second one is port
    */
  def main(args: Array[String]): Unit = {
    if (args.length == 1 && args(0).equals(HELP_KEY)) {
      println(USAGE)
      return
    }
    if (args.size < 2 || args.size % 2 != 0) throw new IllegalArgumentException(USAGE)
    // TODO: make the parse more friendly
    var hostname = "localhost"
    var port: Int = 0
    var brokers: Option[String] = None
    var topicName: Option[String] = None
    var numberOfPartitions: Int = 1
    var numberOfReplications: Short = 1
    args.sliding(2, 2).foreach {
      case Array(HOSTNAME_KEY, value)     => hostname = value
      case Array(PORT_KEY, value)         => port = value.toInt
      case Array(BROKERS_KEY, value)      => brokers = Some(value)
      case Array(TOPIC_KEY, value)        => topicName = Some(value)
      case Array(PARTITIONS_KEY, value)   => numberOfPartitions = value.toInt
      case Array(REPLICATIONS_KEY, value) => numberOfReplications = value.toShort
      case _                              => throw new IllegalArgumentException(USAGE)
    }

    if (brokers.isEmpty ^ topicName.isEmpty)
      throw new IllegalArgumentException(if (brokers.isEmpty) "brokers" else "topic" + " can't be empty")

    val configurator =
      if (brokers.isEmpty) Configurator.builder.noCluster.hostname(hostname).port(port).build()
      else
        Configurator.builder
          .store(
            Store
              .builder(StringSerializer, OharaDataSerializer)
              .brokers(brokers.get)
              .topicName(topicName.get)
              .numberOfReplications(numberOfReplications)
              .numberOfPartitions(numberOfPartitions)
              .build())
          .kafkaClient(KafkaClient(brokers.get))
          .hostname(hostname)
          .port(port)
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
