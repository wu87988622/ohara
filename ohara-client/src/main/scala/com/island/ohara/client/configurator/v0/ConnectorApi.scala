/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.client.configurator.v0
import com.island.ohara.client.Enum
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.data.{Column, DataType}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.{PropGroups, SettingDefinition, StringList}
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object ConnectorApi {
  val CONNECTORS_PREFIX_PATH: String = "connectors"
  val START_COMMAND: String = "start"
  val STOP_COMMAND: String = "stop"
  val PAUSE_COMMAND: String = "pause"
  val RESUME_COMMAND: String = "resume"

  /**
    * The name is a part of "Restful APIs" so "DON'T" change it arbitrarily
    */
  abstract sealed class ConnectorState(val name: String)
  object ConnectorState extends Enum[ConnectorState] {
    case object UNASSIGNED extends ConnectorState("UNASSIGNED")
    case object RUNNING extends ConnectorState("RUNNING")
    case object PAUSED extends ConnectorState("PAUSED")
    case object FAILED extends ConnectorState("FAILED")
    case object DESTROYED extends ConnectorState("DESTROYED")
  }
  // TODO: remove this format after ohara manager starts to use new APIs
  implicit val COLUMN_JSON_FORMAT: RootJsonFormat[Column] = new RootJsonFormat[Column] {
    override def read(json: JsValue): Column = json.asJsObject.getFields("name", "newName", "dataType", "order") match {
      case Seq(JsString(n), JsString(nn), JsString(t), JsNumber(o)) =>
        Column.builder().name(n).newName(nn).dataType(DataType.valueOf(t.toUpperCase())).order(o.toInt).build()
      case Seq(JsString(n), JsNull, JsString(t), JsNumber(o)) =>
        Column.builder().name(n).newName(n).dataType(DataType.valueOf(t.toUpperCase())).order(o.toInt).build()
      case Seq(JsString(n), JsString(t), JsNumber(o)) =>
        Column.builder().name(n).newName(n).dataType(DataType.valueOf(t.toUpperCase)).order(o.toInt).build()
      case _ => throw new UnsupportedOperationException(s"invalid format from ${classOf[Column].getSimpleName}")
    }
    override def write(obj: Column): JsValue = JsObject(
      "name" -> JsString(obj.name),
      "newName" -> JsString(obj.newName),
      "dataType" -> JsString(obj.dataType.name),
      "order" -> JsNumber(obj.order)
    )
  }

  final case class Creation(settings: Map[String, JsValue]) extends CreationRequest {

    /**
      * Convert all json value to plain string. It keeps the json format but all stuff are in string.
      */
    def plain: Map[String, String] = noJsNull(settings).map {
      case (k, v) =>
        k -> (v match {
          case JsString(value) => value
          case _               => v.toString()
        })
    }
    def className: String = plain(SettingDefinition.CONNECTOR_CLASS_DEFINITION.key())
    def columns: Seq[Column] = plain
      .get(SettingDefinition.COLUMNS_DEFINITION.key())
      .map(s => PropGroups.ofJson(s).toColumns.asScala)
      .getOrElse(Seq.empty)
    def changeNumberOfTasks(newValue: Int): Creation = Creation(
      settings = this.settings ++ Map(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key() -> JsNumber(newValue))
    )
    def numberOfTasks: Option[Int] = plain.get(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key()).map(_.toInt)
    def workerClusterName: Option[String] = plain.get(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key())
    def topicNames: Seq[String] =
      plain
        .get(SettingDefinition.TOPIC_NAMES_DEFINITION.key())
        .map(s => StringList.ofJson(s).asScala)
        .getOrElse(Seq.empty)
    override def name: String = plain(SettingDefinition.CONNECTOR_NAME_DEFINITION.key())
  }

  implicit val CONNECTOR_CREATION_REQUEST_JSON_FORMAT: RootJsonFormat[Creation] =
    new RootJsonFormat[Creation] {
      override def write(obj: Creation): JsValue = JsObject(noJsNull(obj.settings))
      override def read(json: JsValue): Creation = Creation(noJsNull(json.asJsObject.fields))
    }

  import MetricsApi._

  /**
    * this is what we store in configurator
    */
  final case class ConnectorDescription(settings: Map[String, JsValue],
                                        state: Option[ConnectorState],
                                        error: Option[String],
                                        metrics: Metrics,
                                        lastModified: Long)
      extends Data {

    /**
      * Convert all json value to plain string. It keeps the json format but all stuff are in string.
      */
    def plain: Map[String, String] = noJsNull(settings).map {
      case (k, v) =>
        k -> (v match {
          case JsString(value) => value
          case _               => v.toString()
        })
    }

    override def id: String = name
    override def name: String = plain(SettingDefinition.CONNECTOR_NAME_DEFINITION.key())
    override def kind: String = "connector"
    def className: String = plain(SettingDefinition.CONNECTOR_CLASS_DEFINITION.key())

    def columns: Seq[Column] = plain
      .get(SettingDefinition.COLUMNS_DEFINITION.key())
      .map(s => PropGroups.ofJson(s).toColumns.asScala)
      .getOrElse(Seq.empty)
    def numberOfTasks: Int = plain(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key()).toInt
    def workerClusterName: String = plain(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key())
    def topicNames: Seq[String] =
      plain
        .get(SettingDefinition.TOPIC_NAMES_DEFINITION.key())
        .map(s => StringList.ofJson(s).asScala)
        .getOrElse(Seq.empty)
  }

  implicit val CONNECTOR_STATE_JSON_FORMAT: RootJsonFormat[ConnectorState] =
    new RootJsonFormat[ConnectorState] {
      override def write(obj: ConnectorState): JsValue = JsString(obj.name)
      override def read(json: JsValue): ConnectorState =
        ConnectorState.forName(json.asInstanceOf[JsString].value)
    }

  implicit val CONNECTOR_DESCRIPTION_JSON_FORMAT: RootJsonFormat[ConnectorDescription] =
    new RootJsonFormat[ConnectorDescription] {
      private[this] val format = jsonFormat5(ConnectorDescription)
      override def read(json: JsValue): ConnectorDescription = format.read(json)
      override def write(obj: ConnectorDescription): JsValue = JsObject(
        // TODO: remove the id
        format.write(obj).asJsObject.fields ++ Map("id" -> JsString(obj.name)))
    }

  /**
    * used to generate the payload and url for POST/PUT request.
    * This basic class is used to collect settings of connector. It is also used by validation so we extract the same behavior from Request
    */
  abstract class BasicRequest {
    protected[this] var name: String = _
    protected[this] var className: String = _
    protected[this] var columns: Seq[Column] = _
    protected[this] var topicNames: Seq[String] = _
    protected[this] var numberOfTasks: Option[Int] = None
    protected[this] var settings: Map[String, String] = Map.empty
    protected[this] var workerClusterName: String = _
    def name(name: String): BasicRequest.this.type = {
      this.name = CommonUtils.requireNonEmpty(name)
      this
    }
    @Optional(
      "You don't need to fill this field when update/create connector. But this filed is required in starting connector")
    def className(className: String): BasicRequest.this.type = {
      this.className = CommonUtils.requireNonEmpty(className)
      this
    }
    @Optional("Not all connectors demand this field. See connectors document for more details")
    def columns(columns: Seq[Column]): BasicRequest.this.type = {
      import scala.collection.JavaConverters._
      this.columns = CommonUtils.requireNonEmpty(columns.asJava).asScala
      this
    }
    @Optional(
      "You don't need to fill this field when update/create connector. But this filed is required in starting connector")
    def topicName(topicName: String): BasicRequest.this.type = topicNames(Seq(CommonUtils.requireNonEmpty(topicName)))
    @Optional(
      "You don't need to fill this field when update/create connector. But this filed is required in starting connector")
    def topicNames(topicNames: Seq[String]): BasicRequest.this.type = {
      import scala.collection.JavaConverters._
      this.topicNames = CommonUtils.requireNonEmpty(topicNames.asJava).asScala
      this
    }
    @Optional(
      "You don't need to fill this field when update/create connector. But this filed is required in starting connector")
    def numberOfTasks(numberOfTasks: Int): BasicRequest.this.type = {
      this.numberOfTasks = Some(CommonUtils.requirePositiveInt(numberOfTasks))
      this
    }
    @Optional("server will match a worker cluster for you if the wk name is ignored")
    def workerClusterName(workerClusterName: String): BasicRequest.this.type = {
      this.workerClusterName = CommonUtils.requireNonEmpty(workerClusterName)
      this
    }
    @Optional("extra settings for this connectors")
    def setting(key: String, value: String): BasicRequest.this.type = settings(
      Map(CommonUtils.requireNonEmpty(key) -> CommonUtils.requireNonEmpty(value)))
    @Optional("extra settings for this connectors")
    def settings(settings: Map[String, String]): BasicRequest.this.type = {
      import scala.collection.JavaConverters._
      this.settings = CommonUtils.requireNonEmpty(settings.asJava).asScala.toMap
      this
    }

    import spray.json._

    /**
      * generate the payload for request. It removes the ignored fields and keeping all value in json representation.
      * This method is exposed to sub classes since this generation is not friendly and hence we should reuse it as much as possible.
      * @return creation object
      */
    protected def creation(): Creation = Creation(
      settings = (settings.map {
        case (k, v) => k -> JsString(v)
      } ++ Map(
        SettingDefinition.CONNECTOR_NAME_DEFINITION.key() -> JsString(CommonUtils.requireNonEmpty(name)),
        SettingDefinition.CONNECTOR_CLASS_DEFINITION.key() -> (if (className == null) JsNull
                                                               else JsString(CommonUtils.requireNonEmpty(className))),
        SettingDefinition.COLUMNS_DEFINITION.key() -> (if (columns == null) JsNull
                                                       else if (columns.isEmpty) JsArray()
                                                       else
                                                         PropGroups.ofColumns(columns.asJava).toJsonString.parseJson),
        SettingDefinition.TOPIC_NAMES_DEFINITION.key() -> (if (topicNames == null) JsNull
                                                           else if (topicNames.isEmpty) JsArray()
                                                           else StringList.toJsonString(topicNames.asJava).parseJson),
        SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key() -> numberOfTasks.map(JsNumber(_)).getOrElse(JsNull),
        SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION
          .key() -> (if (workerClusterName == null) JsNull
                     else JsString(CommonUtils.requireNonEmpty(workerClusterName)))
      )).filter {
        case (_, value) =>
          value match {
            case JsNull => false
            case _      => true
          }
      }
    )
  }

  /**
    * The do-action methods are moved from BasicRequest to this one. Hence, ValidationApi ConnectorRequest does not have those weired methods
    */
  abstract class Request extends BasicRequest {

    /**
      * generate the POST request
      * @param executionContext thread pool
      * @return created data
      */
    def create()(implicit executionContext: ExecutionContext): Future[ConnectorDescription]

    /**
      * generate the PUT request
      * @param executionContext thread pool
      * @return updated/created data
      */
    def update()(implicit executionContext: ExecutionContext): Future[ConnectorDescription]
  }

  class Access private[v0]
      extends com.island.ohara.client.configurator.v0.Access2[ConnectorDescription](CONNECTORS_PREFIX_PATH) {

    private[this] def actionUrl(id: String, action: String): String =
      s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id/$action"

    /**
      * start to run a connector on worker cluster.
      *
      * @param id connector's id
      * @return the configuration of connector
      */
    def start(id: String)(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
      exec.put[ConnectorDescription, ErrorApi.Error](actionUrl(id, START_COMMAND))

    /**
      * stop and remove a running connector.
      *
      * @param id connector's id
      * @return the configuration of connector
      */
    def stop(id: String)(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
      exec.put[ConnectorDescription, ErrorApi.Error](actionUrl(id, STOP_COMMAND))

    /**
      * pause a running connector
      *
      * @param id connector's id
      * @return the configuration of connector
      */
    def pause(id: String)(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
      exec.put[ConnectorDescription, ErrorApi.Error](actionUrl(id, PAUSE_COMMAND))

    /**
      * resume a paused connector
      *
      * @param id connector's id
      * @return the configuration of connector
      */
    def resume(id: String)(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
      exec.put[ConnectorDescription, ErrorApi.Error](actionUrl(id, RESUME_COMMAND))

    def request(): Request = new Request {
      override def create()(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
        exec.post[Creation, ConnectorDescription, ErrorApi.Error](_url, creation())

      override def update()(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
        exec.put[Creation, ConnectorDescription, ErrorApi.Error](s"${_url}/${CommonUtils.requireNonEmpty(name)}",
                                                                 creation())
    }
  }

  def access(): Access = new Access
}
