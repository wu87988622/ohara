package com.island.ohara.configurator

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import akka.http.scaladsl.{Http, server}
import akka.stream.ActorMaterializer
import com.island.ohara.config.{OharaConfig, OharaJson, UuidUtil}
import com.island.ohara.configurator.kafka.KafkaClient
import com.island.ohara.configurator.store.{Store, StoreBuilder}
import com.island.ohara.data._
import com.island.ohara.io.CloseOnce
import com.island.ohara.serialization.{Serializer, StringSerializer}
import com.typesafe.scalalogging.Logger

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}

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
                                          val store: Store[String, OharaData],
                                          kafkaClient: KafkaClient,
                                          initializationTimeout: Duration,
                                          terminationTimeout: Duration)
    extends Iterable[OharaData]
    with CloseOnce {

  private val log = Logger(classOf[Configurator])

  private[this] def rejectNonexistentUuid(uuid: String) = completeJson(
    OharaException(new IllegalArgumentException(s"Failed to find a schema mapping to $uuid")).toJson,
    StatusCodes.BadRequest)

  private[this] def handleException(function: () => StandardRoute): StandardRoute = try function()
  catch {
    // Parsing the invalid request can cause the IllegalArgumentException
    case e: IllegalArgumentException => completeJson(OharaException(e).toJson, StatusCodes.BadRequest)
    // otherwise configurator may encounter some bugs
    case e: Throwable => {
      log.error("What happens here?", e)
      completeJson(OharaException(e).toJson, StatusCodes.ServiceUnavailable)
    }
  }

  /**
    * complete the request with json response. This method also add the "application/json" to the header
    *
    * @param json response body
    * @return route
    */
  private[this] def completeJson(json: OharaJson, status: StatusCode = StatusCodes.OK) = complete(
    HttpResponse(status, entity = HttpEntity(ContentType(MediaTypes.`application/json`), json.toString)))

  private[this] def completeUuid(uuid: String) = completeJson(OharaJson("{\"uuid\":\"" + uuid + "\"}"))

  /**
    * this route is used to handle the request to schema.
    */
  private[this] val schemaRoute = locally {

    val addSchema = pathEnd {
      post {
        entity(as[String]) { body =>
          handleException(() => {
            val schema = OharaSchema(uuidGenerator(), OharaJson(body))
            updateData(schema)
            completeUuid(schema.uuid)
          })
        }
      }
    }

    val listSchema = pathEnd(get(handleException(() => completeJson(listUuid[OharaSchema]))))

    val getSchema = path(Segment) { uuid =>
      get {
        handleException(
          () => data[OharaSchema](uuid).map(r => completeJson(r.toJson)).getOrElse(rejectNonexistentUuid(uuid)))
      }
    }

    val deleteSchema = path(Segment) { uuid =>
      delete {
        handleException(() =>
          removeData[OharaSchema](uuid).map(data => completeJson(data.toJson)).getOrElse(rejectNonexistentUuid(uuid)))
      }
    }

    val updateSchema = path(Segment) { previousUuid =>
      put {
        entity(as[String]) { body =>
          handleException(() => {
            data[OharaSchema](previousUuid)
              .map(_ => {
                val schema = OharaSchema(previousUuid, OharaJson(body))
                // TODO: we should disallow user to reduce or reorder the column. by chia
                updateData(schema)
                completeUuid(previousUuid)
              })
              .getOrElse(rejectNonexistentUuid(previousUuid))
          })
        }
      }
    }

    pathPrefix(Configurator.SCHEMA_PATH) {
      addSchema ~ listSchema ~ getSchema ~ deleteSchema ~ updateSchema
    }
  }

  private[this] val topicRoute = locally {

    val addTopic = pathEnd {
      post {
        entity(as[String]) { body =>
          handleException(() => {
            val topicInfo =
              OharaTopic(uuidGenerator(), OharaJson(body))
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
              updateData(topicInfo)
              completeUuid(topicInfo.uuid)
            }
          })
        }
      }
    }

    val listTopic = pathEnd(get(handleException(() => completeJson(listUuid[OharaTopic]))))

    val updateTopic = path(Segment) { previousUuid =>
      put {
        entity(as[String]) { body =>
          handleException(() => {
            data[OharaTopic](previousUuid)
              .map(previousTopic => {
                val topic = OharaTopic(previousUuid, OharaJson(body))
                if (previousTopic.numberOfReplications != topic.numberOfReplications)
                  throw new IllegalArgumentException("Non-support to change the number of replications")
                if (previousTopic.numberOfPartitions != topic.numberOfPartitions)
                  kafkaClient.addPartition(previousUuid, topic.numberOfPartitions)
                updateData(topic)
                completeUuid(previousUuid)
              })
              .getOrElse(rejectNonexistentUuid(previousUuid))
          })
        }
      }
    }

    val getTopic = path(Segment) { uuid =>
      get {
        handleException(
          () => data[OharaTopic](uuid).map(r => completeJson(r.toJson)).getOrElse(rejectNonexistentUuid(uuid)))
      }
    }

    val deleteTopic = path(Segment) { uuid =>
      delete {
        handleException(
          () =>
            removeData[OharaTopic](uuid)
              .map(data => {
                kafkaClient.deleteTopic(uuid)
                completeJson(data.toJson)
              })
              .getOrElse(rejectNonexistentUuid(uuid)))
      }
    }

    pathPrefix(Configurator.TOPIC_PATH) {
      addTopic ~ listTopic ~ updateTopic ~ getTopic ~ deleteTopic
    }
  }

  /**
    * the full route consists of all routes against all subclass of ohara data and a final route used to reject other requests.
    */
  private[this] val route: server.Route = pathPrefix(Configurator.VERSION)(schemaRoute ~ topicRoute) ~ path(Remaining)(
    _ => {
      // TODO: just reject? by chia
      reject
    })

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

  private[this] def listUuid[T <: OharaData: ClassTag](): OharaJson = {
    val rval = OharaConfig()
    rval.set("uuids", data[T].map(d => (d.uuid -> d.name)).toMap)
    rval.toJson
  }

  /**
    * Remove a "specified" sublcass of ohara data mapping the uuid. If the data mapping to the uuid is not the specified
    * type, the None will be returned.
    *
    * @param uuid of ohara data
    * @tparam T subclass type
    * @return a subclass of ohara data
    */
  private[this] def removeData[T <: OharaData: ClassTag](uuid: String): Option[T] =
    data[T](uuid).flatMap(d => store.remove(d.uuid).map(_.asInstanceOf[T]))

  /**
    * Update a "specified" sublcass of ohara data mapping the uuid. If the data mapping to the uuid is not the specified
    * type, the None will be returned.
    *
    * @param data ohara data
    * @tparam T subclass type
    * @return a subclass of ohara data
    */
  private[this] def updateData[T <: OharaData: ClassTag](data: T): Option[T] =
    store.update(data.uuid, data).filter(classTag[T].runtimeClass.isInstance(_)).map(_.asInstanceOf[T])

  //-----------------[public interfaces]-----------------//

  override def iterator: Iterator[OharaData] = store.map(_._2).iterator

  override def size: Int = store.size

  val port = httpServer.localAddress.getPort

  /**
    * Iterate the specified type. The unrelated type will be ignored.
    *
    * @tparam T subclass type
    * @return iterator
    */
  def data[T <: OharaData: ClassTag]: Iterator[T] =
    store.map(_._2).iterator.filter(classTag[T].runtimeClass.isInstance(_)).map(_.asInstanceOf[T])

  /**
    * Retrieve a "specified" sublcass of ohara data mapping the uuid. If the data mapping to the uuid is not the specified
    * type, the None will be returned.
    *
    * @param uuid of ohara data
    * @tparam T subclass type
    * @return a subclass of ohara data
    */
  def data[T <: OharaData: ClassTag](uuid: String): Option[T] =
    store.get(uuid).filter(classTag[T].runtimeClass.isInstance(_)).map(_.asInstanceOf[T])
}

