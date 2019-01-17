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

package com.island.ohara.configurator.route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{as, complete, entity, onSuccess, path, pathPrefix, put, _}
import com.island.ohara.agent.Agent
import com.island.ohara.client.ConnectorClient
import com.island.ohara.client.configurator.v0.ValidationApi._
import com.island.ohara.configurator.endpoint.Validator
import com.island.ohara.kafka.KafkaClient
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

private[configurator] object ValidationRoute extends SprayJsonSupport {
  private[this] def verifyRoute[Req](root: String, verify: Req => Future[Seq[ValidationReport]])(
    implicit rm: RootJsonFormat[Req]): server.Route = path(root) {
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
      ) ~ verifyRoute(
        root = VALIDATION_NODE_PREFIX_PATH,
        verify = (req: NodeValidationRequest) =>
          Future {
            val cmd = "ls /tmp"
            val message = s"test $cmd on ${req.hostname}:${req.port}"
            Seq[ValidationReport](
              try {
                val agent =
                  Agent.builder().hostname(req.hostname).port(req.port).user(req.user).password(req.password).build()
                try agent.execute(cmd)
                finally agent.close()
                ValidationReport(
                  hostname = req.hostname,
                  message = message,
                  pass = true
                )
              } catch {
                case e: Throwable =>
                  ValidationReport(
                    hostname = req.hostname,
                    message = s"$message (failed by ${e.getMessage})",
                    pass = false
                  )
              }
            )
        }
      )
    }
}
