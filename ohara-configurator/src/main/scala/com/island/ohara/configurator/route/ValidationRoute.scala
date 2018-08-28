package com.island.ohara.configurator.route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{Segment, as, complete, entity, path, pathPrefix, put}
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.ConnectorClient
import com.island.ohara.configurator.endpoint.Validator
import com.island.ohara.kafka.KafkaClient

private[configurator] object ValidationRoute extends SprayJsonSupport {
  private[this] val DEFAULT_NUMBER_OF_VALIDATION = 3
  def apply(implicit kafkaClient: KafkaClient, connectClient: ConnectorClient): server.Route =
    pathPrefix(VALIDATION_PATH) {
      path(Segment) {
        case HDFS_VALIDATION_PATH =>
          put {
            entity(as[HdfsValidationRequest]) { req =>
              {
                val reports = Validator.run(connectClient, kafkaClient, req, DEFAULT_NUMBER_OF_VALIDATION)
                if (reports.isEmpty) throw new IllegalStateException(s"No report!!! Failed to run the validation")
                complete(reports)
              }
            }
          }
        case RDB_VALIDATION_PATH =>
          put {
            entity(as[RdbValidationRequest]) { req =>
              {
                val reports = Validator.run(connectClient, kafkaClient, req, DEFAULT_NUMBER_OF_VALIDATION)
                if (reports.isEmpty) throw new IllegalStateException(s"No report!!! Failed to run the validation")
                complete(reports)
              }
            }
          }
        case _ =>
          throw new IllegalArgumentException(
            s"Unsupported target on validation. accept: $RDB_VALIDATION_PATH and $HDFS_VALIDATION_PATH")
      }
    }
}
