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
import com.island.ohara.client.configurator.v0.QueryApi.RdbInfo
import com.island.ohara.common.annotations.{Optional, VisibleForTesting}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.kafka.connector.json._
import spray.json.DefaultJsonProtocol.{jsonFormat3, _}
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}
object ValidationApi {
  val TARGET = "target"
  val VALIDATION_PREFIX_PATH: String = "validate"
  // TODO: We should use a temporary topic instead of fixed topic...by chia
  val INTERNAL_TOPIC = "_Validator_topic"

  /**
    * add this to setting and then the key pushed to topic will be same with the value
    */
  val REQUEST_ID = "requestId"

  val VALIDATION_HDFS_PREFIX_PATH: String = "hdfs"
  final case class HdfsValidation private[ValidationApi] (uri: String, workerClusterName: Option[String])
  implicit val HDFS_VALIDATION_JSON_FORMAT: OharaJsonFormat[HdfsValidation] =
    JsonRefiner[HdfsValidation].format(jsonFormat2(HdfsValidation)).rejectEmptyString().refine

  val VALIDATION_RDB_PREFIX_PATH: String = "rdb"

  /**
    * the QueryRoute needs to create this object to get tables from validation connector.
    */
  final case class RdbValidation private[ohara] (url: String,
                                                 user: String,
                                                 password: String,
                                                 workerClusterName: Option[String])
  implicit val RDB_VALIDATION_JSON_FORMAT: OharaJsonFormat[RdbValidation] =
    JsonRefiner[RdbValidation].format(jsonFormat4(RdbValidation)).rejectEmptyString().refine

  val VALIDATION_FTP_PREFIX_PATH: String = "ftp"
  final case class FtpValidation private[ValidationApi] (hostname: String,
                                                         port: Int,
                                                         user: String,
                                                         password: String,
                                                         workerClusterName: Option[String])

  implicit val FTP_VALIDATION_JSON_FORMAT: OharaJsonFormat[FtpValidation] = JsonRefiner[FtpValidation]
    .format(jsonFormat5(FtpValidation))
    .rejectEmptyString()
    .requireConnectionPort("port")
    // this marshalling must be able to parse string for number since ValidationUtils use Map[String, String] to carry
    // this validation...I do hate this workaround but kafka supports only the Map[String, String]... by chia
    .acceptStringToNumber("port")
    .refine

  val VALIDATION_NODE_PREFIX_PATH: String = "node"
  final case class NodeValidation private[ValidationApi] (hostname: String, port: Int, user: String, password: String)
  implicit val NODE_VALIDATION_JSON_FORMAT: OharaJsonFormat[NodeValidation] = JsonRefiner[NodeValidation]
    .format(jsonFormat4(NodeValidation))
    .rejectEmptyString()
    .requireConnectionPort("port")
    .refine

  final case class ValidationReport(hostname: String, message: String, pass: Boolean)
  implicit val VALIDATION_REPORT_JSON_FORMAT: RootJsonFormat[ValidationReport] = jsonFormat3(ValidationReport)

  final case class RdbValidationReport(hostname: String, message: String, pass: Boolean, rdbInfo: RdbInfo)
  implicit val RDB_VALIDATION_REPORT_JSON_FORMAT: RootJsonFormat[RdbValidationReport] = jsonFormat4(RdbValidationReport)

  val VALIDATION_CONNECTOR_PREFIX_PATH: String = "connector"

