package com.island.ohara.configurator

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{handleRejections, _}
import akka.http.scaladsl.server.{ExceptionHandler, MalformedRequestContentRejection, RejectionHandler}
import akka.http.scaladsl.{Http, server}
import akka.stream.ActorMaterializer
import com.island.ohara.agent._
import com.island.ohara.agent.jar.JarStore
import com.island.ohara.client.ConnectorClient
import com.island.ohara.client.configurator.ConfiguratorApiInfo
import com.island.ohara.client.configurator.v0.{Data, ErrorApi}
import com.island.ohara.common.data.Serializer
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.island.ohara.configurator.Configurator.Store
import com.island.ohara.configurator.route._
import com.island.ohara.kafka.KafkaClient
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
  * @param configuredHostname hostname from rest server
  * @param configuredPort    port from rest server
  * @param store    store
  */
class Configurator private[configurator] (configuredHostname: String,
                                          configuredPort: Int,
                                          initializationTimeout: Duration,
                                          terminationTimeout: Duration,
                                          extraRoute: Option[server.Route])(implicit store: Store,
                                                                            nodeCollie: NodeCollie,
                                                                            clusterCollie: ClusterCollie,
                                                                            kafkaClient: KafkaClient,
                                                                            connectorClient: ConnectorClient,
                                                                            jarStore: JarStore)
    extends ReleaseOnce
    with SprayJsonSupport {

  private val log = Logger(classOf[Configurator])

  private[this] implicit val zookeeperCollie: ZookeeperCollie = clusterCollie.zookeepersCollie()
  private[this] implicit val brokerCollie: BrokerCollie = clusterCollie.brokerCollie()
  private[this] implicit val workerCollie: WorkerCollie = clusterCollie.workerCollie()

  private[this] def exceptionHandler(): ExceptionHandler = ExceptionHandler {
    case e: IllegalArgumentException =>
      extractRequest { request =>
        log.error(
          s"Request to ${request.uri} with ${request.entity} could not be handled normally because ${e.getMessage}")
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
      TopicsRoute.apply,
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
          handleRejections(rejectionHandler())(basicRoute() ~ privateRoute()) ~ finalRoute()),
        configuredHostname,
        configuredPort
      ),
      initializationTimeout.toMillis milliseconds
    )

  /**
    * Do what you want to do when calling closing.
    */
  override protected def doClose(): Unit = {
    if (httpServer != null) Await.result(httpServer.unbind(), terminationTimeout.toMillis milliseconds)
    if (actorSystem != null) Await.result(actorSystem.terminate(), terminationTimeout.toMillis milliseconds)
    ReleaseOnce.close(kafkaClient)
    ReleaseOnce.close(connectorClient)
    ReleaseOnce.close(clusterCollie)
    ReleaseOnce.close(jarStore)
    ReleaseOnce.close(store)
  }

  //-----------------[public interfaces]-----------------//

  val hostname: String = httpServer.localAddress.getHostName

  val port: Int = httpServer.localAddress.getPort

  val connectionProps: String = s"$hostname:$port"

  def size: Int = store.size
}

object Configurator {
  private[configurator] val DATA_SERIALIZER: Serializer[Data] = new Serializer[Data] {
    override def to(obj: Data): Array[Byte] = Serializer.OBJECT.to(obj)
    override def from(bytes: Array[Byte]): Data =
      Serializer.OBJECT.from(bytes).asInstanceOf[Data]
  }

  /**
    * set all client to fake mode. It means all request sent to configurator won't be executed. a testing-purpose method.
    * @return a configurator with all fake clients
    */
  def fake(): Configurator = builder().fake().port(0).hostname(CommonUtil.hostname()).build()

  def builder(): ConfiguratorBuilder = new ConfiguratorBuilder()

  //----------------[main]----------------//
  private[this] lazy val LOG = Logger(Configurator.getClass)
  private[configurator] val HELP_KEY = "--help"
  private[configurator] val HOSTNAME_KEY = "--hostname"
  private[configurator] val PORT_KEY = "--port"
  private[configurator] val BROKERS_KEY = "--brokers"
  private[configurator] val WORKERS_KEY = "--workers"
  private[configurator] val PARTITIONS_KEY = "--partitions"
  private[configurator] val REPLICATIONS_KEY = "--replications"
  private val USAGE = s"[Usage] $HOSTNAME_KEY $PORT_KEY $BROKERS_KEY $PARTITIONS_KEY $REPLICATIONS_KEY"

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
    var numberOfPartitions: Int = 1
    var numberOfReplications: Short = 1
    args.sliding(2, 2).foreach {
      case Array(HOSTNAME_KEY, value)     => hostname = value
      case Array(PORT_KEY, value)         => port = value.toInt
      case Array(BROKERS_KEY, value)      => brokers = Some(value)
      case Array(WORKERS_KEY, value)      => workers = Some(value)
      case Array(PARTITIONS_KEY, value)   => numberOfPartitions = value.toInt
      case Array(REPLICATIONS_KEY, value) => numberOfReplications = value.toShort
      case _                              => throw new IllegalArgumentException(USAGE)
    }
    var standalone = false
    val configurator =
      if (brokers.isEmpty && workers.isEmpty) {
        standalone = true
        Configurator
          .builder()
          .kafkaClient(new FakeKafkaClient())
          .connectClient(new FakeConnectorClient())
          .hostname(hostname)
          .port(port)
          .build()
      } else if (brokers.isEmpty ^ workers.isEmpty)
        throw new IllegalArgumentException(s"brokers:$brokers workers:$workers")
      else
        Configurator
          .builder()
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
    def update[T <: Data: ClassTag](id: String, data: T => T): Future[T] =
      value[T](id).flatMap(_ => store.update(id, v => data(v.asInstanceOf[T]))).map(_.asInstanceOf[T])

    def add[T <: Data](data: T): Future[T] = store.add(data.id, data).map(_.asInstanceOf[T])

    def exist[T <: Data: ClassTag](id: String): Future[Boolean] =
      store.value(id).map(classTag[T].runtimeClass.isInstance)

    def nonExist[T <: Data: ClassTag](id: String): Future[Boolean] = exist[T](id).map(!_)

    def size: Int = store.size

    override protected def doClose(): Unit = store.close()
  }
}
