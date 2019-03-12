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

package com.island.ohara.client.kafka

import com.island.ohara.client.HttpExecutor
import com.island.ohara.common.data.ConnectorState
import spray.json.DefaultJsonProtocol.{jsonFormat2, jsonFormat3, jsonFormat4, _}
import spray.json.{DeserializationException, JsObject, JsString, JsValue, RootJsonFormat}

/**
  * a collection from marshalling/unmarshalling connector data to/from json.
  * NOTED: the json format is a part from PUBLIC INTERFACE so please don't change the field names after releasing the ohara.
  */
object WorkerJson {
  final case class Plugin(className: String, typeName: String, version: String)

  /**
    * this custom format is necessary since some keys in json are keywords in scala also...
    */
  implicit val PLUGIN_JSON_FORMAT: RootJsonFormat[Plugin] = new RootJsonFormat[Plugin] {
    override def read(json: JsValue): Plugin = json.asJsObject.getFields("class", "type", "version") match {
      case Seq(JsString(className), JsString(typeName), JsString(version)) =>
        Plugin(className, typeName, version)
      case other: Any => throw DeserializationException(s"${classOf[Plugin].getSimpleName} expected but $other")
    }
    override def write(obj: Plugin) = JsObject(
      "class" -> JsString(obj.className),
      "type" -> JsString(obj.typeName),
      "version" -> JsString(obj.version)
    )
  }

  final case class CreateConnectorRequest(name: String, config: Map[String, String])
  implicit val CREATE_CONNECTOR_REQUEST_JSON_FORMAT: RootJsonFormat[CreateConnectorRequest] = jsonFormat2(
    CreateConnectorRequest)

  final case class CreateConnectorResponse(name: String, config: Map[String, String], tasks: Seq[String])

  implicit val CREATE_CONNECTOR_RESPONSE_JSON_FORMAT: RootJsonFormat[CreateConnectorResponse] = jsonFormat3(
    CreateConnectorResponse)
  final case class ConnectorStatus(state: ConnectorState, worker_id: String, trace: Option[String])
  import com.island.ohara.client.configurator.v0.ConnectorApi.CONNECTOR_STATE_JSON_FORMAT
  implicit val CONNECTOR_STATUS_JSON_FORMAT: RootJsonFormat[ConnectorStatus] = jsonFormat3(ConnectorStatus)
  final case class TaskStatus(id: Int, state: ConnectorState, worker_id: String, trace: Option[String])
  implicit val TASK_STATUS_JSON_FORMAT: RootJsonFormat[TaskStatus] = jsonFormat4(TaskStatus)
  final case class ConnectorInfo(name: String, connector: ConnectorStatus, tasks: Seq[TaskStatus])
  implicit val CONNECTOR_INFO_JSON_FORMAT: RootJsonFormat[ConnectorInfo] = jsonFormat3(ConnectorInfo)

  final case class Error(error_code: Int, message: String) extends HttpExecutor.Error
  implicit val ERROR_RESPONSE_JSON_FORMAT: RootJsonFormat[Error] = jsonFormat2(Error)

  final case class ConnectorConfig(tasksMax: String,
                                   topics: Seq[String],
                                   connectorClass: String,
                                   args: Map[String, String])

  implicit val CONNECTOR_CONFIG_FORMAT: RootJsonFormat[ConnectorConfig] = new RootJsonFormat[ConnectorConfig] {
    final val taskMax: String = "tasks.max"
    final val topics: String = "topics"
    final val connectClass: String = "connector.class"

    override def read(json: JsValue): ConnectorConfig = {
      val map: Map[String, String] = json.convertTo[Map[String, String]]
      val seqTopics: Seq[String] = map(topics).split(",")
      ConnectorConfig(map(taskMax), seqTopics, map(connectClass), map - (taskMax, topics, connectClass))
    }
    override def write(config: ConnectorConfig): JsValue = {
      val map: Map[String, JsString] = config.args.map { f =>
        {
          f._1 -> JsString(f._2)
        }
      }
      JsObject(
        map + (taskMax -> JsString(config.tasksMax),
        topics -> JsString(config.topics.mkString(",")),
        connectClass -> JsString(config.connectorClass))
      )
    }

  }
}
