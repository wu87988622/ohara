package com.island.ohara.prometheus

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
