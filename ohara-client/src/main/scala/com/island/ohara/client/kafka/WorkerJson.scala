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
import com.island.ohara.kafka.connector.json.{Creation, Validation}
import spray.json.DefaultJsonProtocol.{jsonFormat2, jsonFormat3, jsonFormat4, _}
import spray.json.{DeserializationException, JsArray, JsNull, JsObject, JsString, JsValue, RootJsonFormat}

/**
  * a collection from marshalling/unmarshalling connector data to/from json.
  * NOTED: the json format is a part from PUBLIC INTERFACE so please don't change the field names after releasing the ohara.
  */
object WorkerJson {
  final case class Plugin(className: String, typeName: String, version: String)

  /**
    * this custom format is necessary since some keys in json are keywords in scala also...
    */
  private[kafka] implicit val PLUGIN_JSON_FORMAT: RootJsonFormat[Plugin] = new RootJsonFormat[Plugin] {
    private[this] val classKey: String = "class"
    private[this] val typeKey: String = "type"
    private[this] val versionKey: String = "version"

    override def read(json: JsValue): Plugin = json.asJsObject.getFields(classKey, typeKey, versionKey) match {
      case Seq(JsString(className), JsString(typeName), JsString(version)) =>
        Plugin(className, typeName, version)
      case other: Any => throw DeserializationException(s"${classOf[Plugin].getSimpleName} expected but $other")
    }
    override def write(obj: Plugin) = JsObject(
      classKey -> JsString(obj.className),
      typeKey -> JsString(obj.typeName),
      versionKey -> JsString(obj.version)
    )
  }
  final case class ConnectorTaskId(connector: String, task: Int)
  private[kafka] implicit val CONNECTOR_TASK_ID_JSON_FORMAT: RootJsonFormat[ConnectorTaskId] = jsonFormat2(
    ConnectorTaskId)

  final case class ConnectorCreationResponse(name: String, config: Map[String, String], tasks: Seq[ConnectorTaskId])

  private[kafka] implicit val CONNECTOR_CREATION_RESPONSE_JSON_FORMAT: RootJsonFormat[ConnectorCreationResponse] =
    jsonFormat3(ConnectorCreationResponse)
  final case class ConnectorStatus(state: String, worker_id: String, trace: Option[String]) {
    def workerHostname: String = {
      val splitIndex = worker_id.lastIndexOf(":")
      if (splitIndex < 0) worker_id else worker_id.substring(0, splitIndex)
    }
  }
  private[kafka] implicit val CONNECTOR_STATUS_JSON_FORMAT: RootJsonFormat[ConnectorStatus] = jsonFormat3(
    ConnectorStatus)
  final case class TaskStatus(id: Int, state: String, worker_id: String, trace: Option[String]) {
    def workerHostname: String = {
      val splitIndex = worker_id.lastIndexOf(":")
      if (splitIndex < 0) worker_id else worker_id.substring(0, splitIndex)
    }
  }
  private[kafka] implicit val TASK_STATUS_JSON_FORMAT: RootJsonFormat[TaskStatus] = jsonFormat4(TaskStatus)
  final case class ConnectorInfo(name: String, connector: ConnectorStatus, tasks: Seq[TaskStatus])
  private[kafka] implicit val CONNECTOR_INFO_JSON_FORMAT: RootJsonFormat[ConnectorInfo] = jsonFormat3(ConnectorInfo)

  final case class Error(error_code: Int, message: String) extends HttpExecutor.Error
  private[kafka] implicit val ERROR_RESPONSE_JSON_FORMAT: RootJsonFormat[Error] = jsonFormat2(Error)

  final case class ConnectorConfig(tasksMax: Int,
                                   topicNames: Set[String],
                                   connectorClass: String,
                                   args: Map[String, String])

  // open to ohara-configurator
  private[ohara] implicit val CONNECTOR_CONFIG_FORMAT: RootJsonFormat[ConnectorConfig] =
    new RootJsonFormat[ConnectorConfig] {
      private[this] val taskMaxKey: String = "tasks.max"
      private[this] val topicNamesKey: String = "topics"
      private[this] val connectClassKey: String = "connector.class"

      override def read(json: JsValue): ConnectorConfig =
        json.asJsObject.getFields(taskMaxKey, topicNamesKey, connectClassKey) match {
          // worker saves tasksMax as string
          case Seq(JsString(tasksMax), JsString(topicNames), JsString(connectorClass)) =>
            ConnectorConfig(
              tasksMax = tasksMax.toInt,
              topicNames = topicNames.split(",").toSet,
              connectorClass = connectorClass,
              args = json.convertTo[Map[String, String]] - (taskMaxKey, topicNamesKey, connectClassKey)
            )
          case other: Any =>
            throw DeserializationException(s"${classOf[ConnectorConfig].getSimpleName} expected but $other")
        }
      override def write(config: ConnectorConfig): JsValue =
        JsObject(
          config.args.map(f => f._1 -> JsString(f._2)) + (taskMaxKey -> JsString(config.tasksMax.toString),
          topicNamesKey -> JsString(config.topicNames.mkString(",")),
          connectClassKey -> JsString(config.connectorClass))
        )
    }

  case class ValidatedValue(name: String, value: Option[String], errors: Seq[String])

  private[kafka] implicit val VALIDATED_VALUE_FORMAT: RootJsonFormat[ValidatedValue] =
    new RootJsonFormat[ValidatedValue] {
      private[this] val nameKey: String = "name"
      private[this] val valueKey: String = "value"
      private[this] val errorsKey: String = "errors"

      override def read(json: JsValue): ValidatedValue = json.asJsObject.getFields(nameKey, errorsKey) match {
        case Seq(JsString(name), JsArray(errors)) =>
          ValidatedValue(
            name = name,
            value = json.asJsObject.fields
              .get(valueKey)
              .flatMap {
                case v: JsString => Some(v.value)
                case JsNull      => None
                case other: Any  => throw DeserializationException(s"unknown format of $valueKey from $other")
              }
              .filter(_.nonEmpty),
            errors = errors.map {
              case error: JsString => error.value
              case _               => throw DeserializationException(s"unknown format of errors:$errors")
            }
          )
        case other: Any =>
          throw DeserializationException(s"${classOf[ValidatedValue].getSimpleName} expected but $other")
      }
      override def write(obj: ValidatedValue) = JsObject(
        nameKey -> JsString(obj.name),
        valueKey -> obj.value.map(JsString(_)).getOrElse(JsNull),
        errorsKey -> JsArray(obj.errors.map(JsString(_)).toVector)
      )
    }

  private[kafka] implicit val CREATION_JSON_FORMAT: RootJsonFormat[Creation] = new RootJsonFormat[Creation] {
    import spray.json._
    override def write(obj: Creation): JsValue = obj.toJsonString.parseJson
    override def read(json: JsValue): Creation = Creation.ofJson(json.toString())
  }

  private[kafka] implicit val VALIDATION_JSON_FORMAT: RootJsonFormat[Validation] = new RootJsonFormat[Validation] {
    import spray.json._
    override def write(obj: Validation): JsValue = obj.toJsonString.parseJson
    override def read(json: JsValue): Validation = Validation.ofJson(json.toString())
  }
}
