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
import com.island.ohara.client.configurator.v0.ConnectorApi.Creation
import com.island.ohara.client.configurator.v0.QueryApi.RdbInfo
import com.island.ohara.kafka.connector.json._
import spray.json.DefaultJsonProtocol.{jsonFormat3, _}
import spray.json.{JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

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
  final case class HdfsValidationRequest(uri: String, workerClusterName: Option[String])
  implicit val HDFS_VALIDATION_REQUEST_JSON_FORMAT: RootJsonFormat[HdfsValidationRequest] = jsonFormat2(
    HdfsValidationRequest)

  val VALIDATION_RDB_PREFIX_PATH: String = "rdb"
  final case class RdbValidationRequest(url: String, user: String, password: String, workerClusterName: Option[String])
  implicit val RDB_VALIDATION_REQUEST_JSON_FORMAT: RootJsonFormat[RdbValidationRequest] = jsonFormat4(
    RdbValidationRequest)

  val VALIDATION_FTP_PREFIX_PATH: String = "ftp"
  final case class FtpValidationRequest(hostname: String,
                                        port: Int,
                                        user: String,
                                        password: String,
                                        workerClusterName: Option[String])
  implicit val FTP_VALIDATION_REQUEST_JSON_FORMAT: RootJsonFormat[FtpValidationRequest] =
    new RootJsonFormat[FtpValidationRequest] {
      private[this] val hostnameKey = "hostname"
      private[this] val userKey = "user"
      private[this] val passwordKey = "password"
      private[this] val portKey = "port"
      private[this] val workerClusterNameKey = "workerClusterName"
      override def read(json: JsValue): FtpValidationRequest = {
        val fields = json.asJsObject.fields.filter(_._2 match {
          case JsNull => false
          case _      => true
        })

        FtpValidationRequest(
          hostname = fields(hostnameKey).asInstanceOf[JsString].value,
          user = fields(userKey).asInstanceOf[JsString].value,
          password = fields(passwordKey).asInstanceOf[JsString].value,
          // we will convert a Map[String, String] to FtpValidationRequest in kafka connector so this method can save us from spray's ClassCastException
          port = fields(portKey) match {
            case s: JsString => s.value.toInt
            case s: JsNumber => s.value.toInt
            case _ =>
              throw new UnsupportedOperationException("failed to parse request for \"port\"")
          },
          workerClusterName = fields.get(workerClusterNameKey).map(_.asInstanceOf[JsString].value)
        )
      }

      override def write(obj: FtpValidationRequest): JsValue = JsObject(
        hostnameKey -> JsString(obj.hostname),
        portKey -> JsNumber(obj.port),
        userKey -> JsString(obj.user),
        passwordKey -> JsString(obj.password),
        workerClusterNameKey -> obj.workerClusterName.map(JsString(_)).getOrElse(JsNull)
      )
    }

  val VALIDATION_NODE_PREFIX_PATH: String = "node"
  final case class NodeValidationRequest(hostname: String, port: Int, user: String, password: String)
  implicit val NODE_VALIDATION_REQUEST_JSON_FORMAT: RootJsonFormat[NodeValidationRequest] = jsonFormat4(
    NodeValidationRequest)

  /**
    * the generic report for all validation.
    */
  trait ValidationReport {
    def hostname: String
    def message: String
    def pass: Boolean
  }

  object ValidationReport {
    def apply(hostname: String, message: String, pass: Boolean): ValidationReport = ValidationReportImpl(
      hostname = hostname,
      message = message,
      pass = pass
    )
  }

  final case class JdbcValidationReport(hostname: String, message: String, pass: Boolean, rdbInfo: RdbInfo)
      extends ValidationReport
  implicit val JDBC_VALIDATION_REPORT_JSON_FORMAT: RootJsonFormat[JdbcValidationReport] = jsonFormat4(
    JdbcValidationReport)

  private[this] def toCaseClass(report: ValidationReport): ValidationReportImpl = report match {
    case _: ValidationReportImpl => report.asInstanceOf[ValidationReportImpl]
    case _ =>
      ValidationReportImpl(
        hostname = report.hostname,
        message = report.message,
        pass = report.pass
      )
  }

  private[this] case class ValidationReportImpl(hostname: String, message: String, pass: Boolean)
      extends ValidationReport
  private[this] val VALIDATION_IMPL_REPORT_JSON_FORMAT: RootJsonFormat[ValidationReportImpl] = jsonFormat3(
    ValidationReportImpl)
  implicit val VALIDATION_REPORT_JSON_FORMAT: RootJsonFormat[ValidationReport] = new RootJsonFormat[ValidationReport] {
    override def write(obj: ValidationReport): JsValue = VALIDATION_IMPL_REPORT_JSON_FORMAT.write(toCaseClass(obj))
    override def read(json: JsValue): ValidationReport = VALIDATION_IMPL_REPORT_JSON_FORMAT.read(json)
  }

  val VALIDATION_CONNECTOR_PREFIX_PATH: String = "connector"

  implicit val SETTING_INFO_JSON_FORMAT: RootJsonFormat[SettingInfo] = new RootJsonFormat[SettingInfo] {
    import spray.json._
    override def write(obj: SettingInfo): JsValue = obj.toJsonString.parseJson

    override def read(json: JsValue): SettingInfo = SettingInfo.ofJson(json.toString())
  }

  /**
    * used to send the validation request to Configurator.
    */
  abstract class ConnectorRequest extends com.island.ohara.client.configurator.v0.ConnectorApi.BasicRequest {

    /**
      * used to verify the setting of connector on specific worker cluster
      * @return validation reports
      */
    def verify()(implicit executionContext: ExecutionContext): Future[SettingInfo]
  }

  sealed abstract class Access(prefix: String) extends BasicAccess(prefix) {

    def connectorRequest(): ConnectorRequest

    /**
      * used to verify the hdfs information on "default" worker cluster
      * @param request hdfs info
      * @return validation reports
      */
    def verify(request: HdfsValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]]

    /**
      * used to verify the rdb information on "default" worker cluster
      * @param request rdb info
      * @return validation reports
      */
    def verify(request: RdbValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[JdbcValidationReport]]

    /**
      * used to verify the ftp information on "default" worker cluster
      * @param request ftp info
      * @return validation reports
      */
    def verify(request: FtpValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]]

    /**
      * used to verify the node information on configurator
      * @param request node info
      * @return validation reports
      */
    def verify(request: NodeValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]]
  }

  def access(): Access = new Access(VALIDATION_PREFIX_PATH) {

    private[this] def url(prefix: String): String = s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$prefix"

    override def verify(request: HdfsValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
      exec.put[HdfsValidationRequest, Seq[ValidationReport], ErrorApi.Error](url(VALIDATION_HDFS_PREFIX_PATH), request)

    override def verify(request: RdbValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[JdbcValidationReport]] =
      exec
        .put[RdbValidationRequest, Seq[JdbcValidationReport], ErrorApi.Error](url(VALIDATION_RDB_PREFIX_PATH), request)

    override def verify(request: FtpValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
      exec.put[FtpValidationRequest, Seq[ValidationReport], ErrorApi.Error](url(VALIDATION_FTP_PREFIX_PATH), request)

    override def verify(request: NodeValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
      exec.put[NodeValidationRequest, Seq[ValidationReport], ErrorApi.Error](url(VALIDATION_NODE_PREFIX_PATH), request)

    override def connectorRequest(): ConnectorRequest = new ConnectorRequest {
      override def verify()(implicit executionContext: ExecutionContext): Future[SettingInfo] =
        exec.put[Creation, SettingInfo, ErrorApi.Error](url(VALIDATION_CONNECTOR_PREFIX_PATH), creation())
    }
  }
}