object Configurator {
  def storeBuilder: StoreBuilder[String, OharaData] = Store.builder(Serializer.string, OharaDataSerializer)
  def builder = new ConfiguratorBuilder()

  val DEFAULT_UUID_GENERATOR: () => String = () => UuidUtil.uuid()

  val VERSION = "v0"
  val SCHEMA_PATH = "schemas"
  val DEFAULT_INITIALIZATION_TIMEOUT: Duration = 10 seconds
  val DEFAULT_TERMINATION_TIMEOUT: Duration = 10 seconds
  val TOPIC_PATH = "topics"

  //----------------[main]----------------//
  private[this] lazy val LOG = Logger(Configurator.getClass)
  val HELP_KEY = "--help"
  val HOSTNAME_KEY = "--hostname"
  val PORT_KEY = "--port"
  val BROKERS_KEY = "--brokers"
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
    var topicName: Option[String] = None
    var numberOfPartitions: Int = 1
    var numberOfReplications: Short = 1
    args.sliding(2, 2).foreach {
      case Array(HOSTNAME_KEY, value)     => hostname = value
      case Array(PORT_KEY, value)         => port = value.toInt
      case Array(BROKERS_KEY, value)      => brokers = Some(value)
      case Array(TOPIC_KEY, value)        => topicName = Some(value)
      case Array(PARTITIONS_KEY, value)   => numberOfPartitions = value.toInt
      case Array(REPLICATIONS_KEY, value) => numberOfReplications = value.toShort
      case _                              => throw new IllegalArgumentException(USAGE)
    }

    if (brokers.isEmpty ^ topicName.isEmpty)
      throw new IllegalArgumentException(if (brokers.isEmpty) "brokers" else "topic" + " can't be empty")

    val configurator =
      if (brokers.isEmpty) Configurator.builder.noCluster.hostname(hostname).port(port).build()
      else
        Configurator.builder
          .store(
            Store
              .builder(StringSerializer, OharaDataSerializer)
              .brokers(brokers.get)
              .topicName(topicName.get)
              .numberOfReplications(numberOfReplications)
              .numberOfPartitions(numberOfPartitions)
              .build())
          .kafkaClient(KafkaClient(brokers.get))
          .hostname(hostname)
          .port(port)
          .build()
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
}
