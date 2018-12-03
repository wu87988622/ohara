package com.island.ohara.agent

import java.io.ByteArrayOutputStream
import java.nio.charset.{Charset, StandardCharsets}
import java.util.Objects
import java.util.concurrent.{ConcurrentSkipListMap, TimeUnit}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.island.ohara.agent.DockerJson.ContainerDescription
import com.island.ohara.agent.KafkaJson._
import com.island.ohara.common.util.CommonUtil
import org.apache.log4j.spi.Configurator
import org.apache.sshd.client.SshClient

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.{ClassTag, classTag}

/**
  * used to execute command on remote node.
  * the default implementation is based on ssh
  */
object Agent {

  private[this] implicit val clusterManager: ClusterManager = new ClusterManager {
    private[this] val remoteNodes: Map[String, Node] = Seq(
      "node00",
      "node01",
      "node02"
    ).map(node => node -> Node(node, 22, "chia7712", "jellynina0208")).toMap

    private[this] val cache = new ConcurrentSkipListMap[String, AnyRef]()
    override def containers(clusterName: String, service: String): Map[Node, Seq[ContainerDescription]] =
      remoteNodes.values.map { node =>
        val dockerClient =
          DockerClient.builder().hostname(node.hostname).password(node.password).user(node.user).port(node.port).build()
        try node -> dockerClient.containers().filter(_.name.startsWith(s"$clusterName-$service"))
        catch {
          case e: Throwable => throw new IllegalArgumentException("Are you running docker daemon", e)
        } finally dockerClient.close()
      }.toMap

    override def removeCluster(name: String): Unit = cache.remove(name)

    override def addCluster(name: String, cluster: AnyRef): Unit = cache.put(name, cluster)

    override def existCluster(name: String): Boolean = cache.containsKey(name)

    override def clusters[Cluster: ClassTag](): Seq[Cluster] =
      cache.values().asScala.filter(classTag[Cluster].runtimeClass.isInstance).map(_.asInstanceOf[Cluster]).toSeq

    override def cluster[Cluster: ClassTag](name: String): Cluster = Option(cache.get(name))
      .filter(classTag[Cluster].runtimeClass.isInstance)
      .map(_.asInstanceOf[Cluster])
      .getOrElse(throw new IllegalArgumentException(s"$name doesn't exist"))

    override def node(name: String): Node = remoteNodes(name)

    override def hostname(service: String, index: Int): String = s"$service$index"

    override def containerName(clusterName: String, service: String, index: Int): String =
      s"$clusterName-${hostname(service, index)}"
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 1) throw new IllegalArgumentException("[Usage] <port>")

    val port = args.head.toInt

    implicit val actorSystem: ActorSystem = ActorSystem(s"${classOf[Configurator].getSimpleName}-system")
    implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

    val httpServer: Http.ServerBinding =
      Await.result(
        Http().bindAndHandle(
          ZookeeperRoute.apply ~ BrokerRoute.apply ~ WorkerRoute.apply,
          CommonUtil.anyLocalAddress(),
          port
        ),
        60 seconds
      )
    try {
      println(s"Agent is bound on ${httpServer.localAddress.getHostString}:${httpServer.localAddress.getPort}")
      TimeUnit.SECONDS.sleep(3000)
    } finally {
      httpServer.unbind()
      actorSystem.terminate()
    }
  }

  def channel(): Channel = new Channel {
    private[this] var hostname: String = _
    private[this] var port: Int = 22
    private[this] var user: String = _
    private[this] var password: String = _
    private[this] var charset = StandardCharsets.US_ASCII
    override def hostname(hostname: String): Channel = {
      this.hostname = hostname
      this
    }

    override def port(port: Int): Channel = {
      this.port = port
      this
    }

    override def user(user: String): Channel = {
      this.user = user
      this
    }

    override def password(password: String): Channel = {
      this.password = password
      this
    }

    override def charset(charset: Charset): Channel = {
      this.charset = charset
      this
    }

    override def execute(command: String): Option[String] = {
      Objects.requireNonNull(hostname)
      if (port <= 0) throw new IllegalArgumentException(s"$port should be bigger than zero")
      Objects.requireNonNull(user)
      Objects.requireNonNull(password)
      Objects.requireNonNull(charset)
      val client = SshClient.setUpDefaultSimpleClient()
      try {
        // TODO: This method can't set the timeout...It is not ok in production I'd say...by chia
        val session = client.sessionLogin(hostname, port, user, password)
        val stdOut = new ByteArrayOutputStream
        try {
          val stdError = new ByteArrayOutputStream
          try {
            session.executeRemoteCommand(command, stdOut, stdError, charset)
            if (stdOut.size() != 0) Some(new String(stdOut.toByteArray, charset))
            else if (stdError.size() != 0) Some(new String(stdError.toByteArray, charset))
            else None
          } finally stdError.close()
        } finally stdOut.close()
      } finally client.close()
    }
  }

  /**
    * a ssh channel between this node and remote node
    */
  trait Channel {

    /**
      * set remote hostname
      * @param hostname hostname
      * @return this channel
      */
    def hostname(hostname: String): Channel

    /**
      * set remote's ssh port
      * @param port ssh port
      * @return this channel
      */
    def port(port: Int): Channel

    /**
      * set remote's ssh user
      * @param user ssh user
      * @return this channel
      */
    def user(user: String): Channel

    /**
      * set remote's ssh password
      * @param password ssh password
      * @return this channel
      */
    def password(password: String): Channel

    /**
      * set charset in communication
      * @param charset ssh charset
      * @return this channel
      */
    def charset(charset: Charset): Channel

    /**
      * execute the command by ssh
      * @param command command
      * @return response from remote node
      */
    def execute(command: String): Option[String]
  }
}
