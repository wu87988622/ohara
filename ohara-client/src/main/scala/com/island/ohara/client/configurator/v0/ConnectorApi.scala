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
import spray.json.DefaultJsonProtocol._
import spray.json.{JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.Future

object ConnectorApi {
  val CONNECTORS_PREFIX_PATH: String = "connectors"
  val START_COMMAND: String = "start"
  val STOP_COMMAND: String = "stop"
  val PAUSE_COMMAND: String = "pause"
  val RESUME_COMMAND: String = "resume"

  implicit val COLUMN_JSON_FORMAT: RootJsonFormat[Column] = new RootJsonFormat[Column] {
    override def read(json: JsValue): Column = json.asJsObject.getFields("name", "newName", "dataType", "order") match {
      case Seq(JsString(n), JsString(nn), JsString(t), JsNumber(o)) => Column.of(n, nn, DataType.of(t), o.toInt)
      case Seq(JsString(n), JsNull, JsString(t), JsNumber(o))       => Column.of(n, n, DataType.of(t), o.toInt)
      case Seq(JsString(n), JsString(t), JsNumber(o))               => Column.of(n, n, DataType.of(t), o.toInt)
      case _                                                        => throw new UnsupportedOperationException(s"invalid format from ${classOf[Column].getSimpleName}")
    }
    override def write(obj: Column): JsValue = JsObject(
      "name" -> JsString(obj.name),
      "newName" -> JsString(obj.newName),
      "dataType" -> JsString(obj.dataType.name),
      "order" -> JsNumber(obj.order)
    )
  }
  final case class ConnectorConfigurationRequest(name: String,
                                                 className: String,
                                                 schema: Seq[Column],
                                                 topics: Seq[String],
                                                 numberOfTasks: Int,
                                                 configs: Map[String, String])
  implicit val CONNECTOR_CONFIGURATION_REQUEST_JSON_FORMAT: RootJsonFormat[ConnectorConfigurationRequest] = jsonFormat6(
    ConnectorConfigurationRequest)

  final case class ConnectorConfiguration(id: String,
                                          name: String,
                                          className: String,
                                          schema: Seq[Column],
                                          topics: Seq[String],
                                          numberOfTasks: Int,
                                          configs: Map[String, String],
                                          state: Option[ConnectorState],
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

  sealed abstract class Access
      extends com.island.ohara.client.configurator.v0.Access[ConnectorConfigurationRequest, ConnectorConfiguration](
        CONNECTORS_PREFIX_PATH) {

    /**
      * start to run a connector on worker cluster.
      * @param id connector's id
      * @return the configuration of connector
      */
    def start(id: String): Future[ConnectorConfiguration]

    /**
      * stop and remove a running connector.
      * @param id connector's id
      * @return the configuration of connector
      */
    def stop(id: String): Future[ConnectorConfiguration]

    /**
      * pause a running connector
      * @param id connector's id
      * @return the configuration of connector
      */
    def pause(id: String): Future[ConnectorConfiguration]

    /**
      * resume a paused connector
      * @param id connector's id
      * @return the configuration of connector
      */
    def resume(id: String): Future[ConnectorConfiguration]
  }

  implicit val CONNECTOR_CONFIGURATION_JSON_FORMAT: RootJsonFormat[ConnectorConfiguration] = jsonFormat9(
    ConnectorConfiguration)
  def access(): Access = new Access {
    private[this] def url(id: String, action: String): String =
      s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$id/$action"
    override def start(id: String): Future[ConnectorConfiguration] =
      exec.put[ConnectorConfiguration](url(id, START_COMMAND))
    override def stop(id: String): Future[ConnectorConfiguration] =
      exec.put[ConnectorConfiguration](url(id, STOP_COMMAND))
    override def pause(id: String): Future[ConnectorConfiguration] =
      exec.put[ConnectorConfiguration](url(id, PAUSE_COMMAND))
    override def resume(id: String): Future[ConnectorConfiguration] =
      exec.put[ConnectorConfiguration](url(id, RESUME_COMMAND))
  }
}
