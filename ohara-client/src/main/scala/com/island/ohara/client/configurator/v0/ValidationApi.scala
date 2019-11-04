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

import com.island.ohara.client.configurator.v0.InspectApi.RdbInfo
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.setting.{ObjectKey, TopicKey}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json.SettingInfo
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}
object ValidationApi {

  /**
    * this key points to the settings passed by user. Those settings are converted to a "single" string and then it is
    * submitted to kafka to run the Validator. Kafka doesn't support various type of json so we have to transfer data
    * via pure string.
    */
  val SETTINGS_KEY = "settings"
  val TARGET_KEY = "target"
  val VALIDATION_PREFIX_PATH: String = "validate"
  // TODO: We should use a temporary topic instead of fixed topic...by chia
  val INTERNAL_TOPIC_KEY: TopicKey = TopicKey.of(GROUP_DEFAULT, "_Validator_topic")

  /**
    * add this to setting and then the key pushed to topic will be same with the value
    */
  val REQUEST_ID = "requestId"

  val VALIDATION_HDFS_PREFIX_PATH: String = "hdfs"
  final case class HdfsValidation private[ValidationApi] (uri: String, workerClusterKey: ObjectKey)
  implicit val HDFS_VALIDATION_JSON_FORMAT: OharaJsonFormat[HdfsValidation] =
    JsonRefiner[HdfsValidation].format(jsonFormat2(HdfsValidation)).rejectEmptyString().refine

  val VALIDATION_RDB_PREFIX_PATH: String = "rdb"

  /**
    * the QueryRoute needs to create this object to get tables from validation connector.
    */
  final case class RdbValidation private[ohara] (url: String,
                                                 user: String,
                                                 password: String,
                                                 workerClusterKey: ObjectKey)
  implicit val RDB_VALIDATION_JSON_FORMAT: OharaJsonFormat[RdbValidation] =
    JsonRefiner[RdbValidation].format(jsonFormat4(RdbValidation)).rejectEmptyString().refine

  val VALIDATION_FTP_PREFIX_PATH: String = "ftp"
  final case class FtpValidation private[ValidationApi] (hostname: String,
                                                         port: Int,
                                                         user: String,
                                                         password: String,
                                                         workerClusterKey: ObjectKey)

  implicit val FTP_VALIDATION_JSON_FORMAT: OharaJsonFormat[FtpValidation] = JsonRefiner[FtpValidation]
    .format(jsonFormat5(FtpValidation))
    .rejectEmptyString()
    .requireConnectionPort("port")
    // this marshalling must be able to parse string for number since ValidationUtils use Map[String, String] to carry
    // this validation...I do hate this workaround but kafka supports only the Map[String, String]... by chia
    .acceptStringToNumber("port")
    .refine

  val VALIDATION_NODE_PREFIX_PATH: String = "node"
  final case class NodeValidation private[ValidationApi] (hostname: String,
                                                          port: Option[Int],
                                                          user: Option[String],
                                                          password: Option[String])
  implicit val NODE_VALIDATION_JSON_FORMAT: OharaJsonFormat[NodeValidation] = JsonRefiner[NodeValidation]
    .format(jsonFormat4(NodeValidation))
    .rejectEmptyString()
    .requireConnectionPort("port")
    .refine

  final case class ValidationReport(hostname: String, message: String, pass: Boolean, lastModified: Long)
  implicit val VALIDATION_REPORT_JSON_FORMAT: RootJsonFormat[ValidationReport] = jsonFormat4(ValidationReport)

  final case class RdbValidationReport(hostname: String, message: String, pass: Boolean, rdbInfo: Option[RdbInfo])
  implicit val RDB_VALIDATION_REPORT_JSON_FORMAT: RootJsonFormat[RdbValidationReport] = jsonFormat4(RdbValidationReport)

  val VALIDATION_CONNECTOR_PREFIX_PATH: String = "connector"

  private[this] implicit val SETTING_INFO_JSON_FORMAT: RootJsonFormat[SettingInfo] = new RootJsonFormat[SettingInfo] {
    import spray.json._
    override def write(obj: SettingInfo): JsValue = obj.toJsonString.parseJson

    override def read(json: JsValue): SettingInfo = SettingInfo.ofJson(json.toString())
  }

