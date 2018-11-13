package com.island.ohara.client

import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsArray, JsNull, JsObject, JsString, JsValue, RootJsonFormat}

/**
  * a collection from marshalling/unmarshalling connector data to/from json.
  * NOTED: the json format is a part from PUBLIC INTERFACE so please don't change the field names after releasing the ohara.
  */
object ConnectorJson {
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

  final case class CreateConnectorResponse(name: String,
                                           config: Map[String, String],
                                           tasks: Seq[String],
                                           typeName: String)

  /**
    * this custom format is necessary since some keys in json are keywords in scala also...
    */
  implicit val CREATE_CONNECTOR_RESPONSE_JSON_FORMAT: RootJsonFormat[CreateConnectorResponse] =
    new RootJsonFormat[CreateConnectorResponse] {
      override def read(json: JsValue): CreateConnectorResponse =
        json.asJsObject.getFields("name", "config", "tasks", "type") match {
          case Seq(JsString(className), JsObject(config), JsArray(tasks), JsString(typeName)) =>
            CreateConnectorResponse(className,
                                    config.map { case (k, v) => (k, v.toString) },
                                    tasks.map(_.toString),
                                    typeName)
          // TODO: this is a kafka bug which always returns null in type name. see KAFKA-7253  by chia
          case Seq(JsString(className), JsObject(config), JsArray(tasks), JsNull) =>
            CreateConnectorResponse(
              className,
              // it is ok to cast JsValue to JsString since we serialize the config to (JsString, JsString)
              config.map { case (k, v) => (k, v.asInstanceOf[JsString].value) },
              tasks.map(_.toString),
              "null"
            )
          case other: Any =>
            throw DeserializationException(s"${classOf[CreateConnectorResponse].getSimpleName} expected but $other")
        }

      override def write(obj: CreateConnectorResponse) = JsObject(
        "class" -> JsString(obj.name),
        "config" -> JsObject(obj.config.map { case (k, v) => (k, JsString(v)) }),
        "tasks" -> JsArray(obj.tasks.map(JsString(_)): _*),
        "type" -> JsString(obj.typeName)
      )
    }

  /**
    * the enumeration is referenced to org.apache.kafka.connect.runtime.WorkerConnector.State
    */
  abstract sealed class State extends Serializable {
    // adding a field to display the name from enumeration avoid we break the compatibility when moving code...
    val name: String
  }

  object State {
    case object UNASSIGNED extends State {
      val name = "UNASSIGNED"
    }

    case object RUNNING extends State {
      val name = "RUNNING"
    }

    case object PAUSED extends State {
      val name = "PAUSED"
    }

    case object FAILED extends State {
      val name = "FAILED"
    }

    case object DESTROYED extends State {
      val name = "DESTROYED"
    }

    val all: Seq[State] = Seq(
      UNASSIGNED,
      RUNNING,
      PAUSED,
      FAILED,
      DESTROYED
    )

  }
  implicit val STATE_JSON_FORMAT: RootJsonFormat[State] = new RootJsonFormat[State] {
    override def write(obj: State): JsValue = JsString(obj.name)
    override def read(json: JsValue): State = State.all
      .find(_.name == json.asInstanceOf[JsString].value)
      .getOrElse(throw new IllegalArgumentException(s"Unknown state name:${json.asInstanceOf[JsString].value}"))
  }

  final case class ConnectorStatus(state: State, worker_id: String, trace: Option[String])
  implicit val CONNECTOR_STATUS_JSON_FORMAT: RootJsonFormat[ConnectorStatus] = jsonFormat3(ConnectorStatus)
  final case class TaskStatus(id: Int, state: State, worker_id: String, trace: Option[String])
  implicit val TASK_STATUS_JSON_FORMAT: RootJsonFormat[TaskStatus] = jsonFormat4(TaskStatus)
  final case class ConnectorInformation(name: String, connector: ConnectorStatus, tasks: Seq[TaskStatus])
  implicit val CONNECTOR_INFORMATION_JSON_FORMAT: RootJsonFormat[ConnectorInformation] = jsonFormat3(
    ConnectorInformation)

  final case class ErrorResponse(error_code: Int, message: String)
  implicit val ERROR_RESPONSE_JSON_FORMAT: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)

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
