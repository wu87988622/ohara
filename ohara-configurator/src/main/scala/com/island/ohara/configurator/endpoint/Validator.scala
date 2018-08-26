package com.island.ohara.configurator.endpoint

import java.sql.DriverManager
import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.island.ohara.configurator.FakeConnectorClient
import com.island.ohara.configurator.endpoint.Validator._
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.{ConsumerRecord, KafkaClient}
import com.island.ohara.client.ConfiguratorJson.{HdfsValidationRequest, RdbValidationRequest, ValidationReport}
import com.island.ohara.client.{ConfiguratorJson, ConnectorClient}
import com.island.ohara.serialization.Serializer
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}
import org.apache.kafka.connect.connector.Task
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.source.{SourceConnector, SourceRecord, SourceTask}
import spray.json.{JsObject, JsString}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * This class is used to verify the connection to 1) HDFS, 2) KAFKA and 3) RDB. Since ohara have many sources/sinks implemented
  * by kafak connector, the verification of connection should be run at the worker nodes. This class is implemented by kafka
  * souce connector in order to run the validation on all worker nodes.
  * TODO: refactor this one...it is ugly I'd say... by chia
  */
class Validator extends SourceConnector {
  private[this] var props: util.Map[String, String] = null
  override def version(): String = VERSION
  override def start(props: util.Map[String, String]): Unit = {
    this.props = new util.HashMap[String, String](props)
    // we don't want to make any exception here
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

}

object Validator {
  private[this] val TIMEOUT = 30 seconds
  private[this] val INDEXER = new AtomicLong()
  private[endpoint] val INTERNAL_TOPIC = "_Validator_topic"

  /**
    * add this to config and then the key pushed to topic will be same with the value
    */
  private[endpoint] val REQUEST_ID = "requestId"

  private[endpoint] val VERSION = "0.1"
  private[endpoint] val TARGET = "target"
  private[endpoint] val TARGET_HDFS = "hdfs"
  private[endpoint] val TARGET_RDB = "rdb"

  def run(connectorClient: ConnectorClient,
          kafkaClient: KafkaClient,
          request: RdbValidationRequest,
          taskCount: Int): Seq[ValidationReport] = run(
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
          taskCount: Int): Seq[ValidationReport] = run(
    connectorClient,
    kafkaClient,
    TARGET_HDFS,
    ConfiguratorJson.HDFS_VALIDATION_REQUEST_JSON_FORMAT.write(request).asJsObject.fields.map {
      case (k, v) => (k, v.asInstanceOf[JsString].value)
    },
    taskCount
  )

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
  private[this] def run(connectorClient: ConnectorClient,
                        kafkaClient: KafkaClient,
                        target: String,
                        config: Map[String, String],
                        taskCount: Int): Seq[ValidationReport] = connectorClient match {
    // we expose the fake component...ugly way (TODO) by chia
    case _: FakeConnectorClient => (0 until taskCount).map(_ => ValidationReport("localhost", "a fake report", true))
    case _ => {
      val requestId: String = INDEXER.getAndIncrement().toString
      val latch = new CountDownLatch(1)
      val closed = new AtomicBoolean(false)
      // we have to create the consumer first so as to avoid the messages from connector
      val future = Future[Seq[ValidationReport]] {
        doClose(
          kafkaClient
            .consumerBuilder(Serializer.STRING, Serializer.OBJECT)
            .offsetAfterLatest()
            .topicName(INTERNAL_TOPIC)
            .build()) { consumer =>
          try {
            val reports = new ArrayBuffer[ValidationReport]()
            val endtime = System.currentTimeMillis() + TIMEOUT.toMillis
            while (!closed.get && reports.size < taskCount && System.currentTimeMillis() < endtime) {
              val records = consumer.poll(if (latch.getCount == 1) 100 millis else 500 millis)
              latch.countDown()
              records.foreach(
                (record: ConsumerRecord[String, Any]) =>
                  record.key
                    .filter(_.equals(requestId))
                    .map(_ => {
                      // must have value...by chia
                      record.value.get match {
                        case report: ValidationReport => reports += report
                        case _                        => throw new IllegalStateException(s"Unknown report:${record.value}")
                      }
                    }))
            }
            reports
          } finally latch.countDown()
        }
      }
      try {
        latch.await(TIMEOUT.toMillis, TimeUnit.MILLISECONDS)
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
          .config(TARGET, target)
          .build()
        try Await.result(future, TIMEOUT)
        finally connectorClient.delete(validationName)
      } finally closed.set(true)
    }
  }

  val CONFIG_DEF = new ConfigDef().define(TARGET, Type.STRING, null, Importance.HIGH, "target type")
}

import scala.collection.JavaConverters._
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
    } catch {
      case e: Throwable => toSourceRecord(ValidationReport(hostname, e.getMessage, false))
    } finally {
      done = true
    }

  override def stop(): Unit = {
    // do nothing
  }

  override def version(): String = VERSION

  private[this] def validate(info: HdfsValidationRequest): String = {
    val config = new Configuration()
    config.set("fs.defaultFS", info.uri)
    val fs = FileSystem.get(config)
    val home = fs.getHomeDirectory
    s"check the hdfs:${info.uri} by getting the home:${home}"
  }
  import com.island.ohara.io.CloseOnce._

  private[this] def validate(info: RdbValidationRequest): String =
    doClose(DriverManager.getConnection(info.uri, info.user, info.password)) { _ =>
      s"succeed to establish the connection:$info.uri"
    }

  private[this] def toJsObject: JsObject = JsObject(props.map { case (k, v) => (k, JsString(v)) })
  private[this] def information = require(TARGET) match {
    case TARGET_HDFS => ConfiguratorJson.HDFS_VALIDATION_REQUEST_JSON_FORMAT.read(toJsObject)
    case TARGET_RDB  => ConfiguratorJson.RDB_VALIDATION_REQUEST_JSON_FORMAT.read(toJsObject)
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
    props.get(key).getOrElse(throw new IllegalArgumentException(s"the $key is required"))

  private[this] def hostname: String = try java.net.InetAddress.getLocalHost().getHostName()
  catch {
    case _: Throwable => "unknown"
  }
}
