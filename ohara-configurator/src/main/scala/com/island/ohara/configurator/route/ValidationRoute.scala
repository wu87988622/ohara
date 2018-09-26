package com.island.ohara.configurator.route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{Segment, as, complete, entity, onComplete, path, pathPrefix, put}
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.ConnectorClient
import com.island.ohara.configurator.endpoint.Validator
import com.island.ohara.kafka.KafkaClient
import spray.json.DefaultJsonProtocol._

import scala.util.{Failure, Success}

private[configurator] object ValidationRoute extends SprayJsonSupport {
  private[this] val DEFAULT_NUMBER_OF_VALIDATION = 3
  def apply(implicit kafkaClient: KafkaClient, connectClient: ConnectorClient): server.Route =
    pathPrefix(VALIDATION_PATH) {
      path(Segment) {
        case HDFS_VALIDATION_PATH =>
          put {
            entity(as[HdfsValidationRequest]) { req =>
              onComplete(Validator.run(connectClient, kafkaClient, req, DEFAULT_NUMBER_OF_VALIDATION)) {
                case Success(reports) =>
                  if (reports.isEmpty)
                    throw new IllegalStateException(s"No report!!! Failed to run the hdfs validation")
                  else complete(reports)
                case Failure(ex) => throw ex
              }
            }
          }
        case RDB_VALIDATION_PATH =>
          put {
            entity(as[RdbValidationRequest]) { req =>
              onComplete(Validator.run(connectClient, kafkaClient, req, DEFAULT_NUMBER_OF_VALIDATION)) {
                case Success(reports) =>
                  if (reports.isEmpty) throw new IllegalStateException(s"No report!!! Failed to run the rdb validation")
                  else complete(reports)
                case Failure(ex) => throw ex
              }
            }
          }
        case FTP_VALIDATION_PATH =>
          put {
            entity(as[FtpValidationRequest]) { req =>
              onComplete(Validator.run(connectClient, kafkaClient, req, DEFAULT_NUMBER_OF_VALIDATION)) {
                case Success(reports) =>
                  if (reports.isEmpty) throw new IllegalStateException(s"No report!!! Failed to run the ftp validation")
                  else complete(reports)
                case Failure(ex) => throw ex
              }
            }
          }
        case _ =>
          throw new IllegalArgumentException(
            s"Unsupported target on validation. accept: $RDB_VALIDATION_PATH and $HDFS_VALIDATION_PATH")
      }
    }
}
