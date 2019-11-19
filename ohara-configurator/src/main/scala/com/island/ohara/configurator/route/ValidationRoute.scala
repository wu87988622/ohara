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
import akka.http.scaladsl.model.{ContentTypes, _}
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, pathPrefix, put, _}
import com.island.ohara.agent.{BrokerCollie, ServiceCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.ConnectorApi.Creation
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.InspectApi.{RdbColumn, RdbInfo, RdbTable}
import com.island.ohara.client.configurator.v0.ValidationApi._
import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.fake.FakeWorkerClient
import com.island.ohara.configurator.store.DataStore
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private[configurator] object ValidationRoute extends SprayJsonSupport {
  private[this] val DEFAULT_NUMBER_OF_VALIDATION = 3

  private[this] def verifyRoute[Req, Report](
    root: String,
    verify: Req => Future[Seq[Report]]
  )(implicit rm: RootJsonFormat[Req], rm2: RootJsonFormat[Report], executionContext: ExecutionContext): server.Route =
    path(root) {
      put {
        entity(as[Req])(
          req =>
            complete(verify(req).map { reports =>
              if (reports.isEmpty) throw new IllegalStateException(s"No report!!! Failed to run verification on $root")
              else reports
            })
        )
      }
    }

  @VisibleForTesting
  private[route] def fakeReport(): Future[Seq[ValidationReport]] =
    Future.successful(
      (0 until DEFAULT_NUMBER_OF_VALIDATION).map(
        _ =>
          ValidationReport(
            hostname = CommonUtils.hostname,
            message = "a fake report",
            pass = true,
            lastModified = CommonUtils.current()
          )
      )
    )

  @VisibleForTesting
  private[route] def fakeJdbcReport(): Future[Seq[RdbValidationReport]] =
    Future.successful(
      (0 until DEFAULT_NUMBER_OF_VALIDATION).map(
        _ =>
          RdbValidationReport(
            hostname = CommonUtils.hostname,
            message = "a fake report",
            pass = true,
            rdbInfo = Some(
              RdbInfo(
                name = "fake database",
                tables = Seq(
                  RdbTable(
                    catalogPattern = None,
                    schemaPattern = None,
                    name = "fake table",
                    columns = Seq(
                      RdbColumn(
                        name = "fake column",
                        dataType = "fake type",
                        pk = true
                      )
                    )
                  )
                )
              )
            )
          )
      )
    )

  def apply(
    implicit brokerCollie: BrokerCollie,
    dataStore: DataStore,
    adminCleaner: AdminCleaner,
    workerCollie: WorkerCollie,
    serviceCollie: ServiceCollie,
    executionContext: ExecutionContext
  ): server.Route =
    pathPrefix(VALIDATION_PREFIX_PATH) {
      verifyRoute(
        root = VALIDATION_HDFS_PREFIX_PATH,
        verify = (req: HdfsValidation) => {
          both(req.workerClusterKey).flatMap {
            case (_, topicAdmin, _, workerClient) =>
              workerClient match {
                case _: FakeWorkerClient => fakeReport()
                case _                   => ValidationUtils.run(workerClient, topicAdmin, req, DEFAULT_NUMBER_OF_VALIDATION)
              }
          }
        }
      ) ~ verifyRoute(
        root = VALIDATION_RDB_PREFIX_PATH,
        verify = (req: RdbValidation) =>
          both(req.workerClusterKey).flatMap {
            case (_, topicAdmin, _, workerClient) =>
              workerClient match {
                case _: FakeWorkerClient => fakeJdbcReport()
                case _ =>
                  ValidationUtils.run(workerClient, topicAdmin, req, DEFAULT_NUMBER_OF_VALIDATION)
              }
          }
      ) ~ verifyRoute(
        root = VALIDATION_FTP_PREFIX_PATH,
        verify = (req: FtpValidation) =>
          both(req.workerClusterKey).flatMap {
            case (_, topicAdmin, _, workerClient) =>
              workerClient match {
                case _: FakeWorkerClient => fakeReport()
                case _                   => ValidationUtils.run(workerClient, topicAdmin, req, DEFAULT_NUMBER_OF_VALIDATION)
              }
          }
      ) ~ verifyRoute(
        root = VALIDATION_NODE_PREFIX_PATH,
        verify = (req: NodeValidation) =>
          serviceCollie
            .verifyNode(
              Node(
                hostname = req.hostname,
                port = req.port,
                user = req.user,
                password = req.password,
                services = Seq.empty,
                lastModified = CommonUtils.current(),
                validationReport = None,
                resources = Seq.empty,
                tags = Map.empty
              )
            )
            .map {
              case Success(value) =>
                ValidationReport(
                  hostname = req.hostname,
                  message = value,
                  pass = true,
                  lastModified = CommonUtils.current()
                )
              case Failure(exception) =>
                ValidationReport(
                  hostname = req.hostname,
                  message = exception.getMessage,
                  pass = false,
                  lastModified = CommonUtils.current()
                )
            }
            .flatMap { report =>
              dataStore.get[Node](NodeApi.key(req.hostname)).flatMap { nodeOption =>
                nodeOption
                  .filter { node =>
                    node.hostname == req.hostname &&
                    node.port == req.port &&
                    node.user == req.user &&
                    node.password == req.password
                  }
                  .map(_.copy(validationReport = Some(report)))
                  .map(node => dataStore.add[Node](node).map(_ => report))
                  .getOrElse(Future.successful(report))
              }
            }
            .map(Seq(_))
      ) ~ path(VALIDATION_CONNECTOR_PREFIX_PATH) {
        put {
          entity(as[Creation])(
            req =>
              complete(
                workerClient(req.workerClusterKey)
                  .flatMap {
                    case (cluster, workerClient) =>
                      workerClient
                        .connectorValidator()
                        .settings(req.plain)
                        .className(req.className)
                        // the topic name is composed by group and name. However, the kafka topic is still a pure string.
                        // Hence, we can't just push Ohara topic "key" to kafka topic "name".
                        // The name of topic is a required for connector and hence we have to fill the filed when starting
                        // connector.
                        .topicKeys(req.topicKeys)
                        // add the connector key manually since the arguments exposed to user is "group" and "name" than "key"
                        .connectorKey(req.key)
                        .run()
                  }
                  .map(settingInfo => HttpEntity(ContentTypes.`application/json`, settingInfo.toJsonString))
              )
          )
        }
      }
    }
}
