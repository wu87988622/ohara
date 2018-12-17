package com.island.ohara.configurator

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{handleRejections, _}
import akka.http.scaladsl.server.{ExceptionHandler, MalformedRequestContentRejection, RejectionHandler}
import akka.http.scaladsl.{Http, server}
import akka.stream.ActorMaterializer
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.ConnectorClient
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{ReleaseOnce, CommonUtil}
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route._
import com.island.ohara.configurator.store.Consistency
import com.island.ohara.kafka.KafkaClient
import com.typesafe.scalalogging.Logger
import spray.json.{DeserializationException, JsonParser}

import scala.concurrent.{Await, Awaitable}
import scala.concurrent.duration.{Duration, _}
import scala.reflect.{ClassTag, classTag}

/**
  * A simple impl from Configurator. This impl maintains all subclass from ohara data in a single ohara store.
  * NOTED: there are many route requiring the implicit variables so we make them be implicit in construction.
  *
  * @param configuredHostname hostname from rest server
  * @param configuredPort    port from rest server
  * @param store    store
  */
class Configurator private[configurator] (configuredHostname: String,
                                          configuredPort: Int,
                                          initializationTimeout: Duration,
                                          terminationTimeout: Duration,
                                          extraRoute: Option[server.Route])(implicit ug: () => String,
                                                                            val store: Store,
                                                                            kafkaClient: KafkaClient,
                                                                            connectorClient: ConnectorClient)
    extends ReleaseOnce
    with SprayJsonSupport {

  private val log = Logger(classOf[Configurator])

  private[this] val exceptionHandler = ExceptionHandler {
    case e: IllegalArgumentException =>
      extractRequest { request =>
        log.error(
          s"Request to ${request.uri} with ${request.entity} could not be handled normally because ${e.getMessage}")
        complete(StatusCodes.BadRequest -> Error(e))
      }
    case e: Throwable =>
      log.error("What happens here?", e)
      complete(StatusCodes.ServiceUnavailable -> Error(e))
  }

  /**
    * the full route consists from all routes against all subclass from ohara data and a final route used to reject other requests.
    */
  private[this] val basicRoute: server.Route = pathPrefix(VERSION_V0)(
    Seq[server.Route](
      TopicInfoRoute.apply,
      HdfsInformationRoute.apply,
      FtpInformationRoute.apply,
      JdbcInformationRoute.apply,
      PipelineRoute.apply,
      ValidationRoute.apply,
      QueryRoute(),
      SourceRoute.apply,
      SinkRoute.apply,
      ClusterRoute.apply
    ).reduce[server.Route]((a, b) => a ~ b))

  private[this] val privateRoute: server.Route = pathPrefix(PRIVATE_API)(extraRoute.getOrElse(path(Remaining)(path =>
    complete(StatusCodes.NotFound -> s"you have to buy the license for advanced API: $path"))))

  private[this] val finalRoute: server.Route =
    path(Remaining)(path => complete(StatusCodes.NotFound -> s"Unsupported API: $path"))

  private[this] implicit val actorSystem: ActorSystem = ActorSystem(s"${classOf[Configurator].getSimpleName}-system")
  private[this] implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  private[this] val httpServer: Http.ServerBinding =
    Await.result(
      Http().bindAndHandle(
        handleExceptions(exceptionHandler)(handleRejections(rejectionHandler)(basicRoute ~ privateRoute) ~ finalRoute),
        configuredHostname,
        configuredPort
      ),
      initializationTimeout.toMillis milliseconds
    )

  /**
    *Akka use rejection to wrap error message
    */
  private[this] val rejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case MalformedRequestContentRejection(_, cause) =>
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
        case otherRejection => { throw new IllegalArgumentException(s"Ohata Error occur : ${otherRejection}") }
      }
      .result()

  /**
    * Do what you want to do when calling closing.
    */
  override protected def doClose(): Unit = {
    if (httpServer != null) Await.result(httpServer.unbind(), terminationTimeout.toMillis milliseconds)
    if (actorSystem != null) Await.result(actorSystem.terminate(), terminationTimeout.toMillis milliseconds)
    ReleaseOnce.close(store)
    ReleaseOnce.close(kafkaClient)
    ReleaseOnce.close(connectorClient)
  }

  //-----------------[public interfaces]-----------------//

  val hostname: String = httpServer.localAddress.getHostName

  val port: Int = httpServer.localAddress.getPort

  val connectionProps: String = s"$hostname:$port"

  def size: Int = store.size
}

object Configurator {
  def builder(): ConfiguratorBuilder = new ConfiguratorBuilder()