  /**
    * used to send the request of validating connector to Configurator.
    */
  abstract class ConnectorRequest extends com.island.ohara.client.configurator.v0.ConnectorApi.BasicRequest {

    /**
      * used to verify the setting of connector on specific worker cluster
      * @return validation reports
      */
    def verify()(implicit executionContext: ExecutionContext): Future[SettingInfo]
  }

  sealed abstract class BasicNodeRequest {
    protected var port: Option[Int] = None
    protected var hostname: String = _
    protected var user: String = _
    protected var password: String = _

    def port(port: Int): BasicNodeRequest.this.type = {
      this.port = Some(CommonUtils.requireConnectionPort(port))
      this
    }

    def hostname(hostname: String): BasicNodeRequest.this.type = {
      this.hostname = CommonUtils.requireNonEmpty(hostname)
      this
    }

    def user(user: String): BasicNodeRequest.this.type = {
      this.user = CommonUtils.requireNonEmpty(user)
      this
    }

    def password(password: String): BasicNodeRequest.this.type = {
      this.password = CommonUtils.requireNonEmpty(password)
      this
    }
  }

  /**
    * used to send the request of validating ftp to Configurator.
    */
  sealed abstract class FtpRequest extends BasicNodeRequest {
    protected var workerClusterKey: ObjectKey = _

    @Optional("server will try to pick a worker cluster for you if this field is ignored")
    def workerClusterKey(workerClusterKey: ObjectKey): FtpRequest = {
      this.workerClusterKey = Objects.requireNonNull(workerClusterKey)
      this
    }

    /**
      * Retrieve the inner object of request payload. Noted, it throw unchecked exception if you haven't filled all required fields
      * @return the payload of creation
      */
    @VisibleForTesting
    private[v0] def validation: FtpValidation

