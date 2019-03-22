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
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, _}
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{as, complete, entity, onSuccess, path, pathPrefix, put, _}
import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.agent.{BrokerCollie, WorkerCollie}
import com.island.ohara.client.configurator.v0.ConnectorApi.ConnectorCreationRequest
import com.island.ohara.client.configurator.v0.Parameters
import com.island.ohara.client.configurator.v0.ValidationApi._
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.fake.{FakeBrokerCollie, FakeWorkerClient, FakeWorkerCollie}
import com.island.ohara.kafka.connector.json.SettingDefinition
import com.typesafe.scalalogging.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}

private[configurator] object ValidationRoute extends SprayJsonSupport {
  private[this] val LOG = Logger(ValidationRoute.getClass)
  private[this] val DEFAULT_NUMBER_OF_VALIDATION = 3

  private[this] def verifyRoute[Req](root: String, verify: (Option[String], Req) => Future[Seq[ValidationReport]])(
    implicit rm: RootJsonFormat[Req]): server.Route = path(root) {
    put {
      parameter(Parameters.CLUSTER_NAME.?) { clusterName =>
        entity(as[Req])(
          req =>
            onSuccess(verify(clusterName, req))(
              reports =>
                if (reports.isEmpty)
                  failWith(new IllegalStateException(s"No report!!! Failed to run verification on $root"))
                else complete(reports)))

      }
    }
  }

  private[this] def fakeReport(): Future[Seq[ValidationReport]] = Future.successful(
    (0 until DEFAULT_NUMBER_OF_VALIDATION).map(_ => ValidationReport(CommonUtils.hostname, "a fake report", true)))

  def apply(implicit brokerCollie: BrokerCollie,
            workerCollie: WorkerCollie,
            executionContext: ExecutionContext): server.Route =
    pathPrefix(VALIDATION_PREFIX_PATH) {
      verifyRoute(
        root = VALIDATION_HDFS_PREFIX_PATH,
        verify = (clusterName, req: HdfsValidationRequest) =>
          CollieUtils.both(if (req.workerClusterName.isEmpty) clusterName else req.workerClusterName).flatMap {
            case (_, topicAdmin, _, workerClient) =>
              workerClient match {
                case _: FakeWorkerClient => fakeReport()
                case _                   => ValidationUtils.run(workerClient, topicAdmin, req, DEFAULT_NUMBER_OF_VALIDATION)
              }
        }
      ) ~ verifyRoute(
        root = VALIDATION_RDB_PREFIX_PATH,
        verify = (clusterName, req: RdbValidationRequest) =>
          CollieUtils.both(if (req.workerClusterName.isEmpty) clusterName else req.workerClusterName).flatMap {
            case (_, topicAdmin, _, workerClient) =>
              workerClient match {
                case _: FakeWorkerClient => fakeReport()
                case _                   => ValidationUtils.run(workerClient, topicAdmin, req, DEFAULT_NUMBER_OF_VALIDATION)
              }
        }
      ) ~ verifyRoute(
        root = VALIDATION_FTP_PREFIX_PATH,
        verify = (clusterName, req: FtpValidationRequest) =>
          CollieUtils.both(if (req.workerClusterName.isEmpty) clusterName else req.workerClusterName).flatMap {
            case (_, topicAdmin, _, workerClient) =>
              workerClient match {
                case _: FakeWorkerClient => fakeReport()
                case _                   => ValidationUtils.run(workerClient, topicAdmin, req, DEFAULT_NUMBER_OF_VALIDATION)
              }
        }
      ) ~ verifyRoute(
        root = VALIDATION_NODE_PREFIX_PATH,
        verify = (_, req: NodeValidationRequest) => {
          // TODO: ugly ... please refactor this fucking code. by chia
          if (brokerCollie.isInstanceOf[FakeBrokerCollie] || workerCollie.isInstanceOf[FakeWorkerCollie])
            Future.successful(
              Seq(
                ValidationReport(
                  hostname = req.hostname,
                  message = s"This is fake mode so we didn't test connection actually...",
                  pass = true
                )))
          else
            Future {
              Seq(
                try {
                  val name = CommonUtils.randomString(10)
                  val dockerClient = DockerClient
                    .builder()
                    .hostname(req.hostname)
                    .port(req.port)
                    .user(req.user)
                    .password(req.password)
                    .build()
                  try {
                    val helloWorldImage = "hello-world"
                    dockerClient.containerCreator().name(name).imageName(helloWorldImage).execute()

                    // TODO: should we directly reject the node which doesn't have hello-world image??? by chia
                    def checkImage(): Boolean = {
                      val endTime = CommonUtils.current() + 3 * 1000 // 3 seconds to timeout
                      while (endTime >= CommonUtils.current()) {
                        if (dockerClient.imageNames().contains(s"$helloWorldImage:latest")) return true
                        else TimeUnit.SECONDS.sleep(1)
                      }
                      dockerClient.imageNames().contains(helloWorldImage)
                    }

                    // there are two checks.
                    // 1) is there hello-world image?
                    // 2) did we succeed to run hello-world container?
                    if (!checkImage())
                      ValidationReport(
                        hostname = req.hostname,
                        message = s"Failed to download $helloWorldImage image",
                        pass = true
                      )
                    else if (dockerClient.containerNames().contains(name))
                      ValidationReport(
                        hostname = req.hostname,
                        message = s"succeed to run $helloWorldImage on ${req.hostname}",
                        pass = true
                      )
                    else
                      ValidationReport(
                        hostname = req.hostname,
                        message = s"failed to run container $helloWorldImage",
                        pass = false
                      )
                  } finally try dockerClient.forceRemove(name)
                  catch {
                    case e: Throwable =>
                      LOG.error(s"failed to remove container:$name", e)
                  } finally dockerClient.close()

                } catch {
                  case e: Throwable =>
                    ValidationReport(
                      hostname = req.hostname,
                      message = e.getMessage,
                      pass = false
                    )
                }
              )
            }
        }
      ) ~ path(VALIDATION_CONNECTOR_PREFIX_PATH) {
        put {
          entity(as[ConnectorCreationRequest])(req =>
            onSuccess(CollieUtils.workerClient(req.workerClusterName).flatMap {
              case (cluster, workerClient) =>
                workerClient
                  .connectorValidator()
                  .connectorClassName(req.className)
                  .settings(req.plain)
                  // we define the cluster name again since user may ignore the worker cluster in request
                  // matching a cluster is supported by ohara 0.3 so we have to set matched cluster to response
                  .setting(SettingDefinition.WORKER_CLUSTER_NAME_DEFINITION.key(), cluster.name)
                  .run
            })(settingInfo => complete(HttpEntity(ContentTypes.`application/json`, settingInfo.toJsonString))))
        }
      }
    }
}
