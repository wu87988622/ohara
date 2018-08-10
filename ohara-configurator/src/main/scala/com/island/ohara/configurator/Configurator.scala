package com.island.ohara.configurator

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.{Http, server}
import akka.stream.ActorMaterializer
import com.island.ohara.config.UuidUtil
import com.island.ohara.configurator.endpoint.Validator
import com.island.ohara.configurator.store.{Store, StoreBuilder}
import com.island.ohara.io.CloseOnce
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.rest.ConfiguratorJson._
import com.island.ohara.rest.ConnectorClient
import com.island.ohara.serialization.Serializer
import com.typesafe.scalalogging.Logger
import org.apache.commons.lang3.exception.ExceptionUtils
import spray.json.RootJsonFormat

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}
import scala.reflect.ClassTag

/**
  * A simple impl of Configurator. This impl maintains all subclass of ohara data in a single ohara store.
  * TODO: the store should be a special class providing the helper methods to iterate the specified sub-class of OharaData
  *
  * @param hostname hostname of rest server
  * @param _port    port of rest server
  * @param store    store
  */
class Configurator private[configurator] (uuidGenerator: () => String,
                                          val hostname: String,
                                          _port: Int,
                                          val store: Store[String, Any],
                                          kafkaClient: KafkaClient,
                                          connectClient: ConnectorClient,
                                          initializationTimeout: Duration,
                                          terminationTimeout: Duration)
    extends CloseOnce
    with SprayJsonSupport {

  private val log = Logger(classOf[Configurator])

  private[this] def rejectNonexistentUuid(uuid: String) = complete(
    StatusCodes.BadRequest -> toResponse(new IllegalArgumentException(s"Failed to find a schema mapping to $uuid")))

  private[this] def toResponse(e: Throwable) = ErrorResponse(e.getClass.getName,
                                                             if (e.getMessage == null) "None" else e.getMessage,
                                                             ExceptionUtils.getStackTrace(e))

  private[this] val exceptionHandler = ExceptionHandler {
    case e: IllegalArgumentException =>
      extractUri { uri =>
        log.error(s"Request to $uri could not be handled normally")
        complete(StatusCodes.BadRequest -> toResponse(e))
      }
    case e: Throwable => {
      log.error("What happens here?", e)
      complete(StatusCodes.ServiceUnavailable -> toResponse(e))
    }
  }

  /**
    * a basic route used to handle CRUD.
    */
  private[this] def basicRoute[Req, Res: ClassTag](toData: (String, Req) => Res, prefix: String)(
    implicit f0: RootJsonFormat[Req],
    f1: RootJsonFormat[Res]): server.Route = pathPrefix(prefix) {
    pathEnd {
      // add
      post {
        entity(as[Req].map(req => {
          val uuid = uuidGenerator()
          (uuid, toData(uuid, req))
        })) {
          case (uuid, data) =>
            updateData(uuid, data)
            complete(data)
        }
      } ~ get(complete(data[Res].toSeq)) // list
    } ~ path(Segment) { uuid =>
      // get data by uuid
      get(data[Res](uuid).map(complete(_)).getOrElse(rejectNonexistentUuid(uuid))) ~
        // delete get by uuid
        delete(removeData[Res](uuid).map(complete(_)).getOrElse(rejectNonexistentUuid(uuid))) ~
        // update data by uuid
        put {
          entity(as[Req].map(req => (uuid, toData(uuid, req)))) {
            case (uuid, newData) =>
              data[Res](uuid)
                .map(_ => {
                  updateData(uuid, newData)
                  complete(newData)
                })
                .getOrElse(rejectNonexistentUuid(uuid))
          }
        }
    }
  }

  //-----------------------------------------------[topic]-----------------------------------------------//
  private[this] val topicRoute = locally {

    def toTopicInfo(uuid: String, request: TopicInfoRequest) = TopicInfo(uuid,
                                                                         request.name,
                                                                         request.numberOfPartitions,
                                                                         request.numberOfReplications,
                                                                         System.currentTimeMillis())

    val addTopic = pathEnd {
      post {
        entity(as[TopicInfoRequest].map(toTopicInfo(uuidGenerator(), _))) { topicInfo =>
          if (kafkaClient.exist(topicInfo.uuid))
            // this should be impossible....
            throw new IllegalArgumentException(s"The topic:${topicInfo.uuid} exists")
          else {
            kafkaClient.topicCreator
            // NOTED: we use the uuid to create topic since we allow user to change the topic name arbitrary
              .topicName(topicInfo.uuid)
              .numberOfPartitions(topicInfo.numberOfPartitions)
              .numberOfReplications(topicInfo.numberOfReplications)
              .create()
            updateData(topicInfo.uuid, topicInfo)
            complete(topicInfo)
          }
        }
      }
    }

    val listTopic = pathEnd(get(complete(data[TopicInfo].toSeq)))

    val updateTopic = path(Segment) { uuid =>
      put {
        entity(as[TopicInfoRequest].map(toTopicInfo(uuid, _))) { newTopicInfo =>
          data[TopicInfo](uuid)
            .map(previousTopicInfo => {
              if (previousTopicInfo.numberOfReplications != newTopicInfo.numberOfReplications)
                throw new IllegalArgumentException("Non-support to change the number of replications")
              if (previousTopicInfo.numberOfPartitions != newTopicInfo.numberOfPartitions)
                kafkaClient.addPartition(uuid, newTopicInfo.numberOfPartitions)
              updateData(uuid, newTopicInfo)
              complete(newTopicInfo)
            })
            .getOrElse(rejectNonexistentUuid(uuid))
        }
      }
    }

    val getTopic =
      path(Segment)(uuid => get(data[TopicInfo](uuid).map(complete(_)).getOrElse(rejectNonexistentUuid(uuid))))

    val deleteTopic = path(Segment) { uuid =>
      delete {
        removeData[TopicInfo](uuid)
          .map(topicInfo => {
            kafkaClient.deleteTopic(uuid)
            complete(topicInfo)
          })
          .getOrElse(rejectNonexistentUuid(uuid))
      }
    }

    pathPrefix(TOPIC_PATH) {
      addTopic ~ listTopic ~ updateTopic ~ getTopic ~ deleteTopic
    }
  }

  //-----------------------------------------------[schema]-----------------------------------------------//
  private[this] val schemaRoute = basicRoute(
    (uuid: String, request: SchemaRequest) =>
      Schema(uuid, request.name, request.types, request.orders, request.disabled, System.currentTimeMillis()),
    SCHEMA_PATH
  )

  //-----------------------------------------------[hdfs]-----------------------------------------------//
  private[this] val hdfsRoute = basicRoute(
    (uuid: String, request: HdfsInformationRequest) =>
      HdfsInformation(uuid, request.name, request.uri, System.currentTimeMillis()),
    HDFS_PATH)

  //-----------------------------------------------[validation]-----------------------------------------------//
  private[this] val validationRoute = path(VALIDATION_PATH / HDFS_VALIDATION_PATH) {
    put {
      entity(as[HdfsValidationRequest]) { request =>
        {
          val reports = Validator.run(connectClient, kafkaClient.brokersString, request, 3)
          if (reports.isEmpty) throw new IllegalStateException(s"No report!!! Failed to run the validation")
          complete(reports)
        }
      }
    }
  }

  //-----------------------------------------------[cluster]-----------------------------------------------//
  private[this] val clusterRoute = path(CLUSTER_PATH) {
    get {
      complete(ClusterInformation(kafkaClient.brokersString, connectClient.workersString))
    }
  }

  /**
    * the full route consists of all routes against all subclass of ohara data and a final route used to reject other requests.
    */
  private[this] val route: server.Route = handleExceptions(exceptionHandler) {
    pathPrefix(VERSION_V0)(schemaRoute ~ topicRoute ~ hdfsRoute ~ validationRoute ~ clusterRoute) ~ path(Remaining)(_ =>
      reject)
  }

  private[this] implicit val actorSystem = ActorSystem(s"${classOf[Configurator].getSimpleName}-system")
  private[this] implicit val actorMaterializer = ActorMaterializer()
  private[this] val httpServer: Http.ServerBinding =
    Await.result(Http().bindAndHandle(route, hostname, _port), initializationTimeout.toMillis milliseconds)

  /**
    * Do what you want to do when calling closing.
    */
  override protected def doClose(): Unit = {
    if (httpServer != null)
      CloseOnce.release(() => Await.result(httpServer.unbind(), terminationTimeout.toMillis milliseconds), true)
    if (actorSystem != null)
      CloseOnce.release(() => Await.result(actorSystem.terminate(), terminationTimeout.toMillis milliseconds), true)
    CloseOnce.close(store)
    CloseOnce.close(kafkaClient)
  }

  import scala.reflect._

  /**
    * Remove a "specified" sublcass of ohara data mapping the uuid. If the data mapping to the uuid is not the specified
    * type, the None will be returned.
    *
    * @param uuid of ohara data
    * @tparam T subclass type
    * @return a subclass of ohara data
    */
  private[this] def removeData[T: ClassTag](uuid: String): Option[T] =
    data[T](uuid).flatMap(d => store.remove(uuid).map(_.asInstanceOf[T]))

  /**
    * Update a "specified" sublcass of ohara data mapping the uuid. If the data mapping to the uuid is not the specified
    * type, the None will be returned.
    *
    * @param data ohara data
    * @tparam T subclass type
    * @return a subclass of ohara data
    */
  private[this] def updateData[T](uuid: String, data: T): Option[T] =
    store.update(uuid, data).filter(_.getClass.equals(data.getClass)).map(_.asInstanceOf[T])

  //-----------------[public interfaces]-----------------//

  val port = httpServer.localAddress.getPort

  /**
    * Iterate the specified type. The unrelated type will be ignored.
    *
    * @tparam T subclass type
    * @return iterator
    */
  def data[T: ClassTag]: Iterator[T] =
    store.map(_._2).iterator.filter(classTag[T].runtimeClass.isInstance(_)).map(_.asInstanceOf[T])

  /**
    * Retrieve a "specified" sublcass of ohara data mapping the uuid. If the data mapping to the uuid is not the specified
    * type, the None will be returned.
    *
    * @param uuid of ohara data
    * @tparam T subclass type
    * @return a subclass of ohara data
    */
  def data[T: ClassTag](uuid: String): Option[T] =
    store.get(uuid).filter(classTag[T].runtimeClass.isInstance(_)).map(_.asInstanceOf[T])

  def size: Int = store.size
}

