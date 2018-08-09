package com.island.ohara.configurator.endpoint

import java.sql.DriverManager
import java.util
import java.util.Properties
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.island.ohara.configurator.FakeConnectorClient
import com.island.ohara.configurator.endpoint.Validator._
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.KafkaUtil
import com.island.ohara.rest.ConfiguratorJson.ValidationResponse
import com.island.ohara.rest.ConnectorClient
import com.island.ohara.serialization.Serializer
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, KafkaConsumer, OffsetResetStrategy}
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.common.config.{ConfigDef, ConfigException}
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.source.{SourceConnector, SourceRecord, SourceTask}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * This class is used to verify the connection to 1) HDFS, 2) KAFKA and 3) RDB. Since ohara have many sources/sinks implemented
  * by kafak connector, the verification of connection should be run at the worker nodes. This class is implemented by kafka
  * souce connector in order to run the validation on all worker nodes.
  */
class Validator extends SourceConnector {
  private[this] var props: util.Map[String, String] = null
  override def version(): String = VERSION
  override def start(props: util.Map[String, String]): Unit = {
    this.props = new util.HashMap[String, String](props)
    checkArgument(TOPIC)
    checkArgument(URL)
    checkArgument(REQUEST_ID)
    checkArgument(TARGET)
    if (props.get(TARGET).toLowerCase.equals(TARGET_HDFS) && props.get(URL).startsWith("/"))
      throw new IllegalArgumentException(s"The $URL should have schema")
  }

  override def taskClass(): Class[_ <: Task] = classOf[ValidatorTask]

  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] = {
    val rval = new util.ArrayList[util.Map[String, String]](maxTasks)
    0 until maxTasks foreach { _ =>
      rval.add(new util.HashMap[String, String](props))
    }
    rval
  }

  override def stop(): Unit = {
    // do nothing
  }
  override def config(): ConfigDef = CONFIG_DEF

  private[this] def checkArgument(key: String): Unit = if (!props.containsKey(key) || props.get(key).isEmpty)
    throw new ConfigException(s"$key is required and it can't be empty")
}

object Validator {
  private[this] val INDEXER = new AtomicLong()
  private val INTERNAL_TOPIC = "_Validator_topic"

  /**
    * a helper method to run the validation process quickly.
    *
    * @param connectorClient connector client
    * @param brokersString broker information. form -> host:port,host:port
    * @param config config used to test
    * @param taskCount the number of task. It implies how many worker nodes should be verified
    * @param timeout timeout
    * @return reports
    */
  def run(connectorClient: ConnectorClient,
          brokersString: String,
          config: Map[String, String],
          taskCount: Int,
          timeout: Duration = 30 seconds): Seq[ValidationResponse] = connectorClient match {
    // we expose the fake component...ugly way (TODO) by chia
    case _: FakeConnectorClient => (0 until taskCount).map(_ => ValidationResponse("localhost", "a fake report", true))
    case _ => {
      val requestId: String = INDEXER.getAndIncrement().toString
      val latch = new CountDownLatch(1)
      val closed = new AtomicBoolean(false)
      // we have to create the consumer first so as to avoid the messages from connector
      val future = Future[Seq[ValidationResponse]] {
        val consumerConfig = new Properties()
        consumerConfig.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersString)
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, s"Validator-consumer-${INDEXER.getAndIncrement()}")
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.LATEST.name.toLowerCase)
        doClose(
          new KafkaConsumer[String, Any](consumerConfig,
                                         KafkaUtil.wrapDeserializer(Serializer.STRING),
                                         KafkaUtil.wrapDeserializer(Serializer.OBJECT))) { consumer =>
          try {
            consumer.subscribe(util.Arrays.asList(INTERNAL_TOPIC))
            val reports = new ArrayBuffer[ValidationResponse]()
            val endtime = System.currentTimeMillis() + timeout.toMillis
            while (!closed.get && reports.size < taskCount && System.currentTimeMillis() < endtime) {
              val records = consumer.poll(if (latch.getCount == 1) 100 else 500)
              latch.countDown()
              if (records != null) {
                records.forEach((record: ConsumerRecord[String, Any]) => {
                  if (record.key != null && record.key.equals(requestId)) record.value() match {
                    case report: ValidationResponse => reports += report
                    case _                          => throw new IllegalStateException(s"Unknown report:${record.value()}")
                  }
                })
              }
            }
            reports
          } finally latch.countDown()
        }
      }
      try {
        latch.await(timeout.toMillis, TimeUnit.MILLISECONDS)
        val validationName = s"Validator-${INDEXER.getAndIncrement()}"
        connectorClient
          .sourceConnectorCreator()
          .name(validationName)
          .disableConverter()
          .connectorClass(classOf[Validator].getName)
          .taskNumber(taskCount)
          .topic(INTERNAL_TOPIC)
          .config(config)
          .config(REQUEST_ID, requestId)
          .run()
        try Await.result(future, timeout)
        finally connectorClient.delete(validationName)
      } finally closed.set(true)
    }
  }

  /**
    * add this to config and then the key pushed to topic will be same with the value
    */
  val REQUEST_ID = "requestId"

  val TOPIC = "topic"
  val VERSION = "0.1"
  val TARGET = "target"
  val TARGET_HDFS = "hdfs"
  val TARGET_RDB = "rdb"
  val URL = "url"
  val TABLE = "table"
  val USER = "user"
  val PASSWORD = "password"

  val CONFIG_DEF = new ConfigDef()
    .define(URL, Type.STRING, null, Importance.HIGH, "target url")
    .define(TARGET, Type.STRING, null, Importance.HIGH, "target type")
    .define(TOPIC, Type.STRING, null, Importance.HIGH, "target topic")
}

