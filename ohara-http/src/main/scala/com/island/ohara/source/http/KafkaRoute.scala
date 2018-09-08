package com.island.ohara.source.http

import java.util.UUID
import java.util.concurrent.ConcurrentMap

import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.kafka.{Producer, RecordMetadata}
import com.island.ohara.serialization.DataType._
import com.island.ohara.serialization._
import spray.json.{JsBoolean, JsString, JsValue}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

final case class CSV(row: List[JsValue])
final case class SchemaException(private val message: String) extends Exception(message)

/**
  * Use to construct routing logic of HTTP post to Kafka Producer
  */
trait KafkaRoute extends Directives with CsvSupport {

  implicit def system: ActorSystem

  /**
    * Using a different dispatcher because Kafka Producer is blocking.
    */
  implicit def kafkaDispatch: MessageDispatcher = system.dispatchers.lookup("kafka-dispatcher")

  private lazy val log = Logging(system, classOf[KafkaRoute])

  def kafkaRoute(producer: Producer[String, Row], schemaMap: ConcurrentMap[String, (String, RowSchema)]): Route = {
    path(Segment.flatMap(pathName => Some(pathName).filter(schemaMap.containsKey))) { pathName =>
      get {
        complete("EXIST")
      } ~ (post & entity(as[CSV])) { csv =>
        val (kafkaTopic, oharaSchema) = schemaMap.get(pathName)
        onComplete(
          transform(csv.row, oharaSchema.schema) match {
            case Success(oharaRow) => producer.sender().key(UUID.randomUUID().toString).value(oharaRow).send(kafkaTopic)
            case Failure(e)        => Future(e)
          }
        ) {
          case Success(_) => complete(StatusCodes.Created.reason)
          // TODO: what message it should return.
          case Failure(ex) =>
            ex match {
              case SchemaException(_) => complete(StatusCodes.BadRequest)
              case _                  =>
                // TODO: define failed message
                log.warning(s"Fail to send message to Kafka: $ex")
                complete(ex.toString)
            }
        }
      }
    }
  }

  private def transform(rows: List[JsValue], types: Vector[(String, DataType)]): Try[Row] = {

    def create(jsValue: JsValue, cellInfo: (String, DataType)): Cell[Any] = {
      val (cellName, cellType) = cellInfo
      Cell(
        cellName,
        (cellType, jsValue) match {
          case (STRING, JsString(str))       => str
          case (BOOLEAN, JsBoolean(boolean)) => boolean
          case (SHORT, _)                    => jsValue.convertTo[Short]
          case (INT, _)                      => jsValue.convertTo[Int]
          case (LONG, _)                     => jsValue.convertTo[Long]
          case (FLOAT, _)                    => jsValue.convertTo[Float]
          case (DOUBLE, _)                   => jsValue.convertTo[Double]
          case (BYTE, _)                     => jsValue.convertTo[Byte]
          case (BYTES, _)                    => jsValue.convertTo[Array[Byte]]
          // TODO: more specific which JSON type can match to ohara data type
        }
      )
    }

    if (rows.size != types.size) {
      log.info(s"JSON didn't match supported schema.")
      Failure(SchemaException("JSON didn't match supported schema."))
    } else {
      Try(Row(rows.zipWithIndex.map {
        case (jsValue, index) => create(jsValue, types(index))
      }: _*))
    }
  }
}
