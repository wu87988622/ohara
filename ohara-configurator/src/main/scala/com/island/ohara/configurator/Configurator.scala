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

import java.io.File
import java.net.URL
import java.util.concurrent.{ExecutionException, TimeUnit}

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{handleRejections, path, _}
import akka.http.scaladsl.server.{ExceptionHandler, MalformedRequestContentRejection, RejectionHandler}
import akka.http.scaladsl.{Http, server}
import akka.stream.ActorMaterializer
import com.island.ohara.agent._
import com.island.ohara.client.HttpExecutor
import com.island.ohara.client.configurator.ConfiguratorApiInfo
import com.island.ohara.client.configurator.v0.BrokerApi.BrokerClusterCreationRequest
import com.island.ohara.client.configurator.v0.NodeApi.NodeCreationRequest
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterCreationRequest
import com.island.ohara.client.configurator.v0._
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{CommonUtil, Releasable, ReleaseOnce}
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.jar.{JarStore, LocalJarStore}
import com.island.ohara.configurator.route._
import com.typesafe.scalalogging.Logger
import spray.json.DeserializationException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.reflect.{ClassTag, classTag}

/**
  * A simple impl from Configurator. This impl maintains all subclass from ohara data in a single ohara store.
  * NOTED: there are many route requiring the implicit variables so we make them be implicit in construction.
  *
  * @param advertisedHostname hostname from rest server
  * @param advertisedPort    port from rest server
  * @param store    store
  */
