package com.island.ohara.agent

import java.util.Objects

import com.island.ohara.agent.DockerClient.Executor
import com.island.ohara.agent.DockerJson.{ContainerDescription, PortMapping, PortPair, State}
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.typesafe.scalalogging.Logger

/**
  * a interface used to control remote node's docker service.
  * the default implementation is based on ssh client.
  * NOTED: All contaiers are executed background so as to avoid blocking call.
  */
trait DockerClient extends ReleaseOnce {

  /**
    * @param name container's name
    * @return true if container exists. otherwise, false
    */
  def exist(name: String): Boolean = containers().exists(_.name == name)

  /**
    * @param name container's name
    * @return true if container does not exist. otherwise, true
    */
  def nonExist(name: String): Boolean = !exist(name)

  /**
    * @return a collection of running docker containers
    */
  def activeContainers(): Seq[ContainerDescription] = containers().filter(_.state == State.RUNNING)

  /**
    * @return a collection of docker containers
    */
  def containers(): Seq[ContainerDescription]

  /**
    * @param name container's name
    * @return container description or None if container doesn't exist
    */
  def container(name: String): Option[ContainerDescription] = containers().find(_.name == name)

  /**
    * @param name container's id
    * @return container description or None if container doesn't exist
    */
  def containerById(id: String): Option[ContainerDescription] = containers().find(_.id == id)

  /**
    * start a docker container.
    * @return a executor
    */
  def executor(): Executor

  /**
    * stop a running container. If the container doesn't exist, exception will be thrown.
    * Noted: Please use #stopById to stop container by id
    * @param name container's name
    * @return container information.
    */
  def stop(name: String): ContainerDescription

  /**
    * remove a stopped container. If the container doesn't exist, exception will be thrown.
    * Noted: Please use #stopById to remove container by id
    * @param name container's name
    * @return container information.
    */
  def remove(name: String): ContainerDescription

  /**
    * stop a running container. If the container doesn't exist, exception will be thrown.
    * Noted: Please use #stop to stop container by name
    * @param id container's id
    * @return container information.
    */
  def stopById(id: String): ContainerDescription

  /**
    * remove a stopped container. If the container doesn't exist, exception will be thrown.
    * Noted: Please use #stop to remove container by name
    * @param id container's id
    * @return container information.
    */
  def removeById(id: String): ContainerDescription

  /**
    * @param id container's id
    * @return true if container exists. otherwise, false
    */
  def existById(id: String): Boolean = containers().exists(_.id == id)

  /**
    * @param id container's id
    * @return true if container does not exist. otherwise, true
    */
  def nonExistById(id: String): Boolean = !existById(id)

  /**
    * check whether the remote node is capable of running docker.
    * @return true if remote node succeed to run hello-world container. otherwise, false.
    */
  def verify(): Boolean

  /**
    * get the console log from the container
    * @param name container's name
    * @return log
    */
  def log(name: String): String

  /**
    * get content of specified file from a container.
    * This method is useful in debugging when you want to check something according to the file content.
    * @param name container's name
    * @param path file path
    * @return content of file
    */
  def cat(name: String, path: String): Option[String]

  def images(): Seq[String]
}

object DockerClient {
  private[agent] val LOG = Logger(classOf[DockerClient])

