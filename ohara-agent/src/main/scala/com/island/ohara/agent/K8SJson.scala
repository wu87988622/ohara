package com.island.ohara.agent

import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsObject, JsString, JsValue, RootJsonFormat}
object K8SJson {
  //for show container information
  final case class EnvInfo(name: String, value: String)
  implicit val ENVINFO_JSON_FORM: RootJsonFormat[EnvInfo] = jsonFormat2(EnvInfo)

  final case class PortInfo(hostPort: Option[Int], containerPort: Int)
  implicit val PORTINFO_JSON_FORMAT: RootJsonFormat[PortInfo] = jsonFormat2(PortInfo)

  final case class Container(image: String, name: String, ports: Seq[PortInfo], env: Option[Seq[EnvInfo]])
  implicit val CONTAINER_JSON_FORMAT: RootJsonFormat[Container] = jsonFormat4(Container)

  final case class Spec(nodeName: Option[String], containers: Seq[Container])
  implicit val SPEC_JSON_FORMAT: RootJsonFormat[Spec] = jsonFormat2(Spec)

  final case class Metadata(uid: String, creationTimestamp: String)
  implicit val METADATA_JSON_FORMAT: RootJsonFormat[Metadata] = jsonFormat2(Metadata)

  final case class Status(phase: String, hostIP: Option[String])
  implicit val STATUS_JSON_FORMAT: RootJsonFormat[Status] = jsonFormat2(Status)

  final case class Items(metadata: Metadata, spec: Spec, status: Status)
  implicit val ITEMS_JSON_FORMAT: RootJsonFormat[Items] = jsonFormat3(Items)

  final case class K8SPodInfo(items: Seq[Items])
  implicit val K8SPODINFO_JSON_FORMAT: RootJsonFormat[K8SPodInfo] = jsonFormat1(K8SPodInfo)

  //for create container
  case class CreatePodPortMapping(containerPort: Int, hostPort: Int)
  implicit val CREATEPOD_PORT_MAPPING_FORMAT: RootJsonFormat[CreatePodPortMapping] = jsonFormat2(CreatePodPortMapping)

  case class CreatePodEnv(name: String, value: String)
  implicit val CREATEPOD_ENV_FORMAT: RootJsonFormat[CreatePodEnv] = jsonFormat2(CreatePodEnv)

  case class CreatePodContainer(name: String, image: String, env: Seq[CreatePodEnv], ports: Seq[CreatePodPortMapping])
  implicit val CREATEPOD_CONTAINER_FORMAT: RootJsonFormat[CreatePodContainer] = jsonFormat4(CreatePodContainer)

  case class CreatePodNodeSelector(hostname: String)
  implicit val CREATEPOD_NODESELECTOR_FORMAT: RootJsonFormat[CreatePodNodeSelector] =
    new RootJsonFormat[CreatePodNodeSelector] {
      override def read(json: JsValue): CreatePodNodeSelector =
        json.asJsObject.getFields("kubernetes.io/hostname") match {
          case Seq(JsString(hostname)) =>
            CreatePodNodeSelector(hostname)
          case other: Any =>
            throw DeserializationException(s"${classOf[CreatePodNodeSelector].getSimpleName} expected but $other")
        }

      override def write(obj: CreatePodNodeSelector) = JsObject(
        "kubernetes.io/hostname" -> JsString(obj.hostname)
      )
    }

  case class CreatePodSpec(nodeSelector: CreatePodNodeSelector, containers: Seq[CreatePodContainer])
  implicit val CREATEPOD_SPEC_FORMAT: RootJsonFormat[CreatePodSpec] = jsonFormat2(CreatePodSpec)

  case class CreatePodMetadata(name: String)
  implicit val CREATEPOD_METADATA_FORMAT: RootJsonFormat[CreatePodMetadata] = jsonFormat1(CreatePodMetadata)

  case class CreatePod(apiVersion: String, kind: String, metadata: CreatePodMetadata, spec: CreatePodSpec)
  implicit val CREATEPOD_FORMAT: RootJsonFormat[CreatePod] = jsonFormat4(CreatePod)

  //for create container result

  case class CreatePodResultMetaData(name: String, uid: String, creationTimestamp: String)
  implicit val CREATEPOD_RESULT_METADATA_FORMAT: RootJsonFormat[CreatePodResultMetaData] = jsonFormat3(
    CreatePodResultMetaData)

  case class CreatePodResultStatus(phase: String)
  implicit val CREATEPOD_RESULT_STATUS_FORMAT: RootJsonFormat[CreatePodResultStatus] = jsonFormat1(
    CreatePodResultStatus)

  case class CreatePodResult(metadata: CreatePodResultMetaData, status: CreatePodResultStatus)
  implicit val CREATEPOD_RESULT_FORMAT: RootJsonFormat[CreatePodResult] = jsonFormat2(CreatePodResult)

}
