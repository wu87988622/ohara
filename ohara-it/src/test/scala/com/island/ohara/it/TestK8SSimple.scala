package com.island.ohara.it

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.it.TestK8SSimple.API_SERVER_URL
import com.typesafe.scalalogging.Logger
import org.junit._
import org.scalatest.Matchers
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class TestK8SSimple extends SmallTest with Matchers {
  private val log = Logger(classOf[TestK8SSimple])

  implicit val actorSystem: ActorSystem = ActorSystem("TestK8SServer")
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  @Before
  def before(): Unit = {
    val message = s"The k8s is skip test, Please setting ${TestK8SSimple.K8S_API_SERVER_URL} properties"
    if (API_SERVER_URL.isEmpty) {
      log.info(message)
      skipTest(message)
    }
  }

  @Test
  def testK8SNode(): Unit = {
    val futureResponse = Http().singleRequest(HttpRequest(uri = s"${TestK8SSimple.API_SERVER_URL.get}/nodes"))
    ScalaFutures.whenReady(futureResponse, timeout = Timeout(Span(2, Seconds))) { response =>
      response.status shouldBe StatusCodes.OK
      val result = Await.result(Unmarshal(response.entity).to[String], 1 second)
      assert(result.contains("machineID"))
    }
  }

  @Test
  def testPropEmpty(): Unit = {
    val apiServerURL: Option[String] = sys.env.get(TestK8SSimple.K8S_API_SERVER_URL)
    apiServerURL should not be empty
  }
}

object TestK8SSimple {
  val K8S_API_SERVER_URL: String = "ohara.it.k8s"
  val API_SERVER_URL: Option[String] = sys.env.get(K8S_API_SERVER_URL)

  @BeforeClass
  def beforeClass(): Unit = {
    //TODO Create NameSpace
  }

  @AfterClass
  def afterClass(): Unit = {
    //TODO DELETE NameSpace
  }
}
