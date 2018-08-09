package com.island.ohara.rest
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{Marshal, Marshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import com.island.ohara.io.CloseOnce
import com.island.ohara.rest.ConfiguratorJson._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Used to communicate with Configurator. The public interface of Configurator is only restful apis. Sometimes the restful
  * apis is unfriendly to use in coding. Hence, this class wraps the restful apis to scala apis. The implementation is based
  * on akka-http and akka-json.
  *
  */
trait ConfiguratorClient extends CloseOnce {

  /**
    * @return a list of schema
    */
  def schemas(): Seq[Schema]

  /**
    * @return a list of topic
    */
  def topics(): Seq[TopicInfo]

  /**
    * Query a schema by a specified uuid. If no schema maps to the uuid, an exception will be thrown.
    * @param uuid uuid of schema
    * @return a schema having the specified uuid. Otherwise, a exception will be thrown.
    */
  def schema(uuid: String): Schema

  /**
    * Query a topic by a specified uuid. If no schema maps to the uuid, an exception will be thrown.
    * @param uuid uuid of topic
    * @return a topic having the specified uuid. Otherwise, a exception will be thrown.
    */
  def topic(uuid: String): TopicInfo

  /**
    * @param request schema request
    * @return the completed schema
    */
  def add(request: SchemaRequest): Schema

  /**
    * @param request topic request
    * @return the completed topic
    */
  def add(request: TopicInfoRequest): TopicInfo

  /**
    * If there is no existent schema having specified uuid, an exception will be thrown.
    * @param uuid uuid of schema
    * @param request updated schema
    * @return the completed schema
    */
  def update(uuid: String, request: SchemaRequest): Schema

  /**
    * If there is no existent topic having specified uuid, an exception will be thrown.
    * @param uuid uuid of topic
    * @param request updated topic
    * @return the completed topic
    */
  def update(uuid: String, request: TopicInfoRequest): TopicInfo

  /**
    * If there is no existent topic having specified uuid, an exception will be thrown.
    * @param uuid uuid of topic
    * @return the deleted topic
    */
  def deleteTopic(uuid: String): TopicInfo

  /**
    * If there is no existent schema having specified uuid, an exception will be thrown.
    * @param uuid uuid of schema
    * @return the deleted schema
    */
  def deleteSchema(uuid: String): Schema
}

object ConfiguratorClient {
  private[this] val COUNTER = new AtomicInteger(0)
  import scala.concurrent.duration._
  private[this] val TIMEOUT = 10 seconds
  private[this] val VERSION = "v0"
  private[this] val TOPIC_CMD = "topics"
  private[this] val SCHEMA_CMD = "schemas"

  def apply(configuratorInfo: String): ConfiguratorClient = new ConfiguratorClient with SprayJsonSupport {
    private[this] implicit val actorSystem = ActorSystem(
      s"${classOf[ConfiguratorClient].getSimpleName}-${COUNTER.getAndIncrement()}-system")
    private[this] implicit val actorMaterializer = ActorMaterializer()
    override protected def doClose(): Unit = Await.result(actorSystem.terminate(), 60 seconds)
    private[this] def uri(cmd: String) = s"http://$configuratorInfo/$VERSION/$cmd"

    private[this] def unmarshal[T](res: HttpResponse)(implicit um: Unmarshaller[ResponseEntity, T]): Future[T] =
      if (res.status.isSuccess()) Unmarshal(res.entity).to[T]
      else
        Unmarshal(res.entity)
          .to[ErrorResponse]
          .flatMap(error => Future.failed(new IllegalArgumentException(error.message)))

    private[this] def data[T](cmd: String)(implicit um: Unmarshaller[ResponseEntity, T]): T =
      Await.result(Http().singleRequest(HttpRequest(HttpMethods.GET, uri(cmd))).flatMap(unmarshal[T](_)), TIMEOUT)

    override def schemas(): Seq[Schema] = data[Seq[Schema]](SCHEMA_CMD)
    override def topics(): Seq[TopicInfo] = data[Seq[TopicInfo]](TOPIC_CMD)

    private[this] def send[Req, Res](method: HttpMethod, cmd: String, request: Req)(
      implicit m: Marshaller[Req, RequestEntity],
      um: Unmarshaller[ResponseEntity, Res]): Res = Await.result(
      Marshal(request)
        .to[RequestEntity]
        .flatMap(entity => {
          Http().singleRequest(HttpRequest(method, uri(cmd), entity = entity)).flatMap(unmarshal[Res](_))
        }),
      TIMEOUT
    )

    override def add(request: SchemaRequest): Schema = send[SchemaRequest, Schema](HttpMethods.POST, "schemas", request)
    override def add(request: TopicInfoRequest): TopicInfo =
      send[TopicInfoRequest, TopicInfo](HttpMethods.POST, TOPIC_CMD, request)
    override def update(uuid: String, request: SchemaRequest): Schema =
      send[SchemaRequest, Schema](HttpMethods.PUT, s"$SCHEMA_CMD/$uuid", request)
    override def update(uuid: String, request: TopicInfoRequest): TopicInfo =
      send[TopicInfoRequest, TopicInfo](HttpMethods.PUT, s"$TOPIC_CMD/$uuid", request)

    private[this] def delete[T](cmd: String)(implicit um: Unmarshaller[ResponseEntity, T]): T =
      Await.result(Http().singleRequest(HttpRequest(HttpMethods.DELETE, uri(cmd))).flatMap(unmarshal[T](_)), TIMEOUT)
    override def deleteTopic(uuid: String): TopicInfo = delete[TopicInfo](s"$TOPIC_CMD/$uuid")
    override def deleteSchema(uuid: String): Schema = delete[Schema](s"$SCHEMA_CMD/$uuid")

    override def schema(uuid: String): Schema = data[Schema](s"$SCHEMA_CMD/$uuid")
    override def topic(uuid: String): TopicInfo = data[TopicInfo](s"$TOPIC_CMD/$uuid")
  }
}