    /**
      * used to verify the setting of ftp on specific worker cluster
      * @return validation reports
      */
    def verify()(implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]]
  }

  sealed trait HdfsRequest {

    def uri(uri: String): HdfsRequest

    @Optional("server will try to pick a worker cluster for you if this field is ignored")
    def workerClusterKey(workerClusterKey: ObjectKey): HdfsRequest

    /**
      * Retrieve the inner object of request payload. Noted, it throw unchecked exception if you haven't filled all required fields
      * @return the payload of creation
      */
    @VisibleForTesting
    private[v0] def validation: HdfsValidation

    /**
      * used to verify the setting of hdfs on specific worker cluster
      * @return validation reports
      */
    def verify()(implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]]
  }

  sealed trait RdbRequest {
    def jdbcUrl(url: String): RdbRequest
    def user(user: String): RdbRequest
    def password(password: String): RdbRequest
    @Optional("server will try to pick a worker cluster for you if this field is ignored")
    def workerClusterKey(workerClusterKey: ObjectKey): RdbRequest

    /**
      * Retrieve the inner object of request payload. Noted, it throw unchecked exception if you haven't filled all required fields
      * @return the payload of creation
      */
    @VisibleForTesting
    private[v0] def validation: RdbValidation

    /**
      * used to verify the setting of rdb on specific worker cluster
      * @return validation reports
      */
    def verify()(implicit executionContext: ExecutionContext): Future[Seq[RdbValidationReport]]
  }

  sealed abstract class NodeRequest extends BasicNodeRequest {

    /**
      * Retrieve the inner object of request payload. Noted, it throw unchecked exception if you haven't filled all required fields
      * @return the payload of creation
      */
    @VisibleForTesting
    private[v0] def validation: NodeValidation

    /**
      * used to verify the setting of rdb on specific worker cluster
      * @return validation reports
      */
    def verify()(implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]]
  }

  sealed abstract class Access(prefix: String) extends BasicAccess(prefix) {

    /**
      * start a progress to build a request to validate connector
      * @return request of validating connector
      */
    def connectorRequest: ConnectorRequest

    /**
      * start a progress to build a request to validate ftp
      * @return request of validating ftp
      */
    def ftpRequest: FtpRequest

    /**
      * start a progress to build a request to validate HDFS
      * @return request of validating HDFS
      */
    def hdfsRequest: HdfsRequest

    def rdbRequest: RdbRequest

    def nodeRequest: NodeRequest
  }

  def access: Access = new Access(VALIDATION_PREFIX_PATH) {

    override def hdfsRequest: HdfsRequest = new HdfsRequest {
      private[this] var uri: String = _
      private[this] var workerClusterKey: ObjectKey = _

      override def uri(uri: String): HdfsRequest = {
        this.uri = CommonUtils.requireNonEmpty(uri)
        this
      }

      override def workerClusterKey(workerClusterKey: ObjectKey): HdfsRequest = {
        this.workerClusterKey = Objects.requireNonNull(workerClusterKey)
        this
      }

      override private[v0] def validation: HdfsValidation = HdfsValidation(
        uri = CommonUtils.requireNonEmpty(uri),
        workerClusterKey = Objects.requireNonNull(workerClusterKey)
      )

      override def verify()(implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
        exec
          .put[HdfsValidation, Seq[ValidationReport], ErrorApi.Error](s"$url/$VALIDATION_HDFS_PREFIX_PATH", validation)

    }

    override def rdbRequest: RdbRequest = new RdbRequest {
      private[this] var jdbcUrl: String = _
      private[this] var user: String = _
      private[this] var password: String = _
      private[this] var workerClusterKey: ObjectKey = _

      override def jdbcUrl(jdbcUrl: String): RdbRequest = {
        this.jdbcUrl = CommonUtils.requireNonEmpty(jdbcUrl)
        this
      }

      override def user(user: String): RdbRequest = {
        this.user = CommonUtils.requireNonEmpty(user)
        this
      }

      override def password(password: String): RdbRequest = {
        this.password = CommonUtils.requireNonEmpty(password)
        this
      }

      override def workerClusterKey(workerClusterKey: ObjectKey): RdbRequest = {
        this.workerClusterKey = Objects.requireNonNull(workerClusterKey)
        this
      }

      override private[v0] def validation: RdbValidation = RdbValidation(
        url = CommonUtils.requireNonEmpty(jdbcUrl),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password),
        workerClusterKey = Objects.requireNonNull(workerClusterKey)
      )

      override def verify()(implicit executionContext: ExecutionContext): Future[Seq[RdbValidationReport]] =
        exec
          .put[RdbValidation, Seq[RdbValidationReport], ErrorApi.Error](s"$url/$VALIDATION_RDB_PREFIX_PATH", validation)
    }

    override def nodeRequest: NodeRequest = new NodeRequest {
      override private[v0] def validation = NodeValidation(
        hostname = CommonUtils.requireNonEmpty(hostname),
        port = port.map(CommonUtils.requireConnectionPort),
        user = Option(user).map(CommonUtils.requireNonEmpty),
        password = Option(password).map(CommonUtils.requireNonEmpty),
      )

      override def verify()(implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
        exec
          .put[NodeValidation, Seq[ValidationReport], ErrorApi.Error](s"$url/$VALIDATION_NODE_PREFIX_PATH", validation)
    }

    override def connectorRequest: ConnectorRequest = new ConnectorRequest {
      override def verify()(implicit executionContext: ExecutionContext): Future[SettingInfo] =
        exec.put[com.island.ohara.client.configurator.v0.ConnectorApi.Creation, SettingInfo, ErrorApi.Error](
          s"$url/$VALIDATION_CONNECTOR_PREFIX_PATH",
          creation)
    }

    override def ftpRequest: FtpRequest = new FtpRequest {
      override private[v0] def validation: FtpValidation = FtpValidation(
        hostname = CommonUtils.requireNonEmpty(hostname),
        port = port.map(CommonUtils.requireConnectionPort).getOrElse(throw new NullPointerException),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password),
        workerClusterKey = Objects.requireNonNull(workerClusterKey)
      )

      override def verify()(implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
        exec.put[FtpValidation, Seq[ValidationReport], ErrorApi.Error](s"$url/$VALIDATION_FTP_PREFIX_PATH", validation)
    }
  }
}
