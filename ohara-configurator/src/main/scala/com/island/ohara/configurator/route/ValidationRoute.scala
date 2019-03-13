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
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives.{as, complete, entity, onSuccess, path, pathPrefix, put, _}
import com.island.ohara.agent.{BrokerCollie, DockerClient, WorkerCollie}
import com.island.ohara.client.configurator.v0.{ConnectorApi, Parameters}
import com.island.ohara.client.configurator.v0.ValidationApi._
import com.island.ohara.client.kafka.WorkerClient
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.configurator.endpoint.Validator
import com.island.ohara.configurator.fake.{FakeBrokerCollie, FakeWorkerCollie}
import com.typesafe.scalalogging.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  def apply(implicit brokerCollie: BrokerCollie, workerCollie: WorkerCollie): server.Route =
    pathPrefix(VALIDATION_PREFIX_PATH) {
      verifyRoute(
        root = VALIDATION_HDFS_PREFIX_PATH,
        verify = (clusterName, req: HdfsValidationRequest) =>
          CollieUtils.both(if (req.workerClusterName.isEmpty) clusterName else req.workerClusterName).flatMap {
            case (_, topicAdmin, _, workerClient) =>
              Validator.run(workerClient, topicAdmin, req, DEFAULT_NUMBER_OF_VALIDATION)
        }
      ) ~ verifyRoute(
        root = VALIDATION_RDB_PREFIX_PATH,
        verify = (clusterName, req: RdbValidationRequest) =>
          CollieUtils.both(if (req.workerClusterName.isEmpty) clusterName else req.workerClusterName).flatMap {
            case (_, topicAdmin, _, workerClient) =>
              Validator.run(workerClient, topicAdmin, req, DEFAULT_NUMBER_OF_VALIDATION)
        }
      ) ~ verifyRoute(
        root = VALIDATION_FTP_PREFIX_PATH,
        verify = (clusterName, req: FtpValidationRequest) =>
          CollieUtils.both(if (req.workerClusterName.isEmpty) clusterName else req.workerClusterName).flatMap {
            case (_, topicAdmin, _, workerClient) =>
              Validator.run(workerClient, topicAdmin, req, DEFAULT_NUMBER_OF_VALIDATION)
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
                  val name = CommonUtil.randomString(10)
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
                      val endTime = CommonUtil.current() + 3 * 1000 // 3 seconds to timeout
                      while (endTime >= CommonUtil.current()) {
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
          entity(as[ConnectorValidationRequest])(req =>
            onSuccess(CollieUtils.workerClient(Some(req.workerClusterName)).flatMap {
              case (_, workerClient) =>
                workerClient
                  .connectorValidator()
                  .name(req.name)
                  .className(req.className)
                  .numberOfTasks(req.numberOfTasks)
                  .topicNames(req.topicNames)
                  .configs(req.configs)
                  .run()
                  .map {
                    validationResponse =>
                      // we have to replace worker's keyword by ohara's keyword
                      validationResponse.copy(
                        definitions = validationResponse.definitions.map {
                          definition =>
                            // TODO: Could we avoid hard code?? by chia
                            definition.name match {
                              case WorkerClient.CONNECTOR_CLASS_KEY_OF_KAFKA =>
                                definition.copy(name = ConnectorApi.CLASS_NAME_KEY)
                              case WorkerClient.TOPICS_KEY_OF_KAFKA =>
                                definition.copy(name = ConnectorApi.TOPIC_NAME_KEY)
                              case WorkerClient.NUMBER_OF_TASKS_KEY_OF_KAFKA =>
                                definition.copy(name = ConnectorApi.NUMBER_OF_TASKS_KEY)
                              case _ => definition
                            }
                        },
                        validatedValues = validationResponse.validatedValues.map {
                          validatedValue =>
                            // TODO: Could we avoid hard code?? by chia
                            validatedValue.name match {
                              case WorkerClient.CONNECTOR_CLASS_KEY_OF_KAFKA =>
                                validatedValue.copy(name = ConnectorApi.CLASS_NAME_KEY)
                              case WorkerClient.TOPICS_KEY_OF_KAFKA =>
                                validatedValue.copy(name = ConnectorApi.TOPIC_NAME_KEY)
                              case WorkerClient.NUMBER_OF_TASKS_KEY_OF_KAFKA =>
                                validatedValue.copy(name = ConnectorApi.NUMBER_OF_TASKS_KEY)
                              case _ => validatedValue
                            }
                        }
                      )
                  }
            })(complete(_)))
        }
      }
    }
}
