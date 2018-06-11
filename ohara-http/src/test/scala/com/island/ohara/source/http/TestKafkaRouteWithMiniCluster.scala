package com.island.ohara.source.http

import java.io.StringReader
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.island.ohara.core.Row
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.KafkaUtil
import com.island.ohara.rule.MediumTest
import com.island.ohara.serialization._
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerConfig}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class TestKafkaRouteWithMiniCluster
    extends MediumTest
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with KafkaRoute {

  val schema = Vector(("name", STRING), ("year", INT), ("month", SHORT), ("isHuman", BOOLEAN))
  val map = new ConcurrentHashMap[String, (String, RowSchema)]() {
    this.put("test", ("test", RowSchema(schema)))
  }

  private def buildProducer(servers: String): Producer[String, Row] = {
    val prop = new Properties()
    val configString =
      s"""
         |${ProducerConfig.ACKS_CONFIG}=all
         |${ProducerConfig.BOOTSTRAP_SERVERS_CONFIG}=$servers
       """.stripMargin
    prop.load(new StringReader(configString))
    new KafkaProducer[String, Row](prop, new StringSerializer, KafkaUtil.wrapSerializer(RowSerializer))
  }

  @Test
  def shouldReturnNotFoundWhenUrlNotExist(): Unit = {
    doClose(new OharaTestUtil(3)) { testUtil =>
      val request = HttpRequest(uri = "/abcd")
      val producer =
        buildProducer(testUtil.producerConfig.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG).get.left.get)

      try {
        val route = kafkaRoute(producer, map)
        request ~> Route.seal(route) ~> check {
          status should ===(StatusCodes.NotFound)
        }
      } finally producer.close

    }
  }

  @Test
  def shouldReturnOkWhenUrlExist(): Unit = {
    doClose(new OharaTestUtil(3)) { testUtil =>
      val request = HttpRequest(uri = "/test")
      val producer =
        buildProducer(testUtil.producerConfig.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG).get.left.get)

      try {
        val route = kafkaRoute(producer, map)
        request ~> Route.seal(route) ~> check {
          status should ===(StatusCodes.OK)
        }
      } finally producer.close
    }
  }

  @Test
  def shouldReceiveSameMessageWhenPostRowToHttpServer(): Unit = {

    doClose(new OharaTestUtil(3)) { testUtil =>
      val url, topic = "test"
      val name = "John Doe"
      val year: Int = 2018
      val month: Short = 8
      val isHuman = true
      val csv = List(name, year, month, isHuman)

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

      val producer =
        buildProducer(testUtil.producerConfig.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG).get.left.get)

      try {
        Post(s"/$url", HttpEntity(ContentTypes.`application/json`, jsonString)) ~> Route.seal(kafkaRoute(producer, map)) ~> check {

          val (_, valueQueue) =
            testUtil.run(topic, true, new StringDeserializer, KafkaUtil.wrapDeserializer(RowSerializer))
          testUtil.await(() => valueQueue.size() == 1, 10 seconds)
          val row = valueQueue.take()

          status should ===(StatusCodes.OK)
          for (((value, (colName, _)), i) <- (csv zip schema).zipWithIndex) {
            row.seekCell(i).name shouldBe colName
            row.seekCell(i).value shouldBe value
          }
        }
      } finally producer.close
    }
  }

  @Test
  def shouldReturnBadRequestWhenCsvSizeDidntFit(): Unit = {

    doClose(new OharaTestUtil(3)) { testUtil =>
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

      val producer =
        buildProducer(testUtil.producerConfig.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG).get.left.get)

      try {
        Post(s"/$url", HttpEntity(ContentTypes.`application/json`, jsonString)) ~> Route.seal(kafkaRoute(producer, map)) ~> check {
          status should ===(StatusCodes.BadRequest)
        }
      } finally producer.close
    }
  }
}
