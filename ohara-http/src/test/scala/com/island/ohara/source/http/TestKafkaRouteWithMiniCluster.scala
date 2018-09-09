package com.island.ohara.source.http

import java.util.concurrent.ConcurrentHashMap

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.island.ohara.data.Row
import com.island.ohara.integration.{OharaTestUtil, With3Brokers}
import com.island.ohara.kafka.{KafkaUtil, Producer}
import com.island.ohara.serialization.DataType._
import com.island.ohara.serialization._
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class TestKafkaRouteWithMiniCluster
    extends With3Brokers
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with KafkaRoute {

  val schema = Vector(("name", STRING), ("year", INT), ("month", SHORT), ("isHuman", BOOLEAN))
  val map: ConcurrentHashMap[String, (String, RowSchema)] = new ConcurrentHashMap[String, (String, RowSchema)]() {
    this.put("test", ("test", RowSchema(schema)))
  }

  @Test
  def shouldReturnNotFoundWhenUrlNotExist(): Unit = {
    val request = HttpRequest(uri = "/abcd")
    val producer = Producer.builder().brokers(testUtil.brokers).allAcks().build[String, Row]

    try {
      val route = kafkaRoute(producer, map)
      request ~> Route.seal(route) ~> check {
        status should ===(StatusCodes.NotFound)
      }
    } finally producer.close()
  }

  @Test
  def shouldReturnOkWhenUrlExist(): Unit = {
    val request = HttpRequest(uri = "/test")
    val producer = Producer.builder().brokers(testUtil.brokers).allAcks().build[String, Row]

    try {
      val route = kafkaRoute(producer, map)
      request ~> Route.seal(route) ~> check {
        status should ===(StatusCodes.OK)
      }
    } finally producer.close()
  }

  @Test
  def shouldReceiveSameMessageWhenPostRowToHttpServer(): Unit = {
    val url, topic = "test"
    val name = "John Doe"
    val year: Int = 2018
    val month: Short = 8
    val isHuman = true
    val csv = List[Any](name, year, month, isHuman)

    testUtil.createTopic(topic)

    val jsonString =
      s"""
        |{
        |   "row":[
        |      "$name",
        |      $year,
        |      $month,
        |      $isHuman
        |   ]
        |}
      """.stripMargin

    val producer = Producer.builder().brokers(testUtil.brokers).allAcks().build[String, Row]

    try {
      Post(s"/$url", HttpEntity(ContentTypes.`application/json`, jsonString)) ~> Route.seal(kafkaRoute(producer, map)) ~> check {

        val (_, valueQueue) =
          testUtil.run(topic, true, new StringDeserializer, KafkaUtil.wrapDeserializer(RowSerializer))
        OharaTestUtil.await(() => valueQueue.size() == 1, 10 seconds)
        val row = valueQueue.take()

        status should ===(StatusCodes.OK)
        for (((value, (colName, _)), i) <- (csv zip schema).zipWithIndex) {
          row.cell(i).name shouldBe colName
          row.cell(i).value shouldBe value
        }
      }
    } finally producer.close()
  }

  @Test
  def shouldReturnBadRequestWhenCsvSizeDidntFit(): Unit = {
    val url, topic = "test"
    val name = "John Doe"
    val year: Int = 2018
    val month: Short = 8
    val isHuman = true

    testUtil.createTopic(topic)

    val jsonString =
      s"""
         |{
         |   "row":[
         |      "$name",
         |      $year,
         |      $month,
         |      $isHuman,
         |      intention-to-fail
         |   ]
         |}
      """.stripMargin

    val producer = Producer.builder().brokers(testUtil.brokers).allAcks().build[String, Row]

    try {
      Post(s"/$url", HttpEntity(ContentTypes.`application/json`, jsonString)) ~> Route.seal(kafkaRoute(producer, map)) ~> check {
        status should ===(StatusCodes.BadRequest)
      }
    } finally producer.close()
  }
}
