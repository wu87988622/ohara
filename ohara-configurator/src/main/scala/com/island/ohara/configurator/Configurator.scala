package com.island.ohara.configurator

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.{Http, server}
import akka.stream.ActorMaterializer
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.ConnectorClient
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route._
import com.island.ohara.configurator.store.Consistency
import com.island.ohara.io.{CloseOnce, UuidUtil}
import com.island.ohara.kafka.KafkaClient
import com.typesafe.scalalogging.Logger

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}
import scala.reflect.{ClassTag, classTag}

/**
  * A simple impl of Configurator. This impl maintains all subclass of ohara data in a single ohara store.
  * NOTED: there are many route requiring the implicit variables so we make them be implicit in construction.
  *
  * @param configuredHostname hostname of rest server
  * @param configuredPort    port of rest server
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
    extends CloseOnce
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
    * the full route consists of all routes against all subclass of ohara data and a final route used to reject other requests.
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
    throw new IllegalArgumentException(s"you have to buy the license for advanced APIs of $path"))))

  private[this] val finalRoute: server.Route = path(Remaining)(path =>
    throw new IllegalArgumentException(s"Unsupported restful api:$path. Or the request is invalid to the $path"))

  private[this] implicit val actorSystem: ActorSystem = ActorSystem(s"${classOf[Configurator].getSimpleName}-system")
  private[this] implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  private[this] val httpServer: Http.ServerBinding =
    Await.result(
      Http().bindAndHandle(
        handleExceptions(exceptionHandler)(basicRoute ~ privateRoute ~ finalRoute),
        configuredHostname,
        configuredPort
      ),
      initializationTimeout.toMillis milliseconds
    )

  /**
    * Do what you want to do when calling closing.
    */
  override protected def doClose(): Unit = {
    if (httpServer != null)
      CloseOnce.release(() => Await.result(httpServer.unbind(), terminationTimeout.toMillis milliseconds))
    if (actorSystem != null)
      CloseOnce.release(() => Await.result(actorSystem.terminate(), terminationTimeout.toMillis milliseconds))
    CloseOnce.close(store)
    CloseOnce.close(kafkaClient)
    CloseOnce.close(connectorClient)
  }

  //-----------------[public interfaces]-----------------//

  val hostname: String = httpServer.localAddress.getHostName

  val port: Int = httpServer.localAddress.getPort

  def size: Int = store.size
}

object Configurator {
  def builder() = new ConfiguratorBuilder()

  val DEFAULT_UUID_GENERATOR: () => String = () => UuidUtil.uuid()
  val DEFAULT_INITIALIZATION_TIMEOUT: Duration = 10 seconds
  val DEFAULT_TERMINATION_TIMEOUT: Duration = 10 seconds

  //----------------[main]----------------//
  private[this] lazy val LOG = Logger(Configurator.getClass)
  val HELP_KEY = "--help"
  val HOSTNAME_KEY = "--hostname"
  val PORT_KEY = "--port"
  val BROKERS_KEY = "--brokers"
  val WORKERS_KEY = "--workers"
  val TOPIC_KEY = "--topic"
  val PARTITIONS_KEY = "--partitions"
  val REPLICATIONS_KEY = "--replications"
  val USAGE = s"[Usage] $HOSTNAME_KEY $PORT_KEY $BROKERS_KEY $TOPIC_KEY $PARTITIONS_KEY $REPLICATIONS_KEY"

  /**
    * Running a standalone configurator.
    * NOTED: this main is exposed to build.gradle. If you want to move the main out of this class, please update the
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
    var hostname = "0.0.0.0"
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
      if (brokers.isEmpty || workers.isEmpty) {
        standalone = true
        Configurator.builder().noCluster.hostname(hostname).port(port).build()
      } else {
        val kafkaClient = KafkaClient(brokers.get)
        kafkaClient
          .topicCreator()
          .numberOfReplications(numberOfReplications)
          .numberOfPartitions(numberOfPartitions)
          .compacted()
          .create(topicName)
        Configurator
          .builder()
          .store(
            com.island.ohara.configurator.store.Store
              .builder()
              .brokers(brokers.get)
              .topicName(topicName)
              .buildBlocking[String, Any])
          .kafkaClient(kafkaClient)
          .connectClient(ConnectorClient(workers.get))
          .hostname(hostname)
          .port(port)
          .build()
      }
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

  private[configurator] class Store(store: com.island.ohara.configurator.store.BlockingStore[String, Any])
      extends CloseOnce {
    private[this] val consistency = Consistency.STRICT

    /**
      * Remove a "specified" sublcass of ohara data mapping the uuid. If the data mapping to the uuid is not the specified
      * type, an exception will be thrown.
      *
      * @param uuid of ohara data
      * @tparam T subclass type
      * @return the removed data
      */
    def remove[T <: Data: ClassTag](uuid: String): T = store
      ._get(uuid)
      .filter(classTag[T].runtimeClass.isInstance(_))
      .flatMap(_ => store._remove(uuid, consistency))
      .getOrElse(throw new IllegalArgumentException(s"Failed to remove $uuid since it doesn't exist"))
      .asInstanceOf[T]

    /**
      * update an existed object in the store. If the uuid doesn't  exists, an exception will be thrown.
      *
      * @param uuid uuid
      * @param data data
      * @tparam T type of data
      * @return the removed data
      */
    def update[T <: Data: ClassTag](uuid: String, data: T): T =
      if (store._get(uuid).exists(classTag[T].runtimeClass.isInstance(_)))
        store._update(uuid, data, consistency).get.asInstanceOf[T]
      else throw new IllegalArgumentException(s"Failed to update $uuid since it doesn't exist")

    /**
      * add an new object to the store. If the uuid already exists, an exception will be thrown.
      *
      * @param uuid uuid
      * @param data data
      * @tparam T type of data
      */
    def add[T <: Data: ClassTag](uuid: String, data: T): Unit =
      if (store._get(uuid).exists(classTag[T].runtimeClass.isInstance(_)))
        throw new IllegalArgumentException(s"The object:$uuid exists")
      else store._update(uuid, data, consistency)

    /**
      * Iterate the specified type. The unrelated type will be ignored.
      *
      * @tparam T subclass type
      * @return iterator
      */
    def data[T <: Data: ClassTag]: Iterator[T] =
      store.map(_._2).iterator.filter(classTag[T].runtimeClass.isInstance).map(_.asInstanceOf[T])

    /**
      * Retrieve a "specified" sublcass of ohara data mapping the uuid. If the data mapping to the uuid is not the specified
      * type, the None will be returned.
      *
      * @param uuid of ohara data
      * @tparam T subclass type
      * @return a subclass of ohara data
      */
    def data[T <: Data: ClassTag](uuid: String): T =
      store
        ._get(uuid)
        .filter(classTag[T].runtimeClass.isInstance(_))
        .getOrElse(throw new IllegalArgumentException(
          s"Failed to find ${classTag[T].runtimeClass.getSimpleName} by $uuid since it doesn't exist"))
        .asInstanceOf[T]

    def raw(): Iterator[Data] = store.iterator.map(_._2).filter(_.isInstanceOf[Data]).map(_.asInstanceOf[Data])

    def raw(uuid: String): Data = store
      ._get(uuid)
      .filter(_.isInstanceOf[Data])
      .map(_.asInstanceOf[Data])
      .getOrElse(throw new IllegalArgumentException(s"Failed to find raw data by $uuid since it doesn't exist"))

    def size: Int = store.size

    override protected def doClose(): Unit = store.close()

    def exist(uuid: String): Boolean = store._exist(uuid)
  }

}