  /**
    * Parse the forward ports from container.
    * NOTED: the form of docker's port is shown below:
    * 1) 0.0.0.0:12345-12350->12345-12350/tcp
    * 2) 0.0.0.0:2181->2181/tcp, 0.0.0.0:2888->2888/tcp, 0.0.0.0:3888->3888/tcp
    * This method is exposed to testing scope
    * @param portMapping port mapping string
    * @return (host's ip -> (host's port -> container's port)
    */
  private[agent] def parsePortMapping(portMapping: String): Seq[PortMapping] = {
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

  private[agent] val DIVIDER = ",,"

  private[agent] val LIST_PROCESS_FORMAT = Seq(
    "{{.ID}}",
    "{{.Image}}",
    "{{.CreatedAt}}",
    "{{.Status}}",
    "{{.Names}}",
    "{{.Size}}",
    "{{.Ports}}"
  ).mkString(DIVIDER)
  def builder(): Builder = new Builder

  /**
    * a interface used to run a docker container on remote node
    */
  trait Executor {

    /**
      * set true if you want to clean up the dead container automatically
      * @return executor
      */
    def cleanup(): Executor

    /**
      * set container's name. default is a random string
      * @param name container name
      * @return this executor
      */
    def name(name: String): Executor

    /**
      * set target image
      * @param imageName docker image
      * @return this executor
      */
    def imageName(imageName: String): Executor

    /**
      * the command passed to docker container
      * @param command command
      * @return this executor
      */
    def command(command: String): Executor

    /**
      * @param hostname the hostname of container
      * @return this executor
      */
    def hostname(hostname: String): Executor

    /**
      * @param envs the env variables exposed to container
      * @return this executor
      */
    def envs(envs: Map[String, String]): Executor

    /**
      * @param route the pre-defined route to container. hostname -> ip
      * @return this executor
      */
    def route(route: Map[String, String]): Executor

    /**
      * forward the port from host to container.
      * NOTED: currently we don't support to specify the network interface so the forwarded port is bound on all networkd adapters.
      * @param ports port mapping (host's port -> container's port)
      * @return this executor
      */
    def portMappings(ports: Map[Int, Int]): Executor

    /**
      * execute the docker container on background
      * @return process information
      */
    def run(): Option[ContainerDescription]

    /**
      * this is used in testing. Devlopers can check the generated command by this method.
      * @return the command used to start docker container
      */
    protected[agent] def dockerCommand(): String
  }

  class Builder private[agent] {
    private[this] var hostname: String = _
    private[this] var port: Int = 22
    private[this] var user: String = _
    private[this] var password: String = _

    def hostname(hostname: String): Builder = {
      this.hostname = hostname
      this
    }

    def port(port: Int): Builder = {
      this.port = port
      this
    }

    def user(user: String): Builder = {
      this.user = user
      this
    }

    def password(password: String): Builder = {
      this.password = password
      this
    }

    def build(): DockerClient = new DockerClient {
      private[this] val agent = Agent
        .builder()
        .hostname(Objects.requireNonNull(hostname))
        .port(port)
        .user(Objects.requireNonNull(user))
        .password(Objects.requireNonNull(password))
        .build()

      override protected def doClose(): Unit = ReleaseOnce.close(agent)

      override def executor(): Executor = new Executor {
        private[this] var imageName: String = _
        private[this] var name: String = CommonUtil.uuid()
        private[this] var command: String = _
        private[this] var disableCleanup: Boolean = true
        private[this] var ports: Map[Int, Int] = Map.empty
        private[this] var envs: Map[String, String] = Map.empty
        private[this] var route: Map[String, String] = Map.empty
        private[this] var hostname: String = _

        override def name(name: String): Executor = {
          this.name = name
          this
        }

        override def imageName(imageName: String): Executor = {
          this.imageName = imageName
          this
        }

        override def run(): Option[ContainerDescription] = {
          val cmd = dockerCommand()
          DockerClient.LOG.info(s"docker command:$cmd")
          agent.execute(cmd)
          activeContainers().find(_.name == name)
        }

        override def command(command: String): Executor = {
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

        override def portMappings(ports: Map[Int, Int]): Executor = {
          this.ports = ports;
          this
        }

        override def hostname(hostname: String): Executor = {
          this.hostname = hostname
          this
        }

        override def envs(envs: Map[String, String]): Executor = {
          this.envs = envs
          this
        }
        override def route(route: Map[String, String]): Executor = {
          this.route = route
          this
        }

        override def cleanup(): Executor = {
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
                  image = items(1),
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

      override def cat(name: String, path: String): Option[String] =
        agent.execute(s"""docker exec $name /bin/bash -c \"cat $path\"""")

      override def images(): Seq[String] = agent
        .execute("docker images --format {{.Repository}}:{{.Tag}}")
        .map(_.split("\n").toSeq)
        .filter(_.nonEmpty)
        .getOrElse(Seq.empty)
    }
  }
}
