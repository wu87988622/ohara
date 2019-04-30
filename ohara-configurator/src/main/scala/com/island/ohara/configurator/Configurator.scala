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

package com.island.ohara.configurator

import java.util.concurrent.{ExecutionException, TimeUnit}

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{handleRejections, path, _}
import akka.http.scaladsl.server.{ExceptionHandler, MalformedRequestContentRejection, RejectionHandler}
import akka.http.scaladsl.{Http, server}
import akka.stream.ActorMaterializer
import com.island.ohara.agent._
import com.island.ohara.agent.docker.DockerClient
import com.island.ohara.agent.k8s.K8SClient
import com.island.ohara.client.HttpExecutor
import com.island.ohara.client.configurator.ConfiguratorApiInfo
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterCreationRequest
import com.island.ohara.client.configurator.v0.NodeApi.{Node, NodeCreationRequest}
import com.island.ohara.client.configurator.v0.ValidationApi.NodeValidationRequest
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterCreationRequest
import com.island.ohara.client.configurator.v0.{ZookeeperApi, _}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{CommonUtils, Releasable, ReleaseOnce}
import com.island.ohara.configurator.jar.{JarStore, LocalJarStore}
import com.island.ohara.configurator.route._
import com.island.ohara.configurator.store.DataStore
import com.typesafe.scalalogging.Logger
import spray.json.DeserializationException

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}

/**
  * A simple impl from Configurator. This impl maintains all subclass from ohara data in a single ohara store.
  * NOTED: there are many route requiring the implicit variables so we make them be implicit in construction.
  *
  * @param advertisedHostname hostname from rest server
  * @param advertisedPort    port from rest server
  * @param store    store
  */
class Configurator private[configurator] (advertisedHostname: Option[String],
                                          advertisedPort: Option[Int],
                                          initializationTimeout: Duration,
                                          terminationTimeout: Duration,
                                          extraRoute: Option[server.Route])(implicit val store: DataStore,
                                                                            nodeCollie: NodeCollie,
                                                                            val clusterCollie: ClusterCollie,
                                                                            val crane: Crane,
                                                                            val k8sClient: Option[K8SClient])
    extends ReleaseOnce
    with SprayJsonSupport {

  private[configurator] def size: Int = store.size

  /**
    * If you don't assign a advertised hostname explicitly, local hostname will be chosen.
    * @return advertised hostname of configurator.
    */
  val hostname: String = advertisedHostname.getOrElse(CommonUtils.hostname())

  /**
    * If you don't assign a port explicitly, a random port will be chosen.
    * Pre-generating a random port can solve the initialization of components in configurator.
    * @return port bound by configurator.
    */
  val port: Int = advertisedPort.getOrElse(CommonUtils.availablePort())

  private[this] val log = Logger(classOf[Configurator])

  private[this] val jarLocalHome = CommonUtils.createTempDir("Configurator").getAbsolutePath

  private[this] implicit val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()
  private[this] implicit val workerCollie: WorkerCollie = clusterCollie.workerCollie()

  private[this] def exceptionHandler(): ExceptionHandler = ExceptionHandler {
    case e @ (_: DeserializationException | _: ParsingException | _: IllegalArgumentException |
        _: NoSuchElementException) =>
      extractRequest { request =>
        log.error(s"Request to ${request.uri} with ${request.entity} is wrong", e)
        complete(StatusCodes.BadRequest -> ErrorApi.of(e))
      }
    case e: Throwable =>
      extractRequest { request =>
        log.error(s"Request to ${request.uri} with ${request.entity} could not be handled normally", e)
        complete(StatusCodes.InternalServerError -> ErrorApi.of(e))
      }
  }

  /**
    *Akka use rejection to wrap error message
    */
  private[this] def rejectionHandler(): RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        // seek the true exception
        case MalformedRequestContentRejection(_, cause) if cause != null => throw cause
        case e: ExecutionException if e.getCause != null                 => throw e.getCause
      }
      .result()

  /**
    * the full route consists from all routes against all subclass from ohara data and a final route used to reject other requests.
    */
  private[this] def basicRoute(): server.Route = pathPrefix(ConfiguratorApiInfo.V0)(
    Seq[server.Route](
      TopicRoute.apply,
      HdfsInfoRoute.apply,
      FtpInfoRoute.apply,
      JdbcInfoRoute.apply,
      PipelineRoute.apply,
      ValidationRoute.apply,
      QueryRoute(),
      ConnectorRoute.apply,
      InfoRoute.apply,
      StreamRoute.apply,
      ShabondiRoute.apply(k8sClient),
      NodeRoute.apply,
      ZookeeperRoute.apply,
      BrokerRoute.apply,
      WorkerRoute.apply,
      JarsRoute.apply,
      LogRoute.apply,
      ObjectRoute.apply,
      ContainerRoute.apply
    ).reduce[server.Route]((a, b) => a ~ b))

  private[this] def privateRoute(): server.Route =
    pathPrefix(ConfiguratorApiInfo.PRIVATE)(extraRoute.getOrElse(path(Remaining)(path =>
      complete(StatusCodes.NotFound -> s"you have to buy the license for advanced API: $path"))))

  private[this] def finalRoute(): server.Route =
    path(Remaining)(path => complete(StatusCodes.NotFound -> s"Unsupported API: $path"))

  private[this] implicit val actorSystem: ActorSystem = ActorSystem(s"${classOf[Configurator].getSimpleName}-system")
  private[this] implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  private[this] val httpServer: Http.ServerBinding =
    try Await.result(
      Http().bindAndHandle(
        handler = handleExceptions(exceptionHandler())(
          handleRejections(rejectionHandler())(basicRoute() ~ privateRoute()) ~ finalRoute()),
        // we bind the service on all network adapter.
        interface = CommonUtils.anyLocalAddress(),
        port = port
      ),
      initializationTimeout.toMillis milliseconds
    )
    catch {
      case e: Throwable =>
        Releasable.close(this)
        throw e
    }

  /**
    * We create an internal jar store based on http server of configurator.
    */
  private[configurator] implicit val jarStore: JarStore = new LocalJarStore(
    homeFolder = jarLocalHome,
    // the download API is in JarsRoute now.
    urlPrefix = s"${ConfiguratorApiInfo.V0}/${JarApi.DOWNLOAD_JAR_PREFIX_PATH}",
    advertisedHostname = hostname,
    advertisedPort = port
  )

  /**
    * Do what you want to do when calling closing.
    */
  override protected def doClose(): Unit = {
    val start = CommonUtils.current()
    if (httpServer != null) Await.result(httpServer.unbind(), terminationTimeout.toMillis milliseconds)
    if (actorSystem != null) Await.result(actorSystem.terminate(), terminationTimeout.toMillis milliseconds)
    Releasable.close(clusterCollie)
    Releasable.close(jarStore)
    Releasable.close(store)
    log.info(s"succeed to close configurator. elapsed:${CommonUtils.current() - start} ms")
  }
}

