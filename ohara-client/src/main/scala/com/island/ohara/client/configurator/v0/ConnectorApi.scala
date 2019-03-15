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
import com.island.ohara.common.data.{Column, ConnectorState, DataType}
import com.island.ohara.kafka.connector.ConnectorUtils
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object ConnectorApi {
  val CONNECTORS_PREFIX_PATH: String = "connectors"
  val START_COMMAND: String = "start"
  val STOP_COMMAND: String = "stop"
  val PAUSE_COMMAND: String = "pause"
  val RESUME_COMMAND: String = "resume"

  implicit val COLUMN_JSON_FORMAT: RootJsonFormat[Column] = new RootJsonFormat[Column] {
    override def read(json: JsValue): Column = json.asJsObject.getFields("name", "newName", "dataType", "order") match {
      case Seq(JsString(n), JsString(nn), JsString(t), JsNumber(o)) =>
        Column.newBuilder().name(n).newName(nn).dataType(DataType.of(t)).order(o.toInt).build()
      case Seq(JsString(n), JsNull, JsString(t), JsNumber(o)) =>
        Column.newBuilder().name(n).newName(n).dataType(DataType.of(t)).order(o.toInt).build()
      case Seq(JsString(n), JsString(t), JsNumber(o)) =>
        Column.newBuilder().name(n).newName(n).dataType(DataType.of(t)).order(o.toInt).build()
      case _ => throw new UnsupportedOperationException(s"invalid format from ${classOf[Column].getSimpleName}")
    }
    override def write(obj: Column): JsValue = JsObject(
      "name" -> JsString(obj.name),
      "newName" -> JsString(obj.newName),
      "dataType" -> JsString(obj.dataType.name),
      "order" -> JsNumber(obj.order)
    )
  }

  // the following keys should be matched to the member name of ConnectorCreationRequest
  val NAME_KEY: String = "name"
  val CLASS_NAME_KEY: String = "className"
  // TODO: replace this by topicNames (https://github.com/oharastream/ohara/issues/444)
  val TOPIC_NAME_KEY: String = "topics"
  val NUMBER_OF_TASKS_KEY: String = "numberOfTasks"
  // TODO: replace this by columns (https://github.com/oharastream/ohara/issues/444)
  val COLUMNS_KEY: String = ConnectorUtils.COLUMNS_KEY
  final case class ConnectorCreationRequest(name: Option[String],
                                            workerClusterName: Option[String],
                                            className: String,
                                            // TODO: replace this by columns (https://github.com/oharastream/ohara/issues/444)
                                            schema: Seq[Column],
                                            // TODO: replace this by topicNames (https://github.com/oharastream/ohara/issues/444)
                                            topics: Seq[String],
                                            numberOfTasks: Int,
                                            configs: Map[String, String])

  implicit val CONNECTOR_CREATION_REQUEST_JSON_FORMAT: RootJsonFormat[ConnectorCreationRequest] = jsonFormat7(
    ConnectorCreationRequest)

  final case class ConnectorInfo(id: String,
                                 name: String,
                                 className: String,
                                 // TODO: replace this by columns (https://github.com/oharastream/ohara/issues/444)
                                 schema: Seq[Column],
                                 // TODO: replace this by topicNames (https://github.com/oharastream/ohara/issues/444)
                                 topics: Seq[String],
                                 numberOfTasks: Int,
                                 configs: Map[String, String],
                                 workerClusterName: String,
                                 state: Option[ConnectorState],
                                 error: Option[String],
                                 lastModified: Long)
      extends Data {
    override def kind: String = className
  }

  implicit val CONNECTOR_STATE_JSON_FORMAT: RootJsonFormat[com.island.ohara.common.data.ConnectorState] =
    new RootJsonFormat[com.island.ohara.common.data.ConnectorState] {
      override def write(obj: com.island.ohara.common.data.ConnectorState): JsValue = JsString(obj.name)
      override def read(json: JsValue): com.island.ohara.common.data.ConnectorState =
        com.island.ohara.common.data.ConnectorState.values
          .find(_.name == json.asInstanceOf[JsString].value)
          .getOrElse(throw new IllegalArgumentException(s"Unknown state name:${json.asInstanceOf[JsString].value}"))
    }

  class Access private[v0]
      extends com.island.ohara.client.configurator.v0.Access[ConnectorCreationRequest, ConnectorInfo](
        CONNECTORS_PREFIX_PATH) {

    private[this] def actionUrl(id: String, action: String): String =
      s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id/$action"

    /**
      * start to run a connector on worker cluster.
      * @param id connector's id
      * @return the configuration of connector
      */
    def start(id: String)(implicit executionContext: ExecutionContext): Future[ConnectorInfo] =
      exec.put[ConnectorInfo, ErrorApi.Error](actionUrl(id, START_COMMAND))

    /**
      * stop and remove a running connector.
      * @param id connector's id
      * @return the configuration of connector
      */
    def stop(id: String)(implicit executionContext: ExecutionContext): Future[ConnectorInfo] =
      exec.put[ConnectorInfo, ErrorApi.Error](actionUrl(id, STOP_COMMAND))

    /**
      * pause a running connector
      * @param id connector's id
      * @return the configuration of connector
      */
    def pause(id: String)(implicit executionContext: ExecutionContext): Future[ConnectorInfo] =
      exec.put[ConnectorInfo, ErrorApi.Error](actionUrl(id, PAUSE_COMMAND))

    /**
      * resume a paused connector
      * @param id connector's id
      * @return the configuration of connector
      */
    def resume(id: String)(implicit executionContext: ExecutionContext): Future[ConnectorInfo] =
      exec.put[ConnectorInfo, ErrorApi.Error](actionUrl(id, RESUME_COMMAND))
  }

  implicit val CONNECTOR_INFO_JSON_FORMAT: RootJsonFormat[ConnectorInfo] = jsonFormat11(ConnectorInfo)

  def access(): Access = new Access
}
