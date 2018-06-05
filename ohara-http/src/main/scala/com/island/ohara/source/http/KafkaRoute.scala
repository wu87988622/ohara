package com.island.ohara.source.http

import java.util.UUID
import java.util.concurrent.ConcurrentMap

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import cakesolutions.kafka.{KafkaProducerLike, KafkaProducerRecord}
import com.island.ohara.core.{Cell, Row, RowBuilder}
import com.island.ohara.serialization._
import spray.json.{JsBoolean, JsString, JsValue}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

final case class CSV(row: List[JsValue])
final case class SchemaException(private val message: String) extends Exception(message)

/**
  * Use to construct routing logic of Akka-HTTP to Kafka Producer
  */
trait KafkaRoute extends Directives with CsvSupport {

  implicit def system: ActorSystem

  implicit val kafkaDispatch = system.dispatchers.lookup("kafka-dispatcher")

  private lazy val log = Logging(system, classOf[KafkaRoute])

  def kafkaRoute(producer: KafkaProducerLike[String, Row], schemaMap: ConcurrentMap[String, (String, RowSchema)]) = {
    path(
      Segment.flatMap(
        pathName =>
          if (schemaMap.containsKey(pathName))
            Some(pathName)
          else
          None)) { pathName =>
      get {
        complete("EXIST")
      } ~ (post & entity(as[CSV])) { csv =>
        val (kafkaTopic, oharaSchema) = schemaMap.get(pathName)
        onComplete(
          transform(csv.row, oharaSchema.schema) match {
            case Success(oharaRow) =>
              producer.send(KafkaProducerRecord(kafkaTopic, UUID.randomUUID().toString, oharaRow))
            case Failure(e) =>
              Future(e)
          }
        ) {
          case Success(_) => complete(StatusCodes.Created.reason)
          // TODO: what message it should return.
          case Failure(ex) => {
            ex match {
              case SchemaException(_) => complete(StatusCodes.BadRequest)
              case _ => {
                // TODO: define failed message
                log.warning(s"Fail to send message to Kafka: $ex")
                complete(ex.toString)
              }
            }
          }
        }
      }
    }
  }

  private def transform(rows: List[JsValue], types: Vector[(String, DataType)]): Try[Row] = {

    def create(jsValue: JsValue, cellInfo: (String, DataType)): Cell[Any] = {
      val (cellName, cellType) = cellInfo
      (cellType, jsValue) match {
        case (STRING, JsString(str))       => Cell.builder.name(cellName).build(str)
        case (BOOLEAN, JsBoolean(boolean)) => Cell.builder.name(cellName).build(boolean)
        case (SHORT, _)                    => Cell.builder.name(cellName).build(jsValue.convertTo[Short])
        case (INT, _)                      => Cell.builder.name(cellName).build(jsValue.convertTo[Int])
        case (LONG, _)                     => Cell.builder.name(cellName).build(jsValue.convertTo[Long])
        case (FLOAT, _)                    => Cell.builder.name(cellName).build(jsValue.convertTo[Float])
        case (DOUBLE, _)                   => Cell.builder.name(cellName).build(jsValue.convertTo[Double])
        case (BYTE, _)                     => Cell.builder.name(cellName).build(jsValue.convertTo[Byte])
        case (BYTES, _)                    => Cell.builder.name(cellName).build(jsValue.convertTo[Array[Byte]])
        // TODO: more specific which JSON type can match to ohara data type
      }
    }

    def buildRow(rowBuilder: RowBuilder, list: List[(JsValue, (String, DataType))]): Row = {
      list match {
        case Nil                         => rowBuilder.build()
        case (jsValue, dataType) :: tail => buildRow(rowBuilder.append(create(jsValue, dataType)), tail)
      }
    }
    if (rows.size != types.size) {
      log.info(s"JSON didn't match supported schema.")
      Failure(throw SchemaException("JSON didn't match supported schema."))
    } else {
      Try(buildRow(Row.builder, rows zip types))
    }
  }
}