object Configurator {
  private[configurator] val DATA_SERIALIZER: Serializer[Data] = new Serializer[Data] {
    override def to(obj: Data): Array[Byte] = Serializer.OBJECT.to(obj)
    override def from(bytes: Array[Byte]): Data =
      Serializer.OBJECT.from(bytes).asInstanceOf[Data]
  }

  def builder(): ConfiguratorBuilder = new ConfiguratorBuilder()

  //----------------[main]----------------//
  private[this] lazy val LOG = Logger(Configurator.getClass)
  private[configurator] val HELP_KEY = "--help"
  private[configurator] val HOSTNAME_KEY = "--hostname"
  private[configurator] val K8S_KEY = "--k8s"
  private[configurator] val PORT_KEY = "--port"
  private[configurator] val NODE_KEY = "--node"
  private val USAGE = s"[Usage] $HOSTNAME_KEY $PORT_KEY $K8S_KEY $NODE_KEY(form: user:password@hostname:port)"

  /**
    * Running a standalone configurator.
    * NOTED: this main is exposed to build.gradle. If you want to move the main out from this class, please update the
    * build.gradle also.
    *
    * @param args the first element is hostname and the second one is port
    */
  def main(args: Array[String]): Unit = {
    if (args.length == 1 && args(0) == HELP_KEY) {
      println(USAGE)
      return
    }

    val configuratorBuilder = Configurator.builder()
    var nodeRequest: Option[NodeCreationRequest] = None
    var k8sClient: Option[K8SClient] = None
    var k8sValue = ""
    args.sliding(2, 2).foreach {
      case Array(HOSTNAME_KEY, value) => configuratorBuilder.advertisedHostname(value)
      case Array(PORT_KEY, value)     => configuratorBuilder.advertisedPort(value.toInt)
      case Array(K8S_KEY, value) =>
        k8sClient = Option(K8SClient(value))
        configuratorBuilder.k8sClient(K8SClient(value))
        configuratorBuilder.clusterCollie(
          ClusterCollie.builderOfK8s().nodeCollie(configuratorBuilder.nodeCollie()).k8sClient(k8sClient.get).build())
        configuratorBuilder.crane(
          Crane.builderOfK8s().nodeCollie(configuratorBuilder.nodeCollie()).k8sClient(k8sClient.get).build())
        k8sValue = value
      case Array(NODE_KEY, value) =>
        val user = value.split(":").head
        val password = value.split("@").head.split(":").last
        val hostname = value.split("@").last.split(":").head
        val port = value.split("@").last.split(":").last.toInt
        nodeRequest = Some(
          NodeCreationRequest(
            name = Some(hostname),
            password = password,
            user = user,
            port = port
          ))
      case _ => throw new IllegalArgumentException(s"input:${args.mkString(" ")}. $USAGE")
    }
    val configurator = configuratorBuilder.build()

    try if (k8sValue.nonEmpty) {
      nodeRequestEach(
        nodeRequest,
        configurator,
        (node: Node) => {
          val validationResult: Seq[ValidationApi.ValidationReport] = Await.result(
            ValidationApi
              .access()
              .hostname(CommonUtils.hostname)
              .port(configurator.port)
              .verify(NodeValidationRequest(node.name, node.port, node.user, node.password)),
            30 seconds
          )
          val isValidationPass: Boolean = validationResult.map(x => x.pass).head
          if (!isValidationPass) throw new IllegalArgumentException(s"${validationResult.map(x => x.message).head}")
          checkImageExists(node, Await.result(k8sClient.get.images(node.name), 30 seconds))
        }
      )
    } else {
      nodeRequestEach(
        nodeRequest,
        configurator,
        (node: Node) => {
          val dockerClient =
            DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
          try checkImageExists(node, dockerClient.imageNames())
          finally dockerClient.close()
        }
      )

    } catch {
      case e: Throwable =>
        LOG.error("failed to initialize cluster. Will close configurator", e)
        Releasable.close(configurator)
        HttpExecutor.close()
        CollieUtils.close()
        throw e
    }
    hasRunningConfigurator = true
    try {
      LOG.info(s"start a configurator built on hostname:${configurator.hostname} and port:${configurator.port}")
      LOG.info("enter ctrl+c to terminate the configurator")
      while (!closeRunningConfigurator) {
        TimeUnit.SECONDS.sleep(2)
        LOG.info(s"Current data size:${configurator.size}")
      }
    } catch {
      case _: InterruptedException => LOG.info("prepare to die")
    } finally {
      hasRunningConfigurator = false
      Releasable.close(configurator)
      HttpExecutor.close()
      CollieUtils.close()
    }
  }