import scala.collection.JavaConverters._

class ValidatorTask extends SourceTask {
  private[this] var done = false
  private[this] var props: Map[String, String] = null
  private[this] var topic: String = null
  private[this] var requestId: String = null
  override def start(props: util.Map[String, String]): Unit = {
    this.props = props.asScala.toMap
    topic = require(TOPIC)
    requestId = require(REQUEST_ID)
  }

  override def poll(): util.List[SourceRecord] = if (done) {
    // just wait the configurator to close this connector
    TimeUnit.SECONDS.sleep(2)
    null
  } else
    try information match {
      case info: HdfsInformation => toSourceRecord(ValidationResponse(hostname, validate(info), true))
      case info: RdbInformation  => toSourceRecord(ValidationResponse(hostname, validate(info), true))
    } catch {
      case e: Throwable => toSourceRecord(ValidationResponse(hostname, e.getMessage, false))
    } finally {
      done = true
    }

  override def stop(): Unit = {
    // do nothing
  }

  override def version(): String = VERSION

  private[this] def validate(info: HdfsInformation): String = {
    val config = new Configuration()
    config.set("fs.defaultFS", info.url)
    val fs = FileSystem.get(config)
    val home = fs.getHomeDirectory
    s"check the hdfs:${info.url} by getting the home:${home}"
  }
  import com.island.ohara.io.CloseOnce._

  private[this] def validate(info: RdbInformation): String = {
    val connectionUrl = s"${info.url};user=${info.user};password=${info.password}"
    doClose(DriverManager.getConnection(connectionUrl)) { _ =>
      s"succeed to establish the connection:$connectionUrl"
    }
  }

  private[this] def information = require(TARGET) match {
    case TARGET_HDFS => HdfsInformation(require(URL))
    case TARGET_RDB  => RdbInformation(require(URL), require(TABLE), require(USER), require(PASSWORD))
    case other: String =>
      throw new IllegalArgumentException(s"valid targets are $TARGET_HDFS and $TARGET_RDB. current is $other")
  }

  private[this] def toSourceRecord(data: ValidationResponse): util.List[SourceRecord] =
    util.Arrays.asList(
      new SourceRecord(null,
                       null,
                       topic,
                       Schema.BYTES_SCHEMA,
                       Serializer.STRING.to(requestId),
                       Schema.BYTES_SCHEMA,
                       Serializer.OBJECT.to(data)))

  private[this] def require(key: String): String =
    props.get(key).getOrElse(throw new IllegalArgumentException(s"the $key is required"))

  private[this] def hostname: String = try java.net.InetAddress.getLocalHost().getHostName()
  catch {
    case _: Throwable => "unknown"
  }
}

private case class HdfsInformation(url: String)
private case class RdbInformation(url: String, table: String, user: String, password: String)
