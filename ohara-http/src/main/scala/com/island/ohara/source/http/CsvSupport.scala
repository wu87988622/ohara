package com.island.ohara.source.http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

trait CsvSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val csvFormat = jsonFormat1(CSV)
}
