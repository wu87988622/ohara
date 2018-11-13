package com.island.ohara.source.http

import java.util.concurrent.ConcurrentHashMap

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.island.ohara.common.data.DataType._
import com.island.ohara.common.data.Serializer
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.kafka.{Consumer, KafkaUtil, Producer}
import org.junit.{After, Test}
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitSuiteLike

import scala.concurrent.duration._

class TestKafkaRouteWithMiniCluster
    extends JUnitSuiteLike
    with ScalaFutures
    with ScalatestRouteTest
    with KafkaRoute
    with Matchers {
  val schema = Vector(("name", STRING), ("year", INT), ("month", SHORT), ("isHuman", BOOLEAN))
  val map: ConcurrentHashMap[String, (String, RowSchema)] = new ConcurrentHashMap[String, (String, RowSchema)]() {
    this.put("test", ("test", RowSchema(schema)))
  }

  /**
    * we can't use With3Brokers3Workers since With3Brokers3Workers is not a "trait" now. It is illegal to extend multi
    * abstract classes...by chia
    */
  private[this] val testUtil = OharaTestUtil.workers()

  @Test
  def shouldReturnNotFoundWhenUrlNotExist(): Unit = {
    val request = HttpRequest(uri = "/abcd")
    val producer =
      Producer.builder().brokers(testUtil.brokersConnProps).allAcks().build(Serializer.STRING, Serializer.ROW)

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
    val producer =
      Producer.builder().brokers(testUtil.brokersConnProps).allAcks().build(Serializer.STRING, Serializer.ROW)

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

    if (!KafkaUtil.exist(testUtil.brokersConnProps, topic))
      KafkaUtil.createTopic(testUtil.brokersConnProps, topic, 1, 1)

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
      Producer.builder().brokers(testUtil.brokersConnProps).allAcks().build(Serializer.STRING, Serializer.ROW)

    try {
      Post(s"/$url", HttpEntity(ContentTypes.`application/json`, jsonString)) ~> Route.seal(kafkaRoute(producer, map)) ~> check {

        val consumer =
          Consumer
            .builder()
            .brokers(testUtil.brokersConnProps)
            .offsetFromBegin()
            .topicName(topic)
            .build(Serializer.STRING, Serializer.ROW)
        try {
          val fromKafka = consumer.poll(30 seconds, 1)
          fromKafka.isEmpty shouldBe false
          val row = fromKafka.head.value.get
          status should ===(StatusCodes.OK)
          for (((value, (colName, _)), i) <- (csv zip schema).zipWithIndex) {
            row.cell(i).name shouldBe colName
            row.cell(i).value shouldBe value
          }
        } finally consumer.close()
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

    if (!KafkaUtil.exist(testUtil.brokersConnProps, topic))
      KafkaUtil.createTopic(testUtil.brokersConnProps, topic, 1, 1)

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
      Producer.builder().brokers(testUtil.brokersConnProps).allAcks().build(Serializer.STRING, Serializer.ROW)

    try {
      Post(s"/$url", HttpEntity(ContentTypes.`application/json`, jsonString)) ~> Route.seal(kafkaRoute(producer, map)) ~> check {
        status should ===(StatusCodes.BadRequest)
      }
    } finally producer.close()
  }

  @After
  def tearDown(): Unit = {
    testUtil.close()
  }
}
