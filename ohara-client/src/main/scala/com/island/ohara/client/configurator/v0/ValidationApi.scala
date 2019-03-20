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
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorCreationRequest
import com.island.ohara.kafka.connector.json._
import spray.json.DefaultJsonProtocol.{jsonFormat3, _}
import spray.json.{JsNull, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}
object ValidationApi {
  val VALIDATION_PREFIX_PATH: String = "validate"
  val VALIDATION_HDFS_PREFIX_PATH: String = "hdfs"
  val WORKER_CLUSTER_NAME_KEY: String = ConnectorFormatter.WORKER_CLUSTER_NAME_KEY
  val CLASS_NAME_KEY: String = ConnectorFormatter.CLASS_NAME_KEY
  val TOPIC_NAMES_KEY: String = ConnectorFormatter.TOPIC_NAMES_KEY
  val NUMBER_OF_TASKS_KEY: String = ConnectorFormatter.NUMBER_OF_TASKS_KEY
  val COLUMNS_KEY: String = ConnectorFormatter.COLUMNS_KEY
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
      override def read(json: JsValue): FtpValidationRequest = {
        val (hostname, user, password) = json.asJsObject.getFields("hostname", "user", "password") match {
          case Seq(JsString(hostname), JsString(user), JsString(password)) => (hostname, user, password)
          case _ =>
            throw new UnsupportedOperationException("failed to parse request for \"hostname\", \"user\", \"password\"")
        }
        // we will convert a Map[String, String] to FtpValidationRequest in kafka connector so this method can save us from spray's ClassCastException
        val port: Int = json.asJsObject.getFields("port") match {
          case Seq(JsString(port)) => port.toInt
          case Seq(JsNumber(port)) => port.toInt
          case _ =>
            throw new UnsupportedOperationException("failed to parse request for \"port\"")
        }

        val workerClusterName: Option[String] = json.asJsObject.getFields("workerClusterName") match {
          case Seq(JsString(workerClusterName)) => Some(workerClusterName)
          case _                                => None
        }
        FtpValidationRequest(hostname = hostname,
                             user = user,
                             password = password,
                             port = port,
                             workerClusterName = workerClusterName)
      }

      override def write(obj: FtpValidationRequest): JsValue = JsObject(
        "hostname" -> JsString(obj.hostname),
        "port" -> JsNumber(obj.port),
        "user" -> JsString(obj.user),
        "password" -> JsString(obj.password),
        "workerClusterName" -> obj.workerClusterName.map(JsString(_)).getOrElse(JsNull)
      )
    }

  val VALIDATION_NODE_PREFIX_PATH: String = "node"
  final case class NodeValidationRequest(hostname: String, port: Int, user: String, password: String)
  implicit val NODE_VALIDATION_REQUEST_JSON_FORMAT: RootJsonFormat[NodeValidationRequest] = jsonFormat4(
    NodeValidationRequest)

  final case class ValidationReport(hostname: String, message: String, pass: Boolean)
  implicit val VALIDATION_REPORT_JSON_FORMAT: RootJsonFormat[ValidationReport] = jsonFormat3(ValidationReport)

  val VALIDATION_CONNECTOR_PREFIX_PATH: String = "connector"

  implicit val SETTING_INFO_JSON_FORMAT: RootJsonFormat[SettingInfo] = new RootJsonFormat[SettingInfo] {
    import spray.json._
    override def write(obj: SettingInfo): JsValue = obj.toJsonString.parseJson

    override def read(json: JsValue): SettingInfo = SettingInfo.ofJson(json.toString())
  }

  sealed abstract class Access(prefix: String) extends BasicAccess(prefix) {

    /**
      * used to verify the setting of connector on specific worker cluster
      * @param request hdfs info
      * @return validation reports
      */
    def verify(request: ConnectorCreationRequest)(implicit executionContext: ExecutionContext): Future[SettingInfo]

    /**
      * used to verify the hdfs information on "default" worker cluster
      * @param request hdfs info
      * @return validation reports
      */
    def verify(request: HdfsValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]]

    /**
      * used to verify the hdfs information on specified worker cluster
      * @param request hdfs info
      * @param target worker cluster used to verify the request
      * @return validation reports
      */
    def verify(request: HdfsValidationRequest, target: String)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]]

    /**
      * used to verify the rdb information on "default" worker cluster
      * @param request rdb info
      * @return validation reports
      */
    def verify(request: RdbValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]]

    /**
      * used to verify the rdb information on specified worker cluster
      * @param request rdb info
      * @param target worker cluster used to verify the request
      * @return validation reports
      */
    def verify(request: RdbValidationRequest, target: String)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]]

    /**
      * used to verify the ftp information on "default" worker cluster
      * @param request ftp info
      * @return validation reports
      */
    def verify(request: FtpValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]]

    /**
      * used to verify the ftp information on specified worker cluster
      * @param request ftp info
      * @param target worker cluster used to verify the request
      * @return validation reports
      */
    def verify(request: FtpValidationRequest, target: String)(
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

    private[this] def url(prefix: String, target: String): String = {
      val url = s"http://${_hostname}:${_port}/${_version}/${_prefixPath}/$prefix"
      if (target == null) url
      else Parameters.appendTargetCluster(url, target)
    }

    override def verify(request: HdfsValidationRequest, target: String)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
      exec.put[HdfsValidationRequest, Seq[ValidationReport], ErrorApi.Error](url(VALIDATION_HDFS_PREFIX_PATH, target),
                                                                             request)

    override def verify(request: RdbValidationRequest, target: String)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
      exec.put[RdbValidationRequest, Seq[ValidationReport], ErrorApi.Error](url(VALIDATION_RDB_PREFIX_PATH, target),
                                                                            request)

    override def verify(request: FtpValidationRequest, target: String)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
      exec.put[FtpValidationRequest, Seq[ValidationReport], ErrorApi.Error](url(VALIDATION_FTP_PREFIX_PATH, target),
                                                                            request)

    override def verify(request: NodeValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] =
      exec.put[NodeValidationRequest, Seq[ValidationReport], ErrorApi.Error](url(VALIDATION_NODE_PREFIX_PATH, null),
                                                                             request)

    override def verify(request: HdfsValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] = verify(request, null)

    override def verify(request: RdbValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] = verify(request, null)

    override def verify(request: FtpValidationRequest)(
      implicit executionContext: ExecutionContext): Future[Seq[ValidationReport]] = verify(request, null)

    override def verify(request: ConnectorCreationRequest)(
      implicit executionContext: ExecutionContext): Future[SettingInfo] =
      exec.put[ConnectorCreationRequest, SettingInfo, ErrorApi.Error](url(VALIDATION_CONNECTOR_PREFIX_PATH, null),
                                                                      request)
  }
}
