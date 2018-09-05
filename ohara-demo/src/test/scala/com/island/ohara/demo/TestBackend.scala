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

class TestBackend extends LargeTest with Matchers {

  @Test
  def testTtl(): Unit = {
    val f = Future {
      Backend.main(Array(Backend.TTL_KEY, "1"))
    }
    // we have to wait all service to be closed so 60 seconds is a safe limit.
    Await.result(f, 60 seconds)
  }

  @Test
  def testConfigurator(): Unit = {
    Backend.run(
      0,
      (configurator, db) => {
        val client = ConfiguratorClient("localhost", configurator.port)
        try {
          // it should pass
          client.cluster[ClusterInformation]
        } finally client.close()
      }
    )
  }

  @Test
  def testCreation(): Unit = {
    Backend.run(
      0,
      (configurator, db) => {
        val creation = Creation(methodName, Seq(RdbColumn("cf", "integer", true)))
        implicit val actorSystem: ActorSystem = ActorSystem(methodName)
        try {
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
            10 seconds
          )
        } finally actorSystem.terminate()
        val client = ConfiguratorClient("localhost", configurator.port)
        try {
          val r = client.query[RdbQuery, RdbInformation](RdbQuery(db.url, db.user, db.password, None, None, None))
          r.tables.size shouldBe 1
          r.tables.head.name shouldBe creation.name
          r.tables.head.columns.size shouldBe creation.schema.size
          r.tables.head.columns.head.name shouldBe creation.schema.head.name
          // the following check is disabled because different database may use different name to describe data type...
          // r.tables.head.columns.head.typeName shouldBe creation.schema.head.typeName
          r.tables.head.columns.head.pk shouldBe creation.schema.head.pk
        } finally client.close()
      }
    )
  }
}
