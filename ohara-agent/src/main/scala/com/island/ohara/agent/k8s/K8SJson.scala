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

package com.island.ohara.agent.k8s

import com.island.ohara.agent.k8s.K8SClient.{ImagePullPolicy, RestartPolicy}
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsObject, JsString, JsValue, RootJsonFormat}

object K8SJson {
  //for show container information
  final case class EnvInfo(name: String, value: Option[String])
  implicit val ENVINFO_JSON_FORM: RootJsonFormat[EnvInfo] = jsonFormat2(EnvInfo)

  final case class PortInfo(hostPort: Option[Int], containerPort: Int)
  implicit val PORTINFO_JSON_FORMAT: RootJsonFormat[PortInfo] = jsonFormat2(PortInfo)

  final case class Container(image: String, name: String, ports: Option[Seq[PortInfo]], env: Option[Seq[EnvInfo]])
  implicit val CONTAINER_JSON_FORMAT: RootJsonFormat[Container] = jsonFormat4(Container)

  final case class Spec(nodeName: Option[String], containers: Seq[Container], hostname: Option[String])
  implicit val SPEC_JSON_FORMAT: RootJsonFormat[Spec] = jsonFormat3(Spec)

  final case class Metadata(uid: String, name: String, creationTimestamp: String)
  implicit val METADATA_JSON_FORMAT: RootJsonFormat[Metadata] = jsonFormat3(Metadata)

  final case class Status(phase: String, hostIP: Option[String])
  implicit val STATUS_JSON_FORMAT: RootJsonFormat[Status] = jsonFormat2(Status)

  final case class Items(metadata: Metadata, spec: Spec, status: Status)
  implicit val ITEMS_JSON_FORMAT: RootJsonFormat[Items] = jsonFormat3(Items)

  final case class K8SPodInfo(items: Seq[Items])
  implicit val K8SPODINFO_JSON_FORMAT: RootJsonFormat[K8SPodInfo] = jsonFormat1(K8SPodInfo)

  //for show node infomation

  final case class NodeAddresses(nodeType: String, nodeAddress: String)
  implicit val NODE_HOSTINFO_FORMAT: RootJsonFormat[NodeAddresses] =
    new RootJsonFormat[NodeAddresses] {
      override def write(obj: NodeAddresses): JsValue = JsObject(
        "type" -> JsString(obj.nodeType),
        "address" -> JsString(obj.nodeAddress)
      )

      override def read(json: JsValue): NodeAddresses =
        json.asJsObject.getFields("type", "address") match {
          case Seq(JsString(nodeType), JsString(nodeAddress)) =>
            NodeAddresses(nodeType, nodeAddress)
          case other: Any =>
            throw DeserializationException(s"${classOf[NodeAddresses].getSimpleName} expected but $other")
        }
    }

  final case class ImageNames(names: Seq[String])
  implicit val NODE_IMAGENAMES_FORMAT: RootJsonFormat[ImageNames] = jsonFormat1(ImageNames)

  final case class Condition(conditionType: String, status: String, message: String)
  implicit val CONDITION_JSON_FORMAT: RootJsonFormat[Condition] =
    new RootJsonFormat[Condition] {
      override def read(json: JsValue): Condition =
        json.asJsObject.getFields("type", "status", "message") match {
          case Seq(JsString(conditionType), JsString(status), JsString(message)) =>
            Condition(conditionType, status, message)
          case other: Any =>
            throw DeserializationException(s"${classOf[Condition].getSimpleName} expected but $other")
        }

      override def write(obj: Condition): JsValue = JsObject(
        "type" -> JsString(obj.conditionType),
        "status" -> JsString(obj.status),
        "message" -> JsString(obj.message)
      )
    }

  final case class NodeStatus(addresses: Seq[NodeAddresses], images: Seq[ImageNames], conditions: Seq[Condition])
  implicit val NODESTATUS_JSON_FORMAT: RootJsonFormat[NodeStatus] = jsonFormat3(NodeStatus)

  final case class NodeMetaData(name: String)
  implicit val NODEMETADATA_JSON_FORMAT: RootJsonFormat[NodeMetaData] = jsonFormat1(NodeMetaData)

  final case class NodeItems(status: NodeStatus, metadata: NodeMetaData)
  implicit val NODEITEMS_JSON_FORMAT: RootJsonFormat[NodeItems] = jsonFormat2(NodeItems)

