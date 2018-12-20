package com.island.ohara.agent
import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

object AgentJson {

  case class Node(name: String, port: Int, user: String, password: String)

  implicit val NODE_JSON_FORMAT: RootJsonFormat[Node] = jsonFormat4(Node)

  //----------------------------------------------------[kafka]----------------------------------------------------//
  sealed trait ClusterDescription {
    def name: String
    def imageName: String
    def nodeNames: Seq[String]
  }

  final case class ZookeeperClusterDescription(name: String,
                                               imageName: String,
                                               clientPort: Int,
                                               peerPort: Int,
                                               electionPort: Int,
                                               nodeNames: Seq[String])
      extends ClusterDescription
  implicit val ZOOKEEPER_CLUSTER_DESCRIPTION_JSON_FORMAT: RootJsonFormat[ZookeeperClusterDescription] = jsonFormat6(
    ZookeeperClusterDescription)

  final case class BrokerClusterDescription(name: String,
                                            imageName: String,
                                            zookeeperClusterName: String,
                                            clientPort: Int,
                                            nodeNames: Seq[String])
      extends ClusterDescription
  implicit val BROKER_CLUSTER_DESCRIPTION_JSON_FORMAT: RootJsonFormat[BrokerClusterDescription] = jsonFormat5(
    BrokerClusterDescription)

  final case class WorkerClusterDescription(name: String,
                                            imageName: String,
                                            brokerClusterName: String,
                                            clientPort: Int,
                                            groupId: String,
                                            statusTopicName: String,
                                            statusTopicPartitions: Int,
                                            statusTopicReplications: Short,
                                            configTopicName: String,
                                            configTopicPartitions: Int,
                                            configTopicReplications: Short,
                                            offsetTopicName: String,
                                            offsetTopicPartitions: Int,
                                            offsetTopicReplications: Short,
                                            nodeNames: Seq[String])
      extends ClusterDescription
  implicit val WORKER_CLUSTER_DESCRIPTION_JSON_FORMAT: RootJsonFormat[WorkerClusterDescription] = jsonFormat15(
    WorkerClusterDescription)

  //----------------------------------------------------[docker]----------------------------------------------------//
  /**
    * the enumeration is referenced to container's status. one of created, restarting, running, removing, paused, exited, or dead.
    * see https://docs.docker.com/engine/reference/commandline/ps/#filtering for more information
    *
    */
  abstract sealed class State extends Serializable {
    // adding a field to display the name from enumeration avoid we break the compatibility when moving code...
    val name: String
  }

  object State {
    case object CREATED extends State {
      val name = "CREATED"
    }

    case object RESTARTING extends State {
      val name = "RESTARTING"
    }

    case object RUNNING extends State {
      val name = "RUNNING"
    }

    case object REMOVING extends State {
      val name = "REMOVING"
    }

    case object PAUSED extends State {
      val name = "PAUSED"
    }

    case object EXITED extends State {
      val name = "EXITED"
    }

    case object DEAD extends State {
      val name = "DEAD"
    }

    val all: Seq[State] = Seq(
      CREATED,
      RESTARTING,
      RUNNING,
      REMOVING,
      PAUSED,
      EXITED,
      DEAD
    )
  }
  implicit val STATE_JSON_FORMAT: RootJsonFormat[State] = new RootJsonFormat[State] {
    override def write(obj: State): JsValue = JsString(obj.name)
    override def read(json: JsValue): State = State.all
      .find(_.name == json.asInstanceOf[JsString].value)
      .getOrElse(throw new IllegalArgumentException(s"Unknown state name:${json.asInstanceOf[JsString].value}"))
  }

  final case class PortPair(hostPort: Int, containerPort: Int)
  implicit val PORT_PAIR_JSON_FORMAT: RootJsonFormat[PortPair] = jsonFormat2(PortPair)

  final case class PortMapping(hostIp: String, portPairs: Seq[PortPair])
  implicit val PORT_MAPPING_JSON_FORMAT: RootJsonFormat[PortMapping] = jsonFormat2(PortMapping)

  final case class ContainerDescription(nodeName: String,
                                        id: String,
                                        imageName: String,
                                        created: String,
                                        state: State,
                                        name: String,
                                        size: String,
                                        portMappings: Seq[PortMapping],
                                        environments: Map[String, String],
                                        hostname: String)
  implicit val CONTAINER_DESCRIPTION_JSON_FORMAT: RootJsonFormat[ContainerDescription] = jsonFormat10(
    ContainerDescription)
}
