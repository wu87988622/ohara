package com.island.ohara.manager.sample

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.island.ohara.rule.SmallTest
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitSuiteLike
import org.junit.Test

// TODO:
//  1. Please extend either SmallTest or MediumTest. With our test catalog, the RunWith is redundant.
//     Also, it requires the junit-style.
class TestUserRoutes extends SmallTest with UserRoutes with Matchers with ScalaFutures with ScalatestRouteTest {

  override lazy val userRegistryActor: ActorRef = system.actorOf(UserRegistryActor.props, "userRegistry")
  lazy val routes = userRoutes

  @Test
  def testReturnNoUsersIfNotPresent(): Unit = {
    val request = HttpRequest(uri = "/users")
    request ~> routes ~> check {
      status should ===(StatusCodes.OK)
      contentType should ===(ContentTypes.`application/json`)
      entityAs[String] should ===("""{"users":[]}""")
    }
  }

}
