package com.island.ohara.agent
import java.util.Objects

import com.island.ohara.agent.AgentJson.{ContainerDescription, PortMapping, PortPair, State}
import com.island.ohara.agent.DockerClient._
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.typesafe.scalalogging.Logger

private[agent] object DockerClientImpl {
  private val LOG = Logger(classOf[DockerClientImpl])

  /**
    * Parse the forward ports from container.
    * NOTED: the form of docker's port is shown below:
    * 1) 0.0.0.0:12345-12350->12345-12350/tcp
    * 2) 0.0.0.0:2181->2181/tcp, 0.0.0.0:2888->2888/tcp, 0.0.0.0:3888->3888/tcp
    * This method is exposed to testing scope
    * @param portMapping port mapping string
    * @return (host's ip -> (host's port -> container's port)
    */
  def parsePortMapping(portMapping: String): Seq[PortMapping] = {
    portMapping
      .replaceAll("/tcp", "")
      // we don't distinguish protocol now.. TODO by chia
      .replaceAll("/udp", "")
      .split(", ")
      .flatMap { portMap =>
        val portPair = portMap.split("->")
        if (portPair.length != 2) throw new IllegalArgumentException(s"invalid format: $portPair")

        def parsePort(portsString: String): Seq[Int] = if (portsString.contains("-"))
          portsString.split("-").head.toInt to portsString.split("-").last.toInt
        else Seq(portsString.toInt)
        if (portPair.head.split(":").length != 2)
          throw new IllegalArgumentException(s"Illegal ${portPair.head} (expected hostname:port)")
        val network: String = portPair.head.split(":").head
        val hostPorts: Seq[Int] = parsePort(portPair.head.split(":").last)
        val containerPort: Seq[Int] = parsePort(portPair.last)
        if (hostPorts.size != containerPort.size)
          throw new IllegalArgumentException("the size of host's port is not matched to size of container's port")
        hostPorts.zipWithIndex.map {
          case (port, index) => (network, port, containerPort(index))
        }
      }
      .groupBy(_._1)
      .map {
        case (key, value) => PortMapping(key, value.map(v => PortPair(v._2, v._3)).toSeq)
      }
      .toSeq
  }

  val DIVIDER: String = ",,"

  val LIST_PROCESS_FORMAT: String = Seq(
    "{{.ID}}",
    "{{.Image}}",
    "{{.CreatedAt}}",
    "{{.Status}}",
    "{{.Names}}",
    "{{.Size}}",
    "{{.Ports}}"
  ).mkString(DIVIDER)
}