class Configurator private[configurator] (
  advertisedHostname: Option[String],
  advertisedPort: Option[Int],
  initializationTimeout: Duration,
  terminationTimeout: Duration,
  extraRoute: Option[server.Route])(implicit val store: Store, nodeCollie: NodeCollie, val clusterCollie: ClusterCollie)
    extends ReleaseOnce
    with SprayJsonSupport {

  //-----------------[public interfaces]-----------------//

  val hostname: String = advertisedHostname.getOrElse(CommonUtil.hostname())

  /**
    * we assign the port manually since we have to create a jar store in order to create related routes. And then
    * the http server can be created by the routes. Hence, we can't get free port through http server.
    * TODO: this won't hurt the production but it may unstabilize our tests because of port conflict...by chia
    */
  val port: Int = advertisedPort.map(CommonUtil.resolvePort).getOrElse(CommonUtil.availablePort())

  def size: Int = store.size

  private[this] val log = Logger(classOf[Configurator])

  private[this] val jarLocalHome = CommonUtil.createTempDir("Configurator").getAbsolutePath
  private[this] val jarDownloadPath = "jar"

  /**
    * We create an internal jar store based on http server of configurator.
    */
  implicit val jarStore: JarStore = new LocalJarStore(jarLocalHome) {
    log.info(s"path of jar:$jarLocalHome")
    override protected def doUrls(): Future[Map[String, URL]] = jarInfos().map(_.map { plugin =>
      plugin.id -> new URL(s"http://${CommonUtil.address(hostname)}:$port/$jarDownloadPath/${plugin.id}.jar")
    }.toMap)

    override protected def doClose(): Unit = {
      // do nothing
    }
  }

  /**
    * the route is exposed to worker cluster. They will download all assigned jars to start worker process.
    * TODO: should we integrate this route to our public API?? by chia
    */
  private[this] def jarDownloadRoute(): server.Route = path(jarDownloadPath / Segment) { idWithExtension =>
    // We force all url end with .jar
    if (!idWithExtension.endsWith(".jar")) complete(StatusCodes.NotFound -> s"$idWithExtension doesn't exist")
    else {
      val id = idWithExtension.substring(0, idWithExtension.indexOf(".jar"))
      val jarFolder = new File(jarLocalHome, id)
      if (!jarFolder.exists() || !jarFolder.isDirectory) complete(StatusCodes.NotFound -> s"$id doesn't exist")
      else {
        val jars = jarFolder.listFiles()
        if (jars == null || jars.isEmpty) complete(StatusCodes.NotFound)
        else if (jars.size != 1) complete(StatusCodes.InternalServerError -> s"duplicate jars for $id")
        else getFromFile(jars.head)
      }
    }
  }

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
      NodeRoute.apply,
      ZookeeperRoute.apply,
      BrokerRoute.apply,
      WorkerRoute.apply,
      JarsRoute.apply,
      LogRoute.apply,
      ObjectRoute.apply
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
        handleExceptions(exceptionHandler())(
          handleRejections(rejectionHandler())(basicRoute() ~ privateRoute() ~ jarDownloadRoute()) ~ finalRoute()),
        // we bind the service on all network adapter.
        CommonUtil.anyLocalAddress(),
        port
      ),
      initializationTimeout.toMillis milliseconds
    )
    catch {
      case e: Throwable =>
        Releasable.close(this)
        throw e
    }

  /**
    * Do what you want to do when calling closing.
    */
  override protected def doClose(): Unit = {
    val start = CommonUtil.current()
    if (httpServer != null) Await.result(httpServer.unbind(), terminationTimeout.toMillis milliseconds)
    if (actorSystem != null) Await.result(actorSystem.terminate(), terminationTimeout.toMillis milliseconds)
    Releasable.close(clusterCollie)
    Releasable.close(jarStore)
    Releasable.close(store)
    log.info(s"succeed to close configurator. elapsed:${CommonUtil.current() - start} ms")
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
  private[configurator] val PORT_KEY = "--port"
  private[configurator] val NODE_KEY = "--node"
  private val USAGE = s"[Usage] $HOSTNAME_KEY $PORT_KEY $NODE_KEY(form: user:password@hostname:port)"

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
    // TODO: make the parse more friendly
    var hostname = CommonUtil.anyLocalAddress
    var port: Int = 0
    var nodeRequest: Option[NodeCreationRequest] = None
    args.sliding(2, 2).foreach {
      case Array(HOSTNAME_KEY, value) => hostname = value
      case Array(PORT_KEY, value)     => port = value.toInt
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
    val configurator = Configurator.builder().advertisedHostname(hostname).advertisedPort(port).build()
    try nodeRequest.foreach { req =>
      LOG.info(s"Find a pre-created node:$req. Will create zookeeper and broker!!")
      import scala.concurrent.duration._
      val node =
        Await.result(NodeApi.access().hostname(configurator.hostname).port(configurator.port).add(req), 30 seconds)
      val dockerClient =
        DockerClient.builder().hostname(node.name).port(node.port).user(node.user).password(node.password).build()
      try {
        val images = dockerClient.imageNames()
        if (!images.contains(ZookeeperApi.IMAGE_NAME_DEFAULT))
          throw new IllegalArgumentException(s"$node doesn't have ${ZookeeperApi.IMAGE_NAME_DEFAULT}")
        if (!images.contains(BrokerApi.IMAGE_NAME_DEFAULT))
          throw new IllegalArgumentException(s"$node doesn't have ${BrokerApi.IMAGE_NAME_DEFAULT}")
      } finally dockerClient.close()
      val zkCluster = Await.result(
        ZookeeperApi
          .access()
          .hostname(configurator.hostname)
          .port(configurator.port)
          .add(
            ZookeeperClusterCreationRequest(name = "preCreatedZkCluster",
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
          .hostname(configurator.hostname)
          .port(configurator.port)
          .add(
            BrokerClusterCreationRequest(name = "preCreatedBkCluster",
                                         imageName = None,
                                         zookeeperClusterName = Some(zkCluster.name),
                                         exporterPort = None,
                                         clientPort = None,
                                         nodeNames = Seq(node.name))),
        30 seconds
      )
      LOG.info(s"succeed to create bk cluster:$bkCluster")
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

  /**
    * visible for testing.
    */
  @volatile private[configurator] var hasRunningConfigurator = false

  /**
    * visible for testing.
    */
  @volatile private[configurator] var closeRunningConfigurator = false

  private[configurator] class Store(store: com.island.ohara.configurator.store.Store[String, Data])
      extends ReleaseOnce {

    def value[T <: Data: ClassTag](id: String): Future[T] =
      store.value(id).filter(classTag[T].runtimeClass.isInstance).map(_.asInstanceOf[T])

    def values[T <: Data: ClassTag]: Future[Seq[T]] =
      store.values().map(_.values.filter(classTag[T].runtimeClass.isInstance).map(_.asInstanceOf[T]).toSeq)

    def raw(): Future[Seq[Data]] = store.values().map(_.values.toSeq)

    def raw(id: String): Future[Data] = store.value(id)

    /**
      * Remove a "specified" sublcass from ohara data mapping the id. If the data mapping to the id is not the specified
      * type, an exception will be thrown.
      *
      * @param id from ohara data
      * @tparam T subclass type
      * @return the removed data
      */
    def remove[T <: Data: ClassTag](id: String): Future[T] =
      value[T](id).flatMap(_ => store.remove(id)).map(_.asInstanceOf[T])

    /**
      * update an existed object in the store. If the id doesn't  exists, an exception will be thrown.
      *
      * @param data data
      * @tparam T type from data
      * @return the removed data
      */
    def update[T <: Data: ClassTag](id: String, data: T => Future[T]): Future[T] =
      value[T](id).flatMap(_ => store.update(id, v => data(v.asInstanceOf[T]))).map(_.asInstanceOf[T])

    def add[T <: Data](data: T): Future[T] = store.add(data.id, data).map(_.asInstanceOf[T])

    def exist[T <: Data: ClassTag](id: String): Future[Boolean] =
      store.value(id).map(classTag[T].runtimeClass.isInstance)

    def nonExist[T <: Data: ClassTag](id: String): Future[Boolean] = exist[T](id).map(!_)

    def size: Int = store.size

    override protected def doClose(): Unit = store.close()
  }
}
