package com.island.ohara.client.configurator.v0
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future

object StreamApi {
  val STREAM_PREFIX_PATH: String = "stream"
  val STREAM_PROPERTY_PREFIX_PATH: String = "property"
  val START_COMMAND: String = "start"
  val STOP_COMMAND: String = "stop"

  // Store object
  final case class StreamData(pipeline_id: String,
                              jarName: String,
                              name: String,
                              fromTopics: Seq[String],
                              toTopics: Seq[String],
                              instances: Int,
                              id: String,
                              filePath: String,
                              lastModified: Long)
      extends Data {
    override def kind: String = "streamApp"
  }

  // StreamApp Property Request Body
  final case class StreamPropertyRequest(name: String, fromTopics: Seq[String], toTopics: Seq[String], instances: Int)
  implicit val STREAM_PROPERTY_REQUEST_JSON_FORMAT: RootJsonFormat[StreamPropertyRequest] = jsonFormat4(
    StreamPropertyRequest)

  // StreamApp Property Page Response Body
  final case class StreamPropertyResponse(id: String,
                                          jarName: String,
                                          name: String,
                                          fromTopics: Seq[String],
                                          toTopics: Seq[String],
                                          instances: Int,
                                          lastModified: Long)
  implicit val STREAM_PROPERTY_RESPONSE_JSON_FORMAT: RootJsonFormat[StreamPropertyResponse] = jsonFormat7(
    StreamPropertyResponse)

  sealed trait PropertyAccess {
    def hostname(hostname: String): PropertyAccess
    def port(port: Int): PropertyAccess
    def get(id: String): Future[StreamPropertyResponse]
    def update(id: String, request: StreamPropertyRequest): Future[StreamPropertyResponse]
  }

  def accessOfProperty(): PropertyAccess = new PropertyAccess {
    private[this] val access: Access[StreamPropertyRequest, StreamPropertyResponse] =
      new Access[StreamPropertyRequest, StreamPropertyResponse](s"$STREAM_PREFIX_PATH/$STREAM_PROPERTY_PREFIX_PATH")

    override def hostname(hostname: String): PropertyAccess = {
      access.hostname(hostname)
      this
    }
    override def port(port: Int): PropertyAccess = {
      access.port(port)
      this
    }

    override def get(id: String): Future[StreamPropertyResponse] = access.get(id)
    override def update(id: String, request: StreamPropertyRequest): Future[StreamPropertyResponse] =
      access.update(id, request)
  }
}