  implicit val SETTING_INFO_JSON_FORMAT: RootJsonFormat[SettingInfo] = new RootJsonFormat[SettingInfo] {
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
    protected var workerClusterName: String = _

    @Optional("server will try to pick a worker cluster for you if this field is ignored")
    def workerClusterName(workerClusterName: String): FtpRequest = {
      this.workerClusterName = CommonUtils.requireNonEmpty(workerClusterName)
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
    def workerClusterName(workerClusterName: String): HdfsRequest

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
    def url(url: String): RdbRequest
    def user(user: String): RdbRequest
    def password(password: String): RdbRequest
    @Optional("server will try to pick a worker cluster for you if this field is ignored")
    def workerClusterName(workerClusterName: String): RdbRequest

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

    private[this] def _url(prefix: String): String = s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$prefix"

    override def hdfsRequest: HdfsRequest = new HdfsRequest {
      private[this] var uri: String = _
      private[this] var workerClusterName: String = _

      override def uri(uri: String): HdfsRequest = {
        this.uri = CommonUtils.requireNonEmpty(uri)
        this
      }

      override def workerClusterName(workerClusterName: String): HdfsRequest = {
        this.workerClusterName = CommonUtils.requireNonEmpty(workerClusterName)
        this
      }

      override private[v0] def validation: HdfsValidation = HdfsValidation(
        uri = CommonUtils.requireNonEmpty(uri),
        workerClusterName = Option(workerClusterName).map(CommonUtils.requireNonEmpty)
      )

      override def verify()(implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
        exec.put[HdfsValidation, Seq[ValidationReport], ErrorApi.Error](_url(VALIDATION_HDFS_PREFIX_PATH), validation)

    }

    override def rdbRequest: RdbRequest = new RdbRequest {
      private[this] var url: String = _
      private[this] var user: String = _
      private[this] var password: String = _
      private[this] var workerClusterName: String = _

      override def url(url: String): RdbRequest = {
        this.url = CommonUtils.requireNonEmpty(url)
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

      override def workerClusterName(workerClusterName: String): RdbRequest = {
        this.workerClusterName = CommonUtils.requireNonEmpty(workerClusterName)
        this
      }

      override private[v0] def validation: RdbValidation = RdbValidation(
        url = CommonUtils.requireNonEmpty(url),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password),
        workerClusterName = Option(workerClusterName).map(CommonUtils.requireNonEmpty)
      )

      //      override def verify()(implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
      //        exec.put[RdbValidation, Seq[ValidationReport], ErrorApi.Error](url(VALIDATION_RDB_PREFIX_PATH), validation)
      override def verify()(implicit executionContext: ExecutionContext): Future[Seq[RdbValidationReport]] =
        exec.put[RdbValidation, Seq[RdbValidationReport], ErrorApi.Error](_url(VALIDATION_RDB_PREFIX_PATH), validation)
    }

    override def nodeRequest: NodeRequest = new NodeRequest {
      override private[v0] def validation = NodeValidation(
        hostname = CommonUtils.requireNonEmpty(hostname),
        port = port.map(CommonUtils.requireConnectionPort).getOrElse(throw new NullPointerException),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password)
      )

      override def verify()(implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
        exec.put[NodeValidation, Seq[ValidationReport], ErrorApi.Error](_url(VALIDATION_NODE_PREFIX_PATH), validation)
    }

    override def connectorRequest: ConnectorRequest = new ConnectorRequest {
      override def verify()(implicit executionContext: ExecutionContext): Future[SettingInfo] =
        exec.put[com.island.ohara.client.configurator.v0.ConnectorApi.Creation, SettingInfo, ErrorApi.Error](
          _url(VALIDATION_CONNECTOR_PREFIX_PATH),
          creation)
    }

    override def ftpRequest: FtpRequest = new FtpRequest {
      override private[v0] def validation: FtpValidation = FtpValidation(
        hostname = CommonUtils.requireNonEmpty(hostname),
        port = port.map(CommonUtils.requireConnectionPort).getOrElse(throw new NullPointerException),
        user = CommonUtils.requireNonEmpty(user),
        password = CommonUtils.requireNonEmpty(password),
        workerClusterName = Option(workerClusterName).map(CommonUtils.requireNonEmpty)
      )

      override def verify()(implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
        exec.put[FtpValidation, Seq[ValidationReport], ErrorApi.Error](_url(VALIDATION_FTP_PREFIX_PATH), validation)
    }
  }
}