  final case class K8SNodeInfo(items: Seq[NodeItems])
  implicit val K8SNODEINFO_JSON_FORMAT: RootJsonFormat[K8SNodeInfo] = jsonFormat1(K8SNodeInfo)

  //for create container
  final case class CreatePodPortMapping(containerPort: Int, hostPort: Int)
  implicit val CREATEPOD_PORT_MAPPING_FORMAT: RootJsonFormat[CreatePodPortMapping] = jsonFormat2(CreatePodPortMapping)

  final case class CreatePodEnv(name: String, value: String)
  implicit val CREATEPOD_ENV_FORMAT: RootJsonFormat[CreatePodEnv] = jsonFormat2(CreatePodEnv)

  implicit val IMAGE_PULL_POLICY_FORMAT: RootJsonFormat[ImagePullPolicy] = new RootJsonFormat[ImagePullPolicy] {
    override def read(json: JsValue): ImagePullPolicy = ImagePullPolicy.forName(json.convertTo[String])

    override def write(obj: ImagePullPolicy): JsValue = JsString(obj.toString)
  }

  final case class CreatePodContainer(name: String,
                                      image: String,
                                      env: Seq[CreatePodEnv],
                                      ports: Seq[CreatePodPortMapping],
                                      imagePullPolicy: ImagePullPolicy,
                                      command: Seq[String],
                                      args: Seq[String])

  implicit val CREATEPOD_CONTAINER_FORMAT: RootJsonFormat[CreatePodContainer] = jsonFormat7(CreatePodContainer)

  final case class CreatePodNodeSelector(hostname: String)
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

  final case class HostAliases(ip: String, hostnames: Seq[String])
  implicit val HOST_ALIASES_FORMAT: RootJsonFormat[HostAliases] = jsonFormat2(HostAliases)

  final case class CreatePodSpec(nodeSelector: CreatePodNodeSelector,
                                 hostname: String,
                                 subdomain: String,
                                 hostAliases: Seq[HostAliases],
                                 containers: Seq[CreatePodContainer],
                                 restartPolicy: RestartPolicy)

  implicit val RESTART_POLICY_JSON_FORMAT: RootJsonFormat[RestartPolicy] = new RootJsonFormat[RestartPolicy] {
    override def read(json: JsValue): RestartPolicy = RestartPolicy.forName(json.convertTo[String])

    override def write(obj: RestartPolicy): JsValue = JsString(obj.toString)
  }
  implicit val CREATEPOD_SPEC_FORMAT: RootJsonFormat[CreatePodSpec] = jsonFormat6(CreatePodSpec)

  final case class CreatePodLabel(name: String)
  implicit val CREATEPOD_LABEL_FORMAT: RootJsonFormat[CreatePodLabel] = jsonFormat1(CreatePodLabel)

  final case class CreatePodMetadata(name: String, labels: CreatePodLabel)
  implicit val CREATEPOD_METADATA_FORMAT: RootJsonFormat[CreatePodMetadata] = jsonFormat2(CreatePodMetadata)

  final case class CreatePod(apiVersion: String, kind: String, metadata: CreatePodMetadata, spec: CreatePodSpec)
  implicit val CREATEPOD_FORMAT: RootJsonFormat[CreatePod] = jsonFormat4(CreatePod)

  //for create container result

  final case class CreatePodResultMetaData(name: String, uid: String, creationTimestamp: String)
  implicit val CREATEPOD_RESULT_METADATA_FORMAT: RootJsonFormat[CreatePodResultMetaData] = jsonFormat3(
    CreatePodResultMetaData)

  final case class CreatePodResultStatus(phase: String)
  implicit val CREATEPOD_RESULT_STATUS_FORMAT: RootJsonFormat[CreatePodResultStatus] = jsonFormat1(
    CreatePodResultStatus)

  final case class CreatePodResult(metadata: CreatePodResultMetaData, status: CreatePodResultStatus)
  implicit val CREATEPOD_RESULT_FORMAT: RootJsonFormat[CreatePodResult] = jsonFormat2(CreatePodResult)

  //for error
  final case class K8SErrorResponse(message: String)
  implicit val K8SERROR_RESPONSE_FORMAT: RootJsonFormat[K8SErrorResponse] = jsonFormat1(K8SErrorResponse)
}
