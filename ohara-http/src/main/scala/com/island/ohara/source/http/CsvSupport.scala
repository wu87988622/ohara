package com.island.ohara.source.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait CsvSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val csvFormat: RootJsonFormat[CSV] = jsonFormat1(CSV)
}
