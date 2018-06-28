package com.island.ohara.configurator

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.StandardRoute
import akka.stream.ActorMaterializer
import com.island.ohara.config.{OharaConfig, OharaJson}
import com.island.ohara.configurator.data.{OharaData, OharaException, OharaSchema}
import com.island.ohara.configurator.store.Store
import com.island.ohara.io.CloseOnce

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}

/**
  * A simple impl of Configurator. This impl maintains all subclass of ohara data in a single ohara store.
  * TODO: the store should be a special class providing the helper methods to iterate the specified sub-class of OharaData
  * @param hostname hostname of rest server
  * @param _port port of rest server
  * @param store store
  */
private class ConfiguratorImpl(uuidGenerator: () => String,
                               val hostname: String,
                               _port: Int,
                               store: Store[String, OharaData],
                               initializationTimeout: Duration,
                               terminationTimeout: Duration)
    extends Configurator {

  /**
    * this route is used to handle the request to schema.
    */
  private[this] val schemaRoute = locally {
    def rejectNonexistantUuid(uuid: String) = complete(
      StatusCodes.BadRequest -> OharaException(
        new IllegalArgumentException(s"Failed to find a schema mapping to $uuid")).toJson.toString)

    def handleException(function: () => StandardRoute) = try function()
    catch {
      case e: Throwable => complete(StatusCodes.BadRequest -> OharaException(e).toJson.toString)
    }
    val addSchema = pathEnd {
      post {
        entity(as[String]) { body =>
          handleException(() => {
            val request = OharaConfig(OharaJson(body))
            val schema =
              OharaSchema(uuidGenerator(),
                          OharaData.name.require(request),
                          OharaSchema.columnType.require(request),
                          OharaSchema.columnOrder.require(request))
            updateData(schema)
            complete("\"uuid\":\"" + schema.uuid + "\"")
          })
        }
      }
    }

    val listSchema = pathEnd(get(handleException(() => complete(listUuid[OharaSchema]))))

    val getSchema = path(Segment) { uuid =>
      {
        get {
          handleException(() =>
            getData[OharaSchema](uuid).map(r => complete(r.toJson.toString)).getOrElse(rejectNonexistantUuid(uuid)))
        }
      }
    }

    val deleteSchema = path(Segment) { uuid =>
      {
        delete {
          handleException(
            () =>
              removeData[OharaSchema](uuid)
                .map(data => complete(data.toJson.toString))
                .getOrElse(rejectNonexistantUuid(uuid)))
        }
      }
    }

    val updateSchema = path(Segment) { previousUuid =>
      {
        put {
          entity(as[String]) { body =>
            handleException(() => {
              getData[OharaSchema](previousUuid)
                .map(_ => {
                  val request = OharaConfig(OharaJson(body))
                  val schema =
                    OharaSchema(previousUuid,
                                OharaData.name.require(request),
                                OharaSchema.columnType.require(request),
                                OharaSchema.columnOrder.require(request))
                  updateData(schema)
                  complete("\"uuid\":\"" + previousUuid + "\"")
                })
                .getOrElse(rejectNonexistantUuid(previousUuid))
            })
          }
        }
      }
    }

    pathPrefix(Configurator.SCHEMA_PATH) {
      addSchema ~ listSchema ~ getSchema ~ deleteSchema ~ updateSchema
    }
  }

  /**
    * the full route consists of all routes against all subclass of ohara data and a final route used to reject other requests.
    */
  private[this] val route: server.Route = pathPrefix(Configurator.VERSION)(schemaRoute) ~ path(Remaining)(_ => {
    // TODO: just reject? by chia
    reject
  })

  private[this] implicit val actorSystem = ActorSystem(s"${classOf[ConfiguratorImpl].getSimpleName}-system")
  private[this] implicit val actorMaterializer = ActorMaterializer()
  private[this] val httpServer: Http.ServerBinding =
    Await.result(Http().bindAndHandle(route, hostname, _port), initializationTimeout.toMillis milliseconds)

  override val port = httpServer.localAddress.getPort

  override def schemas: Iterator[OharaSchema] = iterateData[OharaSchema]

  /**
    * Do what you want to do when calling closing.
    */
  override protected def doClose(): Unit = {
    if (httpServer != null)
      CloseOnce.release(() => Await.result(httpServer.unbind(), terminationTimeout.toMillis milliseconds), true)
    if (actorSystem != null)
      CloseOnce.release(() => Await.result(actorSystem.terminate(), terminationTimeout.toMillis milliseconds), true)
    store.close()
  }

  import scala.reflect._

  private[this] def listUuid[T <: OharaData: ClassTag](): String = {
    val rval = OharaConfig()
    rval.set("uuids", iterateData[T].map(d => (d.uuid -> d.name)).toMap)
    rval.toJson.toString
  }

  /**
    * Retrieve a "specified" sublcass of ohara data mapping the uuid. If the data mapping to the uuid is not the specified
    * type, the None will be returned.
    * @param uuid of ohara data
    * @tparam T subclass type
    * @return a subclass of ohara data
    */
  private[this] def getData[T <: OharaData: ClassTag](uuid: String): Option[T] =
    store.get(uuid).filter(classTag[T].runtimeClass.isInstance(_)).map(_.asInstanceOf[T])

  /**
    * Remove a "specified" sublcass of ohara data mapping the uuid. If the data mapping to the uuid is not the specified
    * type, the None will be returned.
    * @param uuid of ohara data
    * @tparam T subclass type
    * @return a subclass of ohara data
    */
  private[this] def removeData[T <: OharaData: ClassTag](uuid: String): Option[T] =
    getData[T](uuid).flatMap(d => store.remove(d.uuid).map(_.asInstanceOf[T]))

  /**
    * Update a "specified" sublcass of ohara data mapping the uuid. If the data mapping to the uuid is not the specified
    * type, the None will be returned.
    * @param data ohara data
    * @tparam T subclass type
    * @return a subclass of ohara data
    */
  private[this] def updateData[T <: OharaData: ClassTag](data: T): Option[T] =
    store.update(data.uuid, data).filter(classTag[T].runtimeClass.isInstance(_)).map(_.asInstanceOf[T])

  /**
    * Iterate the specified type. The unrelated type will be ignored.
    * @tparam T subclass type
    * @return iterator
    */
  private[this] def iterateData[T <: OharaData: ClassTag]: Iterator[T] = store.iterator
    .filter {
      case (_, data) => classTag[T].runtimeClass.isInstance(data)
    }
    .map {
      case (_, data) => data.asInstanceOf[T]
    }

  override def iterator: Iterator[OharaData] = store.map(_._2).iterator

  override def size = store.size
}