  //----------------[main]----------------//
  private[this] lazy val LOG = Logger(Configurator.getClass)
  private[configurator] val HELP_KEY = "--help"
  private[configurator] val HOSTNAME_KEY = "--hostname"
  private[configurator] val PORT_KEY = "--port"
  private[configurator] val BROKERS_KEY = "--brokers"
  private[configurator] val WORKERS_KEY = "--workers"
  private[configurator] val TOPIC_KEY = "--topic"
  private[configurator] val PARTITIONS_KEY = "--partitions"
  private[configurator] val REPLICATIONS_KEY = "--replications"
  private val USAGE = s"[Usage] $HOSTNAME_KEY $PORT_KEY $BROKERS_KEY $TOPIC_KEY $PARTITIONS_KEY $REPLICATIONS_KEY"

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
    var brokers: Option[String] = None
    var workers: Option[String] = None
    var topicName = "test-topic"
    var numberOfPartitions: Int = 1
    var numberOfReplications: Short = 1
    args.sliding(2, 2).foreach {
      case Array(HOSTNAME_KEY, value)     => hostname = value
      case Array(PORT_KEY, value)         => port = value.toInt
      case Array(BROKERS_KEY, value)      => if (value.toLowerCase != "none") brokers = Some(value)
      case Array(WORKERS_KEY, value)      => if (value.toLowerCase != "none") workers = Some(value)
      case Array(TOPIC_KEY, value)        => topicName = value
      case Array(PARTITIONS_KEY, value)   => numberOfPartitions = value.toInt
      case Array(REPLICATIONS_KEY, value) => numberOfReplications = value.toShort
      case _                              => throw new IllegalArgumentException(USAGE)
    }
    var standalone = false

    val configurator =
      if (brokers.isEmpty && workers.isEmpty) {
        standalone = true
        Configurator.builder().noCluster.hostname(hostname).port(port).build()
      } else if (brokers.isEmpty ^ workers.isEmpty)
        throw new IllegalArgumentException(s"brokers:$brokers workers:$workers")
      else
        Configurator
          .builder()
          .store(
            com.island.ohara.configurator.store.Store
              .builder()
              .brokers(brokers.get)
              .topicName(topicName)
              .build(Serializer.STRING, Serializer.OBJECT))
          .kafkaClient(KafkaClient.of(brokers.get))
          .connectClient(ConnectorClient(workers.get))
          .hostname(hostname)
          .port(port)
          .build()
    hasRunningConfigurator = true
    try {
      LOG.info(
        s"start a ${if (standalone) "standalone" else "truly"} configurator built on hostname:${configurator.hostname} and port:${configurator.port}")
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

  private[configurator] class Store(store: com.island.ohara.configurator.store.Store[String, AnyRef])
      extends ReleaseOnce {
    private[this] val timeout = 30 seconds
    private[this] val consistency = Consistency.STRICT

    private[this] def result[T](awaitable: Awaitable[T]): T = Await.result(awaitable, timeout)

    /**
      * Remove a "specified" sublcass from ohara data mapping the uuid. If the data mapping to the uuid is not the specified
      * type, an exception will be thrown.
      *
      * @param uuid from ohara data
      * @tparam T subclass type
      * @return the removed data
      */
    def remove[T <: Data: ClassTag](uuid: String): T = result(store.get(uuid))
      .filter(classTag[T].runtimeClass.isInstance)
      .flatMap(_ => result(store.remove(uuid, consistency)))
      .getOrElse(throw new IllegalArgumentException(s"Failed to remove $uuid since it doesn't exist"))
      .asInstanceOf[T]

    /**
      * update an existed object in the store. If the uuid doesn't  exists, an exception will be thrown.
      *
      * @param data data
      * @tparam T type from data
      * @return the removed data
      */
    def update[T <: Data: ClassTag](data: T): T =
      if (result(store.get(data.uuid)).exists(classTag[T].runtimeClass.isInstance))
        result(store.update(data.uuid, data, consistency)).get.asInstanceOf[T]
      else throw new IllegalArgumentException(s"Failed to update ${data.uuid} since it doesn't exist")

    /**
      * add an new object to the store. If the uuid already exists, an exception will be thrown.
      *
      * @param data data
      * @tparam T type from data
      */
    def add[T <: Data: ClassTag](data: T): Unit =
      if (result(store.get(data.uuid)).exists(classTag[T].runtimeClass.isInstance))
        throw new IllegalArgumentException(s"The object:${data.uuid} exists")
      else result(store.update(data.uuid, data, consistency))

    /**
      * Iterate the specified type. The unrelated type will be ignored.
      *
      * @tparam T subclass type
      * @return iterator
      */
    def data[T <: Data: ClassTag]: Iterator[T] =
      store.map(_._2).iterator.filter(classTag[T].runtimeClass.isInstance).map(_.asInstanceOf[T])

    /**
      * Retrieve a "specified" subclass from ohara data mapping the uuid. If the data mapping to the uuid is not the specified
      * type, the None will be returned.
      *
      * @param uuid from ohara data
      * @tparam T subclass type
      * @return a subclass from ohara data
      */
    def data[T <: Data: ClassTag](uuid: String): T =
      result(store.get(uuid))
        .filter(classTag[T].runtimeClass.isInstance)
        .getOrElse(throw new IllegalArgumentException(
          s"Failed to find ${classTag[T].runtimeClass.getSimpleName} by $uuid since it doesn't exist"))
        .asInstanceOf[T]

    def exist[T <: Data: ClassTag](uuid: String): Boolean =
      result(store.get(uuid)).exists(classTag[T].runtimeClass.isInstance)

    def nonExist[T <: Data: ClassTag](uuid: String): Boolean = !exist[T](uuid)

    def raw(): Iterator[Data] = store.iterator.map(_._2).filter(_.isInstanceOf[Data]).map(_.asInstanceOf[Data])

    def raw(uuid: String): Data = result(store.get(uuid))
      .filter(_.isInstanceOf[Data])
      .map(_.asInstanceOf[Data])
      .getOrElse(throw new IllegalArgumentException(s"Failed to find raw data by $uuid since it doesn't exist"))

    def size: Int = store.size

    override protected def doClose(): Unit = store.close()
  }
}
