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

package com.island.ohara.it.prometheus

import com.island.ohara.agent.DockerClient
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.common.util.CommonUtil
import com.typesafe.scalalogging.Logger

/**
  * prometheus server
  */
trait PrometheusServer {
  def start(): PrometheusDescription
  def stop(): PrometheusDescription
}

trait PrometheusCreator {
  def create(node: Node, name: String = PrometheusCreator.name("prometheus")): PrometheusServer
  def clientPort(clientPort: Int): PrometheusCreator
  //option
  def dataDir(dataDir: String): PrometheusCreator
  def targetConfigDir(targetConfigDir: String): PrometheusCreator
  def targets(targets: Seq[String]): PrometheusCreator
}

object PrometheusCreator {

  def name(service: String): String = s"$service-${CommonUtil.randomString(10)}"

  private[this] val logger = Logger(classOf[PrometheusCreator])
  def apply(): PrometheusCreator = new PrometheusCreator() {
    private[this] var clientPort: Int = PrometheusServer.CLIENT_PORT_DEFAULT
    private[this] var targetConfigDir: String = _
    private[this] var dataDir: String = _
    private[this] var targets: Seq[String] = _

    override def clientPort(clientPort: Int): PrometheusCreator = {
      this.clientPort = clientPort
      this
    }

    override def dataDir(dataDir: String): PrometheusCreator = {
      this.dataDir = dataDir
      this
    }

    override def targetConfigDir(targetConfigDir: String): PrometheusCreator = {
      this.targetConfigDir = targetConfigDir
      this
    }

    override def targets(targets: Seq[String]): PrometheusCreator = {
      this.targets = targets
      this
    }
    override def create(node: Node, name: String): PrometheusServer = {
      var imageName: String = null
      if (imageName == null) imageName = PrometheusServer.IMAGE_NAME_DEFAULT

      new PrometheusServer {

        val prometheusDescription = PrometheusDescription(name = name,
                                                          imageName = imageName,
                                                          clientPort = clientPort,
                                                          targets = targets,
                                                          targetConfigDir = targetConfigDir,
                                                          dataDir = dataDir)
        override def start(): PrometheusDescription = {
          val client =
            DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
          try {
            client
              .containerCreator()
              .imageName(imageName)
              .volumeMapping({
                Option(targetConfigDir)
                  .map(x => Map(x -> PrometheusServer.PROMETHEUS_TARGET_CONFIG_DIR))
                  .getOrElse(Map()) ++
                  Option(dataDir).map(x => Map(x -> PrometheusServer.PROMETHEUS_DATA_DIR)).getOrElse(Map())
              })
              .portMappings(Map(
                clientPort -> PrometheusServer.CLIENT_PORT_DEFAULT
              ))
              //        .hostname(hostname)
              .envs(Map(
                PrometheusServer.PROMETHEUS_TARGETS_KEY -> Option(targets.mkString(",")).getOrElse("")
              ))
              .name(name)
              //        .route(route)
              .run()
          } catch {
            case e: Throwable =>
              client.remove(name)
              logger.error(s"failed to start $imageName", e)
              None
          } finally client.close()
          prometheusDescription
        }

        override def stop(): PrometheusDescription = {
          val client =
            DockerClient.builder().user(node.user).password(node.password).hostname(node.name).port(node.port).build()
          client.stop(name)
          client.remove(name)
          prometheusDescription
        }
      }
    }
  }
}

object PrometheusServer {

  def creater(): PrometheusCreator = PrometheusCreator.apply()

  private[ohara] val IMAGE_NAME_DEFAULT: String = "oharastream/ohara:prometheus"

  private[prometheus] val PROMETHEUS_TARGET_CONFIG_DIR: String = "/home/prometheus/config/targets"
  private[prometheus] val PROMETHEUS_DATA_DIR: String = "/home/prometheus/data"
  private[prometheus] val PROMETHEUS_TARGETS_FILE: String = "/home/prometheus/config/targets/targets.json"

  private[prometheus] val PROMETHEUS_TARGETS_KEY: String = "PROMETHEUS_TARGETS"
  private[prometheus] val CLIENT_PORT_KEY: String = "BROKER_CLIENT_PORT"
  private[prometheus] val CLIENT_PORT_DEFAULT: Int = 9090

}

final case class PrometheusDescription(name: String,
                                       imageName: String,
                                       targets: Seq[String],
                                       targetConfigDir: String,
                                       clientPort: Int,
                                       dataDir: String)
