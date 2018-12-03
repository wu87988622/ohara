package com.island.ohara.configurator.endpoint

import java.sql.DriverManager
import java.util
import java.util.concurrent.TimeUnit

import com.island.ohara.client.ConfiguratorJson.{
  FtpValidationRequest,
  HdfsValidationRequest,
  RdbValidationRequest,
  ValidationReport
}
import com.island.ohara.client.{ConfiguratorJson, ConnectorClient, FtpClient}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.FakeConnectorClient
import com.island.ohara.configurator.endpoint.Validator._
import com.island.ohara.kafka.{ConsumerRecord, KafkaClient}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.source.{SourceConnector, SourceRecord, SourceTask}
import spray.json.{JsNumber, JsObject, JsString}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * This class is used to verify the connection to 1) HDFS, 2) KAFKA and 3) RDB. Since ohara have many sources/sinks implemented
  * by kafak connector, the verification from connection should be run at the worker nodes. This class is implemented by kafka
  * souce connector in order to run the validation on all worker nodes.
  * TODO: refactor this one...it is ugly I'd say... by chia
  */
class Validator extends SourceConnector {
  private[this] var props: util.Map[String, String] = _
  override def version(): String = com.island.ohara.kafka.connector.ConnectorUtil.VERSION
  override def start(props: util.Map[String, String]): Unit = {
    this.props = new util.HashMap[String, String](props)
    // we don't want to make any exception here
  }

  override def taskClass(): Class[_ <: Task] = classOf[ValidatorTask]

  override def taskConfigs(maxTasks: Int): util.List[util.Map[String, String]] =
    Seq.fill(maxTasks)(new util.HashMap[String, String](props)).map(_.asInstanceOf[util.Map[String, String]]).asJava

  override def stop(): Unit = {
    // do nothing
  }
  override def config(): ConfigDef = CONFIG_DEF

}

object Validator {
  private[this] val TIMEOUT = 30 seconds
  private[endpoint] val INTERNAL_TOPIC = "_Validator_topic"

  /**
    * add this to config and then the key pushed to topic will be same with the value
    */
  private[endpoint] val REQUEST_ID = "requestId"
  private[endpoint] val TARGET = "target"
  private[endpoint] val TARGET_HDFS = "hdfs"
  private[endpoint] val TARGET_RDB = "rdb"
  private[endpoint] val TARGET_FTP = "ftp"

  def run(connectorClient: ConnectorClient,
          kafkaClient: KafkaClient,
          request: RdbValidationRequest,
          taskCount: Int): Future[Seq[ValidationReport]] = run(
    connectorClient,
    kafkaClient,
    TARGET_RDB,
    ConfiguratorJson.RDB_VALIDATION_REQUEST_JSON_FORMAT.write(request).asJsObject.fields.map {
      case (k, v) => (k, v.asInstanceOf[JsString].value)
    },
    taskCount
  )

  def run(connectorClient: ConnectorClient,
          kafkaClient: KafkaClient,
          request: HdfsValidationRequest,
          taskCount: Int): Future[Seq[ValidationReport]] = run(
    connectorClient,
    kafkaClient,
    TARGET_HDFS,
    ConfiguratorJson.HDFS_VALIDATION_REQUEST_JSON_FORMAT.write(request).asJsObject.fields.map {
      case (k, v) => (k, v.asInstanceOf[JsString].value)
    },
    taskCount
  )

  def run(connectorClient: ConnectorClient,
          kafkaClient: KafkaClient,
          request: FtpValidationRequest,
          taskCount: Int): Future[Seq[ValidationReport]] = run(
    connectorClient,
    kafkaClient,
    TARGET_FTP,
    ConfiguratorJson.FTP_VALIDATION_REQUEST_JSON_FORMAT.write(request).asJsObject.fields.map {
      case (k, v) =>
        v match {
          case s: JsString => (k, s.value)
          // port is Int type
          case n: JsNumber => (k, n.value.toString)
          case _           => throw new IllegalArgumentException("what is this??")
        }
    },
    taskCount
  )

