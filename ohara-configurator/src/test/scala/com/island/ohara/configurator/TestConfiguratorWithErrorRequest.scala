package com.island.ohara.configurator

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.client.ConfiguratorClient
import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.configurator.v0.ErrorApi._
import com.island.ohara.common.rule.SmallTest
import com.island.ohara.common.util.ReleaseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers
import spray.json.DefaultJsonProtocol._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * test configurator with error request
  *
  */
class TestConfiguratorWithErrorRequest extends SmallTest with Matchers {

  private[this] val configurator = Configurator.local()

  private[this] val ip = s"${configurator.hostname}:${configurator.port}"

  private[this] val client = ConfiguratorClient(ip)

  private[this] implicit val MY_REQUEST_JSON_FORMAT: RootJsonFormat[MyRequest] = jsonFormat3(MyRequest)

  private[this] val request = MyRequest("5", 5, "testttt")

  private[this] object TestClient extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val actorSystem: ActorSystem = ActorSystem("TestConfigurator")
    implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
    val TIMEOUT: Duration = 10 seconds
    def unmarshal[T](res: HttpResponse)(implicit rm: RootJsonFormat[T]): Future[T] =
      if (res.status.isSuccess()) Unmarshal(res.entity).to[T]
      else
        Unmarshal(res.entity).to[Error].flatMap(error => Future.failed(new IllegalArgumentException(error.message)))

    def doRequest[T](url: String, req: T, method: HttpMethod)(implicit rm: RootJsonFormat[T]): T = {
      //list
      Await.result(
        Marshal(req)
          .to[RequestEntity]
          .flatMap(entity => {
            Http().singleRequest(HttpRequest(method, url, entity = entity)).flatMap(unmarshal[T])

          }),
        TIMEOUT
      )
    }
    def doRequestWithEntity[T](url: String, entity: RequestEntity, method: HttpMethod)(
      implicit rm: RootJsonFormat[T]): T = {
      Await.result(Http().singleRequest(HttpRequest(method, url, entity = entity)).flatMap(unmarshal[T]), TIMEOUT)
    }
  }

  object ExceptionType {
    type parse_exception = spray.json.DeserializationException
    type error_exception = IllegalArgumentException
  }

  @Test
  def queryCommandTest(): Unit = {

    def verify[T](implicit formatter: QueryCommandFormat[T]): Unit = {
      val domain = ip
      val uuid = random()
      val url = formatter.format(domain)
      //query
      an[ExceptionType.error_exception] should be thrownBy TestClient
        .doRequest[MyRequest](url, request, HttpMethods.POST)
    }
    verify[RdbQuery]
  }

  @After
  def tearDown(): Unit = {
    ReleaseOnce.close(client)
    ReleaseOnce.close(configurator)
    TestClient.actorSystem.terminate()
  }
}

private final case class MyRequest(s: String, s2: Int, s3: String)
