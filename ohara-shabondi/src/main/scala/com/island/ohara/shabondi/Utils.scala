/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.shabondi

import java.util.concurrent.CompletableFuture

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.island.ohara.common.data.Row
import spray.json.{DefaultJsonProtocol, JsObject, JsValue, RootJsonFormat}

import scala.compat.java8.FutureConverters
import scala.concurrent.Future

private[shabondi] object JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  type RowData = Map[String, JsValue] // column, value

  implicit val rowDataFormat: RootJsonFormat[RowData] = new RootJsonFormat[RowData] {
    override def write(obj: RowData): JsValue = JsObject(obj)
    override def read(json: JsValue): RowData = json.asJsObject.fields
  }

  def toRow(rowData: RowData): Row = com.island.ohara.client.configurator.v0.toRow(JsObject(rowData))
  def toRowData(row: Row): RowData = com.island.ohara.client.configurator.v0.toJson(row).fields
}

private[shabondi] object ConvertSupport {
  implicit class ScalaFutureConverter[T](completableFuture: java.util.concurrent.Future[T]) {
    def toScala: Future[T] = {
      FutureConverters.toScala(completableFuture.asInstanceOf[CompletableFuture[T]])
    }
  }
}
