package com.island.ohara.configurator.route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{as, complete, entity, onSuccess, path, pathPrefix, put, _}
import com.island.ohara.client.ConnectorClient
import com.island.ohara.client.configurator.v0.ValidationApi._
import com.island.ohara.configurator.endpoint.Validator
import com.island.ohara.kafka.KafkaClient
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future

private[configurator] object ValidationRoute extends SprayJsonSupport {
  private[this] def verifyRoute[Req](root: String, verify: Req => Future[Seq[ValidationReport]])(
    implicit kafkaClient: KafkaClient,
    connectClient: ConnectorClient,
    rm: RootJsonFormat[Req]): server.Route = path(root) {
    put {
      entity(as[Req])(req =>
        onSuccess(verify(req))(reports =>
          if (reports.isEmpty) failWith(new IllegalStateException(s"No report!!! Failed to run verification on $root"))
          else complete(reports)))
    }
  }
  private[this] val DEFAULT_NUMBER_OF_VALIDATION = 3

  def apply(implicit kafkaClient: KafkaClient, connectClient: ConnectorClient): server.Route =
    pathPrefix(VALIDATION_PREFIX_PATH) {
      verifyRoute(
        root = VALIDATION_HDFS_PREFIX_PATH,
        verify =
          (req: HdfsValidationRequest) => Validator.run(connectClient, kafkaClient, req, DEFAULT_NUMBER_OF_VALIDATION)
      ) ~ verifyRoute(
        root = VALIDATION_RDB_PREFIX_PATH,
        verify =
          (req: RdbValidationRequest) => Validator.run(connectClient, kafkaClient, req, DEFAULT_NUMBER_OF_VALIDATION)
      ) ~ verifyRoute(
        root = VALIDATION_FTP_PREFIX_PATH,
        verify =
          (req: FtpValidationRequest) => Validator.run(connectClient, kafkaClient, req, DEFAULT_NUMBER_OF_VALIDATION)
      )
    }
}
