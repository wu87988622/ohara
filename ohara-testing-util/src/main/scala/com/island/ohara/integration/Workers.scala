package com.island.ohara.integration
import java.util

import com.island.ohara.io.{CloseOnce, IoUtil}
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.utils.Time
import org.apache.kafka.connect.runtime.{Connect, Worker, WorkerConfig}
import org.apache.kafka.connect.runtime.distributed.{DistributedConfig, DistributedHerder}
import org.apache.kafka.connect.runtime.isolation.Plugins
import org.apache.kafka.connect.runtime.rest.RestServer
import org.apache.kafka.connect.storage.{KafkaConfigBackingStore, KafkaOffsetBackingStore, KafkaStatusBackingStore}

import scala.collection.mutable

trait Workers extends CloseOnce {

  /**
    * @return workers information. the form is "host_a:port_a,host_b:port_b"
    */
  def connectionProps: String

  /**
    * @return true if this worker cluster is generated locally.
    */
  def isLocal: Boolean
}

object Workers {
  private[integration] val WORKER_CONNECTION_PROPS: String = "ohara.it.workers"
  private[integration] val NUMBER_OF_WORKERS = 3
  def apply(brokers: => Brokers): Workers = apply(sys.env.get(WORKER_CONNECTION_PROPS), brokers)

  /**
    * create an embedded worker cluster with specific port
    * @param brokers brokers
    * @param ports bound ports
    * @return an embedded worker cluster
    */
  def local(brokers: => Brokers, ports: Seq[Int]): Workers = {
    val availablePorts = ports.map(port => if (port <= 0) availablePort() else port)
    val connects = availablePorts.map { port =>
      val config = new mutable.HashMap[String, String]()
      // reduce the number of partitions and replicas to speedup the mini cluster
      // for config storage. the partition of config topic is always 1 so we needn't to set it to 1 here.
      config += (DistributedConfig.CONFIG_TOPIC_CONFIG -> "connect-configs")
      config += (DistributedConfig.CONFIG_STORAGE_REPLICATION_FACTOR_CONFIG -> 1.toString)
      // for offset storage
      config += (DistributedConfig.OFFSET_STORAGE_TOPIC_CONFIG -> "connect-offsets")
      config += (DistributedConfig.OFFSET_STORAGE_PARTITIONS_CONFIG -> 1.toString)
      config += (DistributedConfig.OFFSET_STORAGE_REPLICATION_FACTOR_CONFIG -> 1.toString)
      // for status storage
      config += (DistributedConfig.STATUS_STORAGE_TOPIC_CONFIG -> "connect-status")
      config += (DistributedConfig.STATUS_STORAGE_PARTITIONS_CONFIG -> 1.toString)
      config += (DistributedConfig.STATUS_STORAGE_REPLICATION_FACTOR_CONFIG -> 1.toString)
      // set the brokers info
      config += (CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG -> brokers.connectionProps)
      config += (DistributedConfig.GROUP_ID_CONFIG -> "connect")
      // set the normal converter
      config += (WorkerConfig.KEY_CONVERTER_CLASS_CONFIG -> "org.apache.kafka.connect.json.JsonConverter")
      config += ("key.converter.schemas.enable" -> true.toString)
      config += (WorkerConfig.VALUE_CONVERTER_CLASS_CONFIG -> "org.apache.kafka.connect.json.JsonConverter")
      config += ("value.converter.schemas.enable" -> true.toString)
      // set the internal converter. NOTED: kafka connector doesn't support to use schema in internal topics.
      config += (WorkerConfig.INTERNAL_KEY_CONVERTER_CLASS_CONFIG -> "org.apache.kafka.connect.json.JsonConverter")
      config += ("internal.key.converter.schemas.enable" -> false.toString)
      config += (WorkerConfig.INTERNAL_VALUE_CONVERTER_CLASS_CONFIG -> "org.apache.kafka.connect.json.JsonConverter")
      config += ("internal.value.converter.schemas.enable" -> false.toString)
      // TODO: REST_PORT_CONFIG is deprecated in kafka-1.1.0. Use LISTENERS_CONFIG instead. by chia
      config += (WorkerConfig.REST_PORT_CONFIG -> port.toString)
      import scala.collection.JavaConverters._
      val distConfig = new DistributedConfig(config.asJava)
      val rest = new RestServer(distConfig)
      val workerId = s"${IoUtil.hostname}:${rest.advertisedUrl().getPort}"
      val offsetBackingStore = new KafkaOffsetBackingStore
      offsetBackingStore.configure(distConfig)
      val time = Time.SYSTEM
      val worker =
        new Worker(workerId, time, new Plugins(util.Collections.emptyMap()), distConfig, offsetBackingStore)
      val internalValueConverter = worker.getInternalValueConverter
      val statusBackingStore = new KafkaStatusBackingStore(time, internalValueConverter)
      statusBackingStore.configure(distConfig)
      val configBackingStore = new KafkaConfigBackingStore(internalValueConverter, distConfig)
      // TODO: DistributedHerder is a private class so its constructor is changed in kafka-1.1.0. by chia
      val herder = new DistributedHerder(distConfig,
                                         time,
                                         worker,
                                         statusBackingStore,
                                         configBackingStore,
                                         rest.advertisedUrl().toString)
      val connect = new Connect(herder, rest)
      connect.start()
      connect
    }
    new Workers {
      override def connectionProps: String = availablePorts.map(p => s"${IoUtil.hostname}:$p").mkString(",")
      override protected def doClose(): Unit = {
        connects.foreach(_.stop())
        connects.foreach(_.awaitStop())
      }
      override def isLocal: Boolean = true
    }
  }

  private[integration] def apply(workers: Option[String], brokers: => Brokers): Workers = workers
    .map { s =>
      new Workers {
        override def connectionProps: String = s
        override protected def doClose(): Unit = {}
        override def isLocal: Boolean = false
      }
    }
    .getOrElse(local(brokers, Seq.fill(NUMBER_OF_WORKERS)(availablePort())))
}
