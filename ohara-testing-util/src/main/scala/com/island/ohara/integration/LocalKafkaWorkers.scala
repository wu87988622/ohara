package com.island.ohara.integration

import java.util

import com.island.ohara.config.OharaConfig
import com.island.ohara.io.CloseOnce
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.utils.Time
import org.apache.kafka.connect.runtime.distributed.{DistributedConfig, DistributedHerder}
import org.apache.kafka.connect.runtime.isolation.Plugins
import org.apache.kafka.connect.runtime.rest.RestServer
import org.apache.kafka.connect.runtime.{Connect, Worker, WorkerConfig}
import org.apache.kafka.connect.storage.{KafkaConfigBackingStore, KafkaOffsetBackingStore, KafkaStatusBackingStore}

import scala.util.Random

/**
  * Mini workers services. Each work is bind on random port default. The config, status, and offset topics are configured with no
  * replica and single partition in order to speedup the startup, and their names are "connect-config", "connect-status", and "connect-offset".
  * Also, both of internal converter and data converter are org.apache.kafka.connect.json.JsonConverter. Make sure the connector you tried
  * to load should be added to the classpath when you are running this mini services.
  *
  * @param brokersConn    the brokers info
  * @param ports         the ports to bind for workers
  * @param baseConfig the properties is used to override the default configs
  */
class LocalKafkaWorkers(brokersConn: String, ports: Seq[Int], baseConfig: OharaConfig = OharaConfig())
    extends CloseOnce {
  private[this] val logger = Logger(getClass.getName)
  private[this] val validPorts = resolvePorts(ports)
  logger.info(s"ports used in LocalKafkaWorkers are ${validPorts.mkString(",")}")
  val connects = new Array[Connect](validPorts.size)
  val workers = new Array[Worker](validPorts.size)
  val restServers = new Array[RestServer](validPorts.size)

  def pickRandomRestServer(): RestServer = restServers(Random.nextInt(restServers.size))

  validPorts.zipWithIndex.foreach {
    case (port: Int, index: Int) => {
      val config = OharaConfig()
      // reduce the number of partitions and replicas to speedup the mini cluster
      // for config storage. the partition of config topic is always 1 so we needn't to set it to 1 here.
      config.set(DistributedConfig.CONFIG_TOPIC_CONFIG, "connect-configs")
      config.set(DistributedConfig.CONFIG_STORAGE_REPLICATION_FACTOR_CONFIG, 1.toString)
      // for offset storage
      config.set(DistributedConfig.OFFSET_STORAGE_TOPIC_CONFIG, "connect-offsets")
      config.set(DistributedConfig.OFFSET_STORAGE_PARTITIONS_CONFIG, 1.toString)
      config.set(DistributedConfig.OFFSET_STORAGE_REPLICATION_FACTOR_CONFIG, 1.toString)
      // for status storage
      config.set(DistributedConfig.STATUS_STORAGE_TOPIC_CONFIG, "connect-status")
      config.set(DistributedConfig.STATUS_STORAGE_PARTITIONS_CONFIG, 1.toString)
      config.set(DistributedConfig.STATUS_STORAGE_REPLICATION_FACTOR_CONFIG, 1.toString)
      // set the brokers info
      config.set(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersConn)
      config.set(DistributedConfig.GROUP_ID_CONFIG, "connect")
      // set the normal converter
      config.set(WorkerConfig.KEY_CONVERTER_CLASS_CONFIG, "org.apache.kafka.connect.json.JsonConverter")
      config.set("key.converter.schemas.enable", true.toString)
      config.set(WorkerConfig.VALUE_CONVERTER_CLASS_CONFIG, "org.apache.kafka.connect.json.JsonConverter")
      config.set("value.converter.schemas.enable", true.toString)
      // set the internal converter. NOTED: kafka connector doesn't support to use schema in internal topics.
      config.set(WorkerConfig.INTERNAL_KEY_CONVERTER_CLASS_CONFIG, "org.apache.kafka.connect.json.JsonConverter")
      config.set("internal.key.converter.schemas.enable", false.toString)
      config.set(WorkerConfig.INTERNAL_VALUE_CONVERTER_CLASS_CONFIG, "org.apache.kafka.connect.json.JsonConverter")
      config.set("internal.value.converter.schemas.enable", false.toString)
      // TODO: REST_PORT_CONFIG is deprecated in kafka-1.1.0. Use LISTENERS_CONFIG instead. by chia
      config.set(WorkerConfig.REST_PORT_CONFIG, port.toString)
      config.set(WorkerConfig.PLUGIN_PATH_CONFIG, "")
      config.load(baseConfig)
      import scala.collection.JavaConverters._
      val distConfig = new DistributedConfig(config.toPlainMap.asJava)

      def createPlugins: Plugins = {
        val pluginProps = new util.HashMap[String, String]

        // Set up the plugins to have no additional plugin directories.
        // This won't allow us to test classpath isolation, but it will allow us to test some of the utility methods.
        pluginProps.put(WorkerConfig.PLUGIN_PATH_CONFIG, "")
        new Plugins(pluginProps)
      }
      val rest = new RestServer(distConfig)
      val workerId = s"localhost:${rest.advertisedUrl().getPort}"
      val offsetBackingStore = new KafkaOffsetBackingStore
      offsetBackingStore.configure(distConfig)
      val time = Time.SYSTEM
      val worker = new Worker(workerId, time, createPlugins, distConfig, offsetBackingStore)
      val internalValueConverter = worker.getInternalValueConverter()
      val statusBackingStore = new KafkaStatusBackingStore(time, internalValueConverter)
      statusBackingStore.configure(distConfig)
      val configBackingStore = new KafkaConfigBackingStore(internalValueConverter, distConfig)
      // TODO: DistributedHerder is a private class so its constructor is changed in kafka-1.1.0. by chia
      val herder = new DistributedHerder(distConfig,
                                         time,
                                         worker,
                                         statusBackingStore,
                                         configBackingStore,
                                         rest.advertisedUrl().toString())
      val connect = new Connect(herder, rest)
      connect.start()
      restServers.update(index, rest)
      workers.update(index, worker)
      connects.update(index, connect)
    }
  }

  override protected def doClose(): Unit = {
    connects.foreach(_.stop())
    connects.foreach(_.awaitStop())
  }
}
