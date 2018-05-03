package com.island.ohara.source.http

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import spray.json.{DefaultJsonProtocol, JsValue}
import spray.json._

import scala.util.{Failure, Success}

final case class CSV(row: List[JsValue])

trait CsvSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val csvFormat = jsonFormat1(CSV)
}

class WebService(system: ActorSystem, producer: KafkaProducer[String, String]) extends Directives with CsvSupport {
  def route = {
    pathSingleSlash {
      get {
        complete("OK")
      } ~ parameters("topic") { topic =>
        (post & entity(as[CSV])) { csv =>
          onComplete(producer.send(KafkaProducerRecord(topic, csv.toJson.toString))) {
            case Success(_) => complete(StatusCodes.Created.reason)
            // TODO: what message it should return.
            case Failure(ex) => complete(ex.toString)
          }
        }
      }
    }
  }
}