  /**
    * a helper method to run the validation process quickly.
    *
    * @param connectorClient connector client
    * @param kafkaClient kafka client
    * @param config config used to test
    * @param taskCount the number from task. It implies how many worker nodes should be verified
    * @return reports
    */
  private[this] def run(connectorClient: ConnectorClient,
                        kafkaClient: KafkaClient,
                        target: String,
                        config: Map[String, String],
                        taskCount: Int): Future[Seq[ValidationReport]] = connectorClient match {
    // we expose the fake component...ugly way (TODO) by chia
    case _: FakeConnectorClient =>
      Future.successful((0 until taskCount).map(_ => ValidationReport(CommonUtil.hostname, "a fake report", true)))
    case _ =>
      Future {
        val requestId: String = CommonUtil.uuid()
        val validationName = s"Validator-${CommonUtil.uuid()}"
        connectorClient
          .connectorCreator()
          .name(validationName)
          .disableConverter()
          .connectorClass(classOf[Validator].getName)
          .numberOfTasks(taskCount)
          .topic(INTERNAL_TOPIC)
          .configs(config)
          .config(REQUEST_ID, requestId)
          .config(TARGET, target)
          .create()
        // TODO: receiving all messages may be expensive...by chia
        val client = kafkaClient
          .consumerBuilder()
          .offsetFromBegin()
          .topicName(INTERNAL_TOPIC)
          .build(Serializer.STRING, Serializer.OBJECT)
        try client
          .poll(
            java.time.Duration.ofNanos(TIMEOUT.toNanos),
            taskCount,
            new java.util.function.Function[util.List[ConsumerRecord[String, Object]],
                                            util.List[ConsumerRecord[String, Object]]] {
              override def apply(
                records: util.List[ConsumerRecord[String, Object]]): util.List[ConsumerRecord[String, Object]] = {
                records.asScala.filter(requestId == _.key.orElse(null)).asJava
              }
            }
          )
          .asScala
          .map(_.value.get match {
            case report: ValidationReport => report
            case _                        => throw new IllegalStateException(s"Unknown report")
          })
        finally connectorClient.delete(validationName)
      }
  }

  val CONFIG_DEF: ConfigDef = new ConfigDef().define(TARGET, Type.STRING, null, Importance.HIGH, "target type")
}

class ValidatorTask extends SourceTask {
  private[this] var done = false
  private[this] var props: Map[String, String] = _
  private[this] val topic: String = INTERNAL_TOPIC
  private[this] var requestId: String = _
  override def start(props: util.Map[String, String]): Unit = {
    this.props = props.asScala.toMap
    requestId = require(REQUEST_ID)
  }

  override def poll(): util.List[SourceRecord] = if (done) {
    // just wait the configurator to close this connector
    TimeUnit.SECONDS.sleep(2)
    null
  } else
    try information match {
      case info: HdfsValidationRequest => toSourceRecord(ValidationReport(hostname, validate(info), true))
      case info: RdbValidationRequest  => toSourceRecord(ValidationReport(hostname, validate(info), true))
      case info: FtpValidationRequest  => toSourceRecord(ValidationReport(hostname, validate(info), true))
    } catch {
      case e: Throwable => toSourceRecord(ValidationReport(hostname, e.getMessage, false))
    } finally {
      done = true
    }

  override def stop(): Unit = {
    // do nothing
  }

  override def version(): String = com.island.ohara.kafka.connector.ConnectorUtil.VERSION

  private[this] def validate(info: HdfsValidationRequest): String = {
    val config = new Configuration()
    config.set("fs.defaultFS", info.uri)
    val fs = FileSystem.get(config)
    val home = fs.getHomeDirectory
    s"check the hdfs:${info.uri} by getting the home:$home"
  }

  private[this] def validate(info: RdbValidationRequest): String = {
    val conn = DriverManager.getConnection(info.url, info.user, info.password)
    try s"succeed to establish the connection:${info.url}"
    finally conn.close()
  }
  private[this] def validate(info: FtpValidationRequest): String = {
    val client =
      FtpClient.builder().hostname(info.hostname).port(info.port).user(info.user).password(info.password).build()
    try s"succeed to establish the connection:${info.hostname}:${info.port} with status:${client.status()}"
    finally client.close()
  }

  private[this] def toJsObject: JsObject = JsObject(props.map { case (k, v) => (k, JsString(v)) })
  private[this] def information = require(TARGET) match {
    case TARGET_HDFS => ConfiguratorJson.HDFS_VALIDATION_REQUEST_JSON_FORMAT.read(toJsObject)
    case TARGET_RDB  => ConfiguratorJson.RDB_VALIDATION_REQUEST_JSON_FORMAT.read(toJsObject)
    case TARGET_FTP  => ConfiguratorJson.FTP_VALIDATION_REQUEST_JSON_FORMAT.read(toJsObject)
    case other: String =>
      throw new IllegalArgumentException(s"valid targets are $TARGET_HDFS and $TARGET_RDB. current is $other")
  }

  private[this] def toSourceRecord(data: ValidationReport): util.List[SourceRecord] =
    util.Arrays.asList(
      new SourceRecord(null,
                       null,
                       topic,
                       Schema.BYTES_SCHEMA,
                       Serializer.STRING.to(requestId),
                       Schema.BYTES_SCHEMA,
                       Serializer.OBJECT.to(data)))

  private[this] def require(key: String): String =
    props.getOrElse(key, throw new IllegalArgumentException(s"the $key is required"))

  private[this] def hostname: String = try CommonUtil.hostname
  catch {
    case _: Throwable => "unknown"
  }
}
