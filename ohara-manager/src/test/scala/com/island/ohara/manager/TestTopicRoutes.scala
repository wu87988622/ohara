package com.island.ohara.manager

import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.island.ohara.rule.SmallTest
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import spray.json._
import com.island.ohara.manager.TopicRoutes._

import scala.concurrent.Future

class TestTopicRoutes
    extends SmallTest
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with MockitoSugar
    with TopicJsonSupport
    with SprayJsonSupport {

  @Test
  def configuratorReturnUUID(): Unit = {
    val configuratorService = mock[ConfiguratorService]
    val post = CreateTopic("123", 1, 1)

    val uuid = UUID.randomUUID().toString
    when(configuratorService.createTopic(post)).thenReturn(Future.successful(Right(CreateTopicSuccess(uuid))))

    val routes = new TopicRoutes(configuratorService).routes

    val request = HttpEntity(MediaTypes.`application/json`, post.toJson.toString)
    Post("/topics", request) ~> Route.seal(routes) ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[CreateTopicResponse] shouldEqual CreateTopicResponse(true, EMPTY_STRING, uuid)
    }
  }

  @Test
  def configuratorReturnEmpty(): Unit = {
    val configuratorService = mock[ConfiguratorService]
    val post = CreateTopic("123", 1, 1)
    val stackMessage = "Message from stack trace"
    when(configuratorService.createTopic(post))
      .thenReturn(Future.successful(Left(CreateTopicFailure("", "", stackMessage))))

    val routes = new TopicRoutes(configuratorService).routes

    val request = HttpEntity(MediaTypes.`application/json`, post.toJson.toString)
    Post("/topics", request) ~> Route.seal(routes) ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[CreateTopicResponse] shouldEqual CreateTopicResponse(false, stackMessage, EMPTY_UUID)
    }
  }
}
