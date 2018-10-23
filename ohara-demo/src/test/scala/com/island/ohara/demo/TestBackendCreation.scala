package com.island.ohara.demo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, RequestEntity}
import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.demo.Backend.{Creation, _}
import com.island.ohara.rule.LargeTest
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

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
      0,
      (configurator, db, _) => {
        implicit val actorSystem: ActorSystem = ActorSystem(methodName)
        try creations.foreach(creation => {
          Await.result(
            Marshal(creation)
              .to[RequestEntity]
              .flatMap(entity => {
                Http()
                  .singleRequest(
                    HttpRequest(HttpMethods.POST,
                                s"http://localhost:${configurator.port}/$PRIVATE_API/creation/rdb",
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
          val client = ConfiguratorClient("localhost", configurator.port)
          try {
            val r = client.query[RdbQuery, RdbInformation](
              RdbQuery(db.url, db.user, db.password, None, None, Some(creation.name)))
            r.tables.size shouldBe 1
            r.tables.head.name shouldBe creation.name
            r.tables.head.schema.size shouldBe creation.schema.size
            r.tables.head.schema.foreach(lhs => {
              val e = creation.schema.find(_.name == lhs.name).get
              e.pk shouldBe lhs.pk
              // the following check is disabled because different database may use different name to describe data type...
              // e.typeName shouldBe lhs.typeName
            })
          } finally client.close()
        })
        finally actorSystem.terminate()
      }
    )
  }
}
