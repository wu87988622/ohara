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
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{handleRejections, path, _}
import akka.http.scaladsl.server.{ExceptionHandler, MalformedRequestContentRejection, RejectionHandler}
import akka.http.scaladsl.{Http, server}
import akka.stream.ActorMaterializer
import com.island.ohara.agent._
import com.island.ohara.client.configurator.ConfiguratorApiInfo
import com.island.ohara.client.configurator.v0.{Data, ErrorApi}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{CommonUtil, Releasable, ReleaseOnce}
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.jar.{JarStore, LocalJarStore}
import com.island.ohara.configurator.route._
import com.typesafe.scalalogging.Logger
import spray.json.{DeserializationException, JsonParser}

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
  extraRoute: Option[server.Route])(implicit store: Store, nodeCollie: NodeCollie, clusterCollie: ClusterCollie)
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

  private[this] implicit val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeepersCollie()
  private[this] implicit val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()
  private[this] implicit val workerCollie: WorkerCollie = clusterCollie.workerCollie()

  private[this] def exceptionHandler(): ExceptionHandler = ExceptionHandler {
    case e: IllegalArgumentException =>
      extractRequest { request =>
        log.error(s"Request to ${request.uri} with ${request.entity} could not be handled normally", e)
        complete(StatusCodes.BadRequest -> ErrorApi.of(e))
      }
    case e: Throwable =>
      log.error("What happens here?", e)
      complete(StatusCodes.ServiceUnavailable -> ErrorApi.of(e))
  }

  /**
    *Akka use rejection to wrap error message
    */
  private[this] def rejectionHandler(): RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case MalformedRequestContentRejection(_, cause) =>
          // TODO: ohara needs exception hierarchy ... by chia
          cause match {
            case e: DeserializationException =>
              throw new IllegalArgumentException(s"Deserialized Error :${e.getMessage}", e)
            case e: JsonParser.ParsingException =>
              throw new IllegalArgumentException(s"JSON Parse Error :${e.getMessage}", e)
            case e: Throwable =>
              throw new IllegalArgumentException(s"Error :${e.getMessage}.")
          }
      }
      //which handles undefined Rejections to Exceptions , like ValidationRejection(java.lang.NumberFormatException)
      .handle {
        case otherRejection => throw new IllegalArgumentException(s"Ohata Error occur : $otherRejection")
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
      NodesRoute.apply,
      ZookeeperRoute.apply,
      BrokerRoute.apply,
      WorkerRoute.apply,
      JarsRoute.apply
    ).reduce[server.Route]((a, b) => a ~ b))

  private[this] def privateRoute(): server.Route =
    pathPrefix(ConfiguratorApiInfo.PRIVATE)(extraRoute.getOrElse(path(Remaining)(path =>
      complete(StatusCodes.NotFound -> s"you have to buy the license for advanced API: $path"))))

  private[this] def finalRoute(): server.Route =
    path(Remaining)(path => complete(StatusCodes.NotFound -> s"Unsupported API: $path"))

  private[this] implicit val actorSystem: ActorSystem = ActorSystem(s"${classOf[Configurator].getSimpleName}-system")
  private[this] implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  private[this] val httpServer: Http.ServerBinding =
    Await.result(
      Http().bindAndHandle(
        handleExceptions(exceptionHandler())(
          handleRejections(rejectionHandler())(basicRoute() ~ privateRoute() ~ jarDownloadRoute()) ~ finalRoute()),
        // we bind the service on all network adapter.
        CommonUtil.anyLocalAddress(),
        port
      ),
      initializationTimeout.toMillis milliseconds
    )

  /**
    * Do what you want to do when calling closing.
    */
  override protected def doClose(): Unit = {
    if (httpServer != null) Await.result(httpServer.unbind(), terminationTimeout.toMillis milliseconds)
    if (actorSystem != null) Await.result(actorSystem.terminate(), terminationTimeout.toMillis milliseconds)
    Releasable.close(clusterCollie)
    Releasable.close(jarStore)
    Releasable.close(store)
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
  private val USAGE = s"[Usage] $HOSTNAME_KEY $PORT_KEY"

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
    args.sliding(2, 2).foreach {
      case Array(HOSTNAME_KEY, value) => hostname = value
      case Array(PORT_KEY, value)     => port = value.toInt
      case _                          => throw new IllegalArgumentException(USAGE)
    }
    val configurator = Configurator.builder().hostname(hostname).port(port).build()
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
      configurator.close()
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