object Configurator {
  def storeBuilder: StoreBuilder[String, Any] = Store.builder(Serializer.STRING, Serializer.OBJECT)
  def builder = new ConfiguratorBuilder()

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
    if (args.length == 1 && args(0).equals(HELP_KEY)) {
      println(USAGE)
      return
    }
    if (args.size < 2 || args.size % 2 != 0) throw new IllegalArgumentException(USAGE)
    // TODO: make the parse more friendly
    var hostname = "localhost"
    var port: Int = 0
    var brokers: Option[String] = None
    var workers: Option[String] = None
    var topicName = "test-topic"
    var numberOfPartitions: Int = 1
    var numberOfReplications: Short = 1
    args.sliding(2, 2).foreach {
      case Array(HOSTNAME_KEY, value)     => hostname = value
      case Array(PORT_KEY, value)         => port = value.toInt
      case Array(BROKERS_KEY, value)      => if (!value.toLowerCase.equals("none")) brokers = Some(value)
      case Array(WORKERS_KEY, value)      => if (!value.toLowerCase.equals("none")) workers = Some(value)
      case Array(TOPIC_KEY, value)        => topicName = value
      case Array(PARTITIONS_KEY, value)   => numberOfPartitions = value.toInt
      case Array(REPLICATIONS_KEY, value) => numberOfReplications = value.toShort
      case _                              => throw new IllegalArgumentException(USAGE)
    }
    var standalone = false
    val configurator =
      if (brokers.isEmpty || workers.isEmpty) {
        standalone = true
        Configurator.builder.noCluster.hostname(hostname).port(port).build()
      } else
        Configurator.builder
          .store(
            Store
              .builder(Serializer.STRING, Serializer.OBJECT)
              .brokers(brokers.get)
              .topicName(topicName)
              .numberOfReplications(numberOfReplications)
              .numberOfPartitions(numberOfPartitions)
              .build())
          .kafkaClient(KafkaClient(brokers.get))
          .connectClient(ConnectorClient(workers.get))
          .hostname(hostname)
          .port(port)
          .build()
    hasRunningConfigurator = true
    try {
      LOG.info(
        s"start a ${(if (standalone) "standalone" else "truly")} configurator built on hostname:${configurator.hostname} and port:${configurator.port}")
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
}
