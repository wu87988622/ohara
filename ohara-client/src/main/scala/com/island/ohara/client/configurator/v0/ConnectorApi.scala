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
import java.util.Objects

import com.island.ohara.client.Enum
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.data.Column
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json._
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsArray, JsNull, JsObject, JsString, JsValue, RootJsonFormat}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object ConnectorApi {
  val GROUP_DEFAULT: String = Data.GROUP_DEFAULT
  val CONNECTORS_PREFIX_PATH: String = "connectors"
  val DEFAULT_NUMBER_OF_TASKS = 1

  /**
    * The name is a part of "Restful APIs" so "DON'T" change it arbitrarily
    */
  // Make this class to be serializable since it's stored in configurator
  abstract sealed class ConnectorState(val name: String) extends Serializable
  object ConnectorState extends Enum[ConnectorState] {
    case object UNASSIGNED extends ConnectorState("UNASSIGNED")
    case object RUNNING extends ConnectorState("RUNNING")
    case object PAUSED extends ConnectorState("PAUSED")
    case object FAILED extends ConnectorState("FAILED")
    case object DESTROYED extends ConnectorState("DESTROYED")
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
    def numberOfTasks: Int = plain(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key()).toInt
    def workerClusterName: Option[String] = plain.get(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key())

    /**
      * TODO: remove this old key parser ... by chia
      */
    private[this] def topicKeysFromTopicNames: Set[TopicKey] =
      plain
        .get(SettingDefinition.TOPIC_NAMES_DEFINITION.key())
        .map(s => StringList.ofJson(s).asScala.toSet)
        .map(_.map(TopicKey.of(GROUP_DEFAULT, _)))
        .getOrElse(Set.empty)
    def topicKeys: Set[TopicKey] =
      noJsNull(settings)
        .get(SettingDefinition.TOPIC_KEYS_DEFINITION.key())
        .map(_.convertTo[Set[TopicKey]])
        .getOrElse(topicKeysFromTopicNames)

    override def group: String = plain(Data.GROUP_KEY)
    override def name: String = plain(SettingDefinition.CONNECTOR_NAME_DEFINITION.key())

    def key: ConnectorKey = ConnectorKey.of(group, name)

    override def tags: Map[String, JsValue] = noJsNull(settings)
      .get(SettingDefinition.TAGS_DEFINITION.key())
      .map {
        case s: JsObject => s.fields
        case other: JsValue =>
          throw new IllegalArgumentException(s"the type of tags should be JsObject, actual type is ${other.getClass}")
      }
      .getOrElse(Map.empty)
  }

  implicit val CONNECTOR_CREATION_FORMAT: OharaJsonFormat[Creation] = JsonRefiner[Creation]
    .format(new RootJsonFormat[Creation] {
      override def write(obj: Creation): JsValue = JsObject(noJsNull(obj.settings))
      override def read(json: JsValue): Creation = Creation(json.asJsObject.fields)
    })
    // set the default number of tasks
    .nullToInt(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key(), DEFAULT_NUMBER_OF_TASKS)
    .rejectEmptyString()
    .nullToString(Data.GROUP_KEY, () => GROUP_DEFAULT)
    .nullToString(Data.NAME_KEY, () => CommonUtils.randomString(10))
    .nullToEmptyObject(Data.TAGS_KEY)
    .valueChecker(
      SettingDefinition.COLUMNS_DEFINITION.key(), {
        case v: JsArray if v.elements.nonEmpty =>
          try {
            val columns = PropGroups.ofJson(v.toString()).toColumns.asScala
            // name can't be empty
            if (columns.exists(_.name().isEmpty))
              throw DeserializationException(msg = s"name can't be empty", fieldNames = List("name"))
            // newName can't be empty
            if (columns.exists(_.newName().isEmpty))
              throw DeserializationException(msg = s"newName can't be empty", fieldNames = List("newName"))
            // order can't be negative number
            if (columns.exists(_.order() < 0))
              throw DeserializationException(msg = s"order can't be negative number", fieldNames = List("order"))
            // order can't be duplicate
            if (columns.map(_.order).toSet.size != columns.size)
              throw DeserializationException(msg = s"duplicate order:${columns.map(_.order)}",
                                             fieldNames = List("order"))
          } catch {
            case e: DeserializationException => throw e
            case other: Throwable =>
              throw DeserializationException(
                msg = s"the string to ${SettingDefinition.COLUMNS_DEFINITION.key()} is not correct format",
                cause = other,
                fieldNames = List(SettingDefinition.COLUMNS_DEFINITION.key())
              )
          }
        case _ => // do nothing
      }
    )
    .refine

  final case class Update(settings: Map[String, JsValue]) {
    def workerClusterName: Option[String] = Creation(settings).workerClusterName
  }

  implicit val CONNECTOR_UPDATE_FORMAT: RootJsonFormat[Update] = JsonRefiner[Update]
    .format(new RootJsonFormat[Update] {
      override def write(obj: Update): JsValue = JsObject(noJsNull(obj.settings))
      override def read(json: JsValue): Update = Update(json.asJsObject.fields)
    })
    .rejectEmptyString()
    .valueChecker(
      SettingDefinition.COLUMNS_DEFINITION.key(), {
        case v: JsArray => CONNECTOR_CREATION_FORMAT.check(SettingDefinition.COLUMNS_DEFINITION.key(), v)
        case _          => // do nothing
      }
    )
    .refine

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

    override def key: ConnectorKey = ConnectorKey.of(group, name)

    override def group: String = plain(Data.GROUP_KEY)

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

    override def name: String = plain(SettingDefinition.CONNECTOR_NAME_DEFINITION.key())
    override def kind: String = "connector"
    def className: String = plain(SettingDefinition.CONNECTOR_CLASS_DEFINITION.key())

    def columns: Seq[Column] = plain
      .get(SettingDefinition.COLUMNS_DEFINITION.key())
      .map(s => PropGroups.ofJson(s).toColumns.asScala)
      .getOrElse(Seq.empty)
    def numberOfTasks: Int = plain(SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key()).toInt
    def workerClusterName: Option[String] = plain.get(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key())

    /**
      * TODO: remove this old key parser ... by chia
      */
    private[this] def topicKeysFromTopicNames: Set[TopicKey] =
      plain
        .get(SettingDefinition.TOPIC_NAMES_DEFINITION.key())
        .map(s => StringList.ofJson(s).asScala.toSet)
        .map(_.map(TopicKey.of(GROUP_DEFAULT, _)))
        .getOrElse(Set.empty)
    def topicKeys: Set[TopicKey] =
      noJsNull(settings)
        .get(SettingDefinition.TOPIC_KEYS_DEFINITION.key())
        .map(_.convertTo[Set[TopicKey]])
        .getOrElse(topicKeysFromTopicNames)
    override def tags: Map[String, JsValue] = noJsNull(settings)
      .get(SettingDefinition.TAGS_DEFINITION.key())
      .map {
        case s: JsObject => s.fields
        case other: JsValue =>
          throw new IllegalArgumentException(s"the type of tags should be JsObject, actual type is ${other.getClass}")
      }
      .getOrElse(Map.empty)
  }

  implicit val CONNECTOR_STATE_FORMAT: RootJsonFormat[ConnectorState] =
    new RootJsonFormat[ConnectorState] {
      override def write(obj: ConnectorState): JsValue = JsString(obj.name)
      override def read(json: JsValue): ConnectorState =
        ConnectorState.forName(json.convertTo[String])
    }

  implicit val CONNECTOR_DESCRIPTION_FORMAT: RootJsonFormat[ConnectorDescription] =
    new RootJsonFormat[ConnectorDescription] {
      private[this] val format = jsonFormat5(ConnectorDescription)
      override def read(json: JsValue): ConnectorDescription = format.read(json)

      override def write(obj: ConnectorDescription): JsValue =
        JsObject(
          noJsNull(
            format.write(obj).asJsObject.fields ++
              // TODO: the group should be equal to workerClusterName ... by chia
              Map(Data.GROUP_KEY -> JsString(GROUP_DEFAULT),
                  Data.NAME_KEY -> obj.settings.getOrElse(Data.NAME_KEY, JsNull))))
    }

  /**
    * used to generate the payload and url for POST/PUT request.
    * This basic class is used to collect settings of connector. It is also used by validation so we extract the same behavior from Request.
    * We use private[v0] instead of "sealed" since it is extendable to ValidationApi.
    */
  abstract class BasicRequest private[v0] {
    protected[this] var group: String = GROUP_DEFAULT
    protected[this] var name: String = _
    protected[this] var className: String = _
    protected[this] var columns: Seq[Column] = _
    protected[this] var topicKeys: Set[TopicKey] = _
    protected[this] var numberOfTasks: Int = DEFAULT_NUMBER_OF_TASKS
    protected[this] var settings: Map[String, String] = Map.empty
    protected[this] var workerClusterName: String = _
    protected[this] var tags: Map[String, JsValue] = _

    /**
      * set the group and name via key
      * @param objectKey object key
      * @return this request
      */
    def key(objectKey: ObjectKey): BasicRequest.this.type = {
      group(objectKey.group())
      name(objectKey.name())
    }

    @Optional("default group is \"default\"")
    def group(group: String): BasicRequest.this.type = {
      this.group = CommonUtils.requireNonEmpty(group)
      this
    }
    @Optional("default name is a random string. But it is required in updating")
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
    def topicKey(topicKey: TopicKey): BasicRequest.this.type = topicKeys(Set(topicKey))

    @Optional(
      "You don't need to fill this field when update/create connector. But this filed is required in starting connector")
    def topicKeys(topicKeys: Set[TopicKey]): BasicRequest.this.type = {
      import scala.collection.JavaConverters._
      this.topicKeys = CommonUtils.requireNonEmpty(topicKeys.asJava).asScala.toSet
      this
    }

    @Optional("default value is 1")
    def numberOfTasks(numberOfTasks: Int): BasicRequest.this.type = {
      this.numberOfTasks = CommonUtils.requirePositiveInt(numberOfTasks)
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

    @Optional("default value is empty array in creation and None in update")
    def tags(tags: Map[String, JsValue]): BasicRequest.this.type = {
      this.tags = Objects.requireNonNull(tags)
      this
    }

    import spray.json._

    /**
      * generate the payload for request. It removes the ignored fields and keeping all value in json representation.
      * This method is exposed to sub classes since this generation is not friendly and hence we should reuse it as much as possible.
      * Noted, it throw unchecked exception if you haven't filled all required fields
      * @return creation object
      */
    private[v0] def creation: Creation = Creation(
      update.settings ++
        Map(SettingDefinition.CONNECTOR_NAME_DEFINITION.key() -> JsString(
          if (CommonUtils.isEmpty(name)) CommonUtils.randomString(10) else name)))

    private[v0] def update: Update = Update(
      settings.map {
        case (k, v) => k -> JsString(v)
      } ++ Map(
        SettingDefinition.CONNECTOR_CLASS_DEFINITION.key() -> (if (className == null) JsNull
                                                               else JsString(CommonUtils.requireNonEmpty(className))),
        SettingDefinition.COLUMNS_DEFINITION.key() -> (if (columns == null) JsNull
                                                       else if (columns.isEmpty) JsArray.empty
                                                       else
                                                         PropGroups.ofColumns(columns.asJava).toJsonString.parseJson),
        SettingDefinition.TOPIC_KEYS_DEFINITION.key() -> (if (topicKeys == null) JsNull
                                                          else if (topicKeys.isEmpty) JsArray.empty
                                                          else JsArray(topicKeys.map(TOPIC_KEY_FORMAT.write).toVector)),
        SettingDefinition.NUMBER_OF_TASKS_DEFINITION.key() -> JsNumber(CommonUtils.requirePositiveInt(numberOfTasks)),
        SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key() -> (if (workerClusterName == null) JsNull
                                                                   else
                                                                     JsString(
                                                                       CommonUtils.requireNonEmpty(workerClusterName))),
        SettingDefinition.TAGS_DEFINITION.key() -> (if (tags == null) JsNull else JsObject(tags))
      ).filter {
        case (_, value) =>
          value match {
            case JsNull => false
            case _      => true
          }
      })
  }

  /**
    * The do-action methods are moved from BasicRequest to this one. Hence, ValidationApi ConnectorRequest does not have those weired methods
    */
  sealed abstract class Request extends BasicRequest {

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
      extends com.island.ohara.client.configurator.v0.Access[ConnectorDescription](CONNECTORS_PREFIX_PATH) {

    /**
      * start to run a connector on worker cluster.
      *
      * @param key connector's key
      * @return the configuration of connector
      */
    def start(key: ConnectorKey)(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
      exec.put[ConnectorDescription, ErrorApi.Error](url(key, START_COMMAND))

    /**
      * stop and remove a running connector.
      *
      * @param key connector's key
      * @return the configuration of connector
      */
    def stop(key: ConnectorKey)(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
      exec.put[ConnectorDescription, ErrorApi.Error](url(key, STOP_COMMAND))

    /**
      * pause a running connector
      *
      * @param key connector's key
      * @return the configuration of connector
      */
    def pause(key: ConnectorKey)(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
      exec.put[ConnectorDescription, ErrorApi.Error](url(key, PAUSE_COMMAND))

    /**
      * resume a paused connector
      *
      * @param key connector's key
      * @return the configuration of connector
      */
    def resume(key: ConnectorKey)(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
      exec.put[ConnectorDescription, ErrorApi.Error](url(key, RESUME_COMMAND))

    def request: Request = new Request {
      override def create()(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
        exec.post[Creation, ConnectorDescription, ErrorApi.Error](url, creation)

      override def update()(implicit executionContext: ExecutionContext): Future[ConnectorDescription] =
        exec.put[Update, ConnectorDescription, ErrorApi.Error](url(ConnectorKey.of(group, name)), update)
    }
  }

  def access: Access = new Access
}