import DockerClientImpl._
private[agent] class DockerClientImpl(hostname: String, port: Int, user: String, password: String)
    extends ReleaseOnce
    with DockerClient {
  private[this] val agent = Agent
    .builder()
    .hostname(Objects.requireNonNull(hostname))
    .port(port)
    .user(Objects.requireNonNull(user))
    .password(Objects.requireNonNull(password))
    .build()

  override protected def doClose(): Unit = ReleaseOnce.close(agent)

  override def containerCreator(): ContainerCreator = new ContainerCreator {
    private[this] var imageName: String = _
    private[this] var name: String = CommonUtil.uuid()
    private[this] var command: String = _
    private[this] var disableCleanup: Boolean = true
    private[this] var ports: Map[Int, Int] = Map.empty
    private[this] var envs: Map[String, String] = Map.empty
    private[this] var route: Map[String, String] = Map.empty
    private[this] var hostname: String = _

    override def name(name: String): ContainerCreator = {
      this.name = name
      this
    }

    override def imageName(imageName: String): ContainerCreator = {
      this.imageName = imageName
      this
    }

    override def run(): Option[ContainerDescription] = {
      val cmd = dockerCommand()
      LOG.info(s"docker command:$cmd")
      agent.execute(cmd)
      activeContainers().find(_.name == name)
    }

    override def command(command: String): ContainerCreator = {
      this.command = command
      this
    }

    override protected[agent] def dockerCommand(): String = Seq(
      "docker run -d ",
      if (hostname == null) "" else s"-h $hostname",
      route
        .map {
          case (hostname, ip) => s"--add-host $hostname:$ip"
        }
        .mkString(" "),
      if (disableCleanup) "" else "--rm",
      s"--name ${Objects.requireNonNull(name)}",
      ports
        .map {
          case (hostPort, containerPort) => s"-p $hostPort:$containerPort"
        }
        .mkString(" "),
      envs
        .map {
          case (key, value) => s"""-e \"$key=$value\""""
        }
        .mkString(" "),
      Objects.requireNonNull(imageName),
      if (command == null) "" else command
    ).filter(_.nonEmpty).mkString(" ")

    override def portMappings(ports: Map[Int, Int]): ContainerCreator = {
      this.ports = ports;
      this
    }

    override def hostname(hostname: String): ContainerCreator = {
      this.hostname = hostname
      this
    }

    override def envs(envs: Map[String, String]): ContainerCreator = {
      this.envs = envs
      this
    }
    override def route(route: Map[String, String]): ContainerCreator = {
      this.route = route
      this
    }

    override def cleanup(): ContainerCreator = {
      this.disableCleanup = false
      this
    }
  }

  override def containers(): Seq[ContainerDescription] = agent
    .execute(s"docker ps -a --format $LIST_PROCESS_FORMAT")
    .map(_.split("\n"))
    .map {
      _.flatMap { line =>
        try {
          // filter out all empty string
          val items = line.split(DIVIDER).filter(_.nonEmpty).toSeq
          // not all containers have forward ports so length - 1
          if (items.length != LIST_PROCESS_FORMAT.split(DIVIDER).length
              && items.length != LIST_PROCESS_FORMAT.split(DIVIDER).length - 1)
            throw new IllegalArgumentException(
              s"the expected number of items in $line is ${LIST_PROCESS_FORMAT.split(DIVIDER).length} or ${LIST_PROCESS_FORMAT.split(DIVIDER).length - 1}")
          val id = items.head
          Some(
            ContainerDescription(
              nodeName = hostname,
              id = id,
              imageName = items(1),
              created = items(2),
              state = State.all
                .find(s => items(3).toLowerCase.contains(s.name.toLowerCase))
                // the running container show "up to xxx"
                .getOrElse(State.RUNNING),
              name = items(4),
              size = items(5),
              portMappings = if (items.size < 7) Seq.empty else parsePortMapping(items(6)),
              environments = agent
                .execute(s"docker inspect $id --format '{{.Config.Env}}'")
                .map(_.replaceAll("\n", ""))
                // form: [abc=123 aa=111]
                .filter(_.length > 2)
                .map(_.substring(1))
                .map(s => s.substring(0, s.length - 1))
                .map { s =>
                  s.split(" ")
                    .filterNot(_.isEmpty)
                    .map { line =>
                      val items = line.split("=")
                      items.head -> items.last
                    }
                    .toMap
                }
                .getOrElse(Map.empty),
              // we can't cat file from a exited container
              hostname = agent
                .execute(s"docker inspect $id --format '{{.Config.Hostname}}'")
                // remove new line
                .map(_.replaceAll("\n", ""))
                .get
            ))
        } catch {
          case e: Throwable =>
            val errorMessage = s"failed to get container description from $hostname." +
              "This error may be caused by operator conflict since we can't get container information by single command."
            LOG.error(errorMessage, e)
            None
        }
      }.toSeq
    }
    .getOrElse(Seq.empty)

  override def stop(name: String): ContainerDescription =
    containers()
      .find(_.name == name)
      .map(container => {
        agent.execute(s"docker stop $name")
        container
      })
      .getOrElse(throw new IllegalArgumentException(s"Name:$name doesn't exist"))

  override def remove(name: String): ContainerDescription =
    containers()
      .find(_.name == name)
      .map(container => {
        agent.execute(s"docker rm $name")
        container
      })
      .getOrElse(throw new IllegalArgumentException(s"Name:$name doesn't exist"))

  override def stopById(id: String): ContainerDescription =
    containers()
      .find(_.id == id)
      .map(container => {
        agent.execute(s"docker stop $id")
        container
      })
      .getOrElse(throw new IllegalArgumentException(s"Id:$id doesn't exist"))

  override def removeById(id: String): ContainerDescription =
    containers()
      .find(_.id == id)
      .map(container => {
        agent.execute(s"docker rm $id")
        container
      })
      .getOrElse(throw new IllegalArgumentException(s"Id:$id doesn't exist"))

  override def verify(): Boolean =
    agent.execute("docker run --rm hello-world").exists(_.contains("Hello from Docker!"))

  override def log(name: String): String = agent
    .execute(s"docker container logs $name")
    .map(msg => if (msg.contains("ERROR:")) throw new IllegalArgumentException(msg) else msg)
    .getOrElse(throw new IllegalArgumentException(s"no response from $hostname"))

  override def containerInspector(containerName: String): ContainerInspector = containerInspector(containerName, false)

  private[this] def containerInspector(containerName: String, beRoot: Boolean): ContainerInspector =
    new ContainerInspector {
      private[this] def rootConfig: String = if (beRoot) "-u root" else ""
      override def cat(path: String): Option[String] =
        agent.execute(s"""docker exec $rootConfig $containerName /bin/bash -c \"cat $path\"""")

      override def append(path: String, content: Seq[String]): String = {
        agent.execute(
          s"""docker exec $rootConfig $containerName /bin/bash -c \"echo \\"${content.mkString("\n")}\\" >> $path\"""")
        cat(path).get
      }

      override def asRoot(): ContainerInspector = containerInspector(containerName, true)
    }

  override def images(): Seq[String] = agent
    .execute("docker images --format {{.Repository}}:{{.Tag}}")
    .map(_.split("\n").toSeq)
    .filter(_.nonEmpty)
    .getOrElse(Seq.empty)

  override def toString: String = s"$user@$hostname:$port"

}
