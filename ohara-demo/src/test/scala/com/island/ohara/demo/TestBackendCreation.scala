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

package com.island.ohara.demo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import com.island.ohara.client.configurator.ConfiguratorApiInfo
import com.island.ohara.client.configurator.v0.QueryApi
import com.island.ohara.client.configurator.v0.QueryApi.{RdbColumn, RdbQuery}
import com.island.ohara.common.rule.LargeTest
import com.island.ohara.demo.Backend.{Creation, _}
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
@deprecated("embedded services are deprecated. We all should love docker, shouldn't we?", "0.2")
class TestBackendCreation extends LargeTest with Matchers {

  @Test
  def testCreation(): Unit = {
    testCreation(
      Seq(
        Creation(methodName, Seq(RdbColumn("cf", "integer", true))),
        Creation(methodName + "_2", Seq(RdbColumn("cf", "integer", true), RdbColumn("cf2", "integer", false)))
      ))
  }

  private[this] def testCreation(creations: Seq[Creation]): Unit = {
    Backend.run(
      ServicePorts.default,
      (configurator, _, _, _, db, _) => {
        implicit val actorSystem: ActorSystem = ActorSystem(methodName)
        try creations.foreach {
          creation =>
            Await.result(
              Marshal(creation)
                .to[RequestEntity]
                .flatMap(entity => {
                  Http()
                    .singleRequest(
                      HttpRequest(HttpMethods.POST,
                                  s"http://localhost:${configurator.port}/${ConfiguratorApiInfo.PRIVATE}/creation/rdb",
                                  entity = entity))
                    .flatMap(res => {
                      if (res.status.isSuccess()) Future.successful((): Unit)
                      else
                        Future.failed(
                          new IllegalArgumentException(s"Failed to create table. error:${res.status.intValue()}"))
                    })
                }),
              20 seconds
            )
            val r = Await.result(QueryApi
                                   .access()
                                   .hostname("localhost")
                                   .port(configurator.port)
                                   .query(RdbQuery(db.url, db.user, db.password, None, None, Some(creation.name))),
                                 10 seconds)
            r.tables.size shouldBe 1
            r.tables.head.name shouldBe creation.name
            r.tables.head.schema.size shouldBe creation.schema.size
            r.tables.head.schema.foreach { lhs =>
              val e = creation.schema.find(_.name == lhs.name).get
              e.pk shouldBe lhs.pk
            // the following check is disabled because different database may use different name to describe data type...
            // e.typeName shouldBe lhs.typeName
            }
        } finally actorSystem.terminate()
      }
    )
  }
}