  private[this] def checkImageExists(node: Node, images: Seq[String]): Unit = {
    if (!images.contains(ZookeeperApi.IMAGE_NAME_DEFAULT))
      throw new IllegalArgumentException(s"$node doesn't have ${ZookeeperApi.IMAGE_NAME_DEFAULT}")
    if (!images.contains(BrokerApi.IMAGE_NAME_DEFAULT))
      throw new IllegalArgumentException(s"$node doesn't have ${BrokerApi.IMAGE_NAME_DEFAULT}")
    if (!images.contains(StreamApi.IMAGE_NAME_DEFAULT))
      throw new IllegalArgumentException(s"$node doesn't have ${StreamApi.IMAGE_NAME_DEFAULT}")
  }

  private[this] def nodeRequestEach(nodeRequest: Option[NodeCreationRequest],
                                    configurator: Configurator,
                                    otherCheck: Node => Unit): Unit = {
    nodeRequest.foreach { req =>
      LOG.info(s"Find a pre-created node:$req. Will create zookeeper and broker!!")

      val node =
        Await.result(NodeApi.access().hostname(CommonUtils.hostname()).port(configurator.port).add(req), 30 seconds)
      otherCheck(node)

      val zkCluster = Await.result(
        ZookeeperApi
          .access()
          .hostname(CommonUtils.hostname())
          .port(configurator.port)
          .add(
            ZookeeperClusterCreationRequest(name = PRE_CREATE_ZK_NAME,
                                            imageName = None,
                                            clientPort = None,
                                            electionPort = None,
                                            peerPort = None,
                                            nodeNames = Seq(node.name))),
        30 seconds
      )
      LOG.info(s"succeed to create zk cluster:$zkCluster")

      val bkCluster = Await.result(
        BrokerApi
          .access()
          .hostname(CommonUtils.hostname())
          .port(configurator.port)
          .add(BrokerClusterCreationRequest(
            name = PRE_CREATE_BK_NAME,
            imageName = None,
            zookeeperClusterName = Some(PRE_CREATE_ZK_NAME),
            exporterPort = None,
            clientPort = None,
            jmxPort = None,
            nodeNames = Seq(node.name)
          )),
        30 seconds
      )
      LOG.info(s"succeed to create bk cluster:$bkCluster")
    }
  }

  /**
    * Add --node argument to pre create zookeeper cluster name
    */
  private[configurator] val PRE_CREATE_ZK_NAME: String = "precreatezkcluster"

  /**
    * Add --node argument to pre create broker cluster name
    */
  private[configurator] val PRE_CREATE_BK_NAME: String = "precreatebkcluster"

  /**
    * visible for testing.
    */
  @volatile private[configurator] var hasRunningConfigurator = false

  /**
    * visible for testing.
    */
  @volatile private[configurator] var closeRunningConfigurator = false
}
