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

import com.island.ohara.agent.DockerClient.ContainerInspector
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
  * Prometheus reload config  using  file or third party framwork like  marathon and k8s etc..
  *
  * This util can change file JSON
  *
  * This config will be drop after k8s merge in OHARA .
  */
class PrometheusConfigUtil(inspectort: ContainerInspector,
                           fileName: String = PrometheusServer.PROMETHEUS_TARGETS_FILE) {
  implicit val Labels_JSON_FORMAT: RootJsonFormat[Labels] = jsonFormat2(Labels)
  implicit val TargetsJson_JSON_FORMAT: RootJsonFormat[TargetsJson] = jsonFormat2(TargetsJson)

  def addTarget(target: String): Unit = setJson(List(getJson().head.addTarget(target)))
  def removeTarget(target: String): Unit = setJson(List(getJson().head.removeTarget(target)))

  def getJson(): Seq[TargetsJson] = {
    val json = inspectort.cat(fileName).getOrElse("")
    json.parseJson.convertTo[Seq[TargetsJson]]
  }

  def setJson(targetsJson: Seq[TargetsJson]): Unit = {
    val json = targetsJson.toJson.prettyPrint
    //in bash -c echo needs to escape twice
    val echo = json.replaceAll("\"", """\\\\\\\"""")
    inspectort.write(fileName, echo)
  }
}

object PrometheusConfigUtil {

  /**
    * change remote docker file
    */
  def apply(inspectort: ContainerInspector): PrometheusConfigUtil = new PrometheusConfigUtil(inspectort)
}

case class TargetsJson(targets: Seq[String], labels: Labels) {
  def addTarget(target: String): TargetsJson = {
    this.copy(targets = this.targets :+ target)
  }
  def removeTarget(target: String): TargetsJson = {
    this.copy(targets = this.targets.filter(_ != target))
  }
}
case class Labels(env: String, job: String)
