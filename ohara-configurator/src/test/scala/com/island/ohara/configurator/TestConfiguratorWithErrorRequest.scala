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
import com.island.ohara.io.CloseOnce
import com.island.ohara.rule.SmallTest
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

  private[this] val configurator =
    Configurator.builder().hostname("localhost").port(0).noCluster.build()

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
  }

  object ExceptionType {
    type parse_exception = spray.json.DeserializationException
    type error_exception = IllegalArgumentException
  }

  @Test
  def dataTest(): Unit = {

    def verify[T](implicit formatter: DataCommandFormat[T]): Unit = {
      val domain = ip
      val uuid = random()
      val url = formatter.format(domain)
      val url2 = formatter.format(domain, uuid)

      an[ExceptionType.parse_exception] should be thrownBy TestClient
        .doRequest[MyRequest](url, request, HttpMethods.GET)
      //add
      an[ExceptionType.error_exception] should be thrownBy TestClient
        .doRequest[MyRequest](url, request, HttpMethods.POST)
      //get
      an[ExceptionType.error_exception] should be thrownBy TestClient
        .doRequest[MyRequest](url2, request, HttpMethods.GET)
      //delete
      an[ExceptionType.error_exception] should be thrownBy TestClient
        .doRequest[MyRequest](url2, request, HttpMethods.DELETE)
      //update
      an[ExceptionType.error_exception] should be thrownBy TestClient
        .doRequest[MyRequest](url2, request, HttpMethods.PUT)
    }
    verify[TopicInfo]
    verify[HdfsInformation]
    verify[FtpInformation]
    verify[JdbcInformation]
    verify[Pipeline]
    verify[Sink]
    verify[Source]

  }

  @Test
  def validateTest(): Unit = {

    def verify[T](implicit formatter: ValidationCommandFormat[T]): Unit = {
      val domain = ip
      val uuid = random()
      val url = formatter.format(domain)
      //validate
      an[ExceptionType.error_exception] should be thrownBy TestClient
        .doRequest[MyRequest](url, request, HttpMethods.PUT)
    }
    verify[RdbValidationRequest]
    verify[HdfsValidationRequest]
    verify[RdbValidationRequest]

  }
  @Test
  def clusterTest(): Unit = {

    def verify[T](implicit formatter: ClusterCommandFormat[T]): Unit = {
      val domain = ip
      val uuid = random()

      val url = formatter.format(domain)
      //cluster
      an[ExceptionType.parse_exception] should be thrownBy TestClient
        .doRequest[MyRequest](url, request, HttpMethods.GET)
    }
    verify[ClusterInformation]

  }
  @Test
  def controlCommandTest(): Unit = {

    def verify[T](implicit formatter: ControlCommandFormat[T]): Unit = {
      val domain = ip
      val uuid = random()

      val url = formatter.start(domain, uuid)
      val url2 = formatter.stop(domain, uuid)
      //start
      an[ExceptionType.error_exception] should be thrownBy TestClient
        .doRequest[MyRequest](url, request, HttpMethods.PUT)
      //stop
      an[ExceptionType.error_exception] should be thrownBy TestClient
        .doRequest[MyRequest](url2, request, HttpMethods.PUT)
    }
    verify[Pipeline]
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

  @Test
  def errorRequestWithConfiguratorClient(): Unit = {

    an[IllegalArgumentException] should be thrownBy client.add[MyRequest, TopicInfo](request)
    an[IllegalArgumentException] should be thrownBy client.add[MyRequest, HdfsInformation](request)
    an[IllegalArgumentException] should be thrownBy client.add[MyRequest, Pipeline](request)
    an[IllegalArgumentException] should be thrownBy client.add[MyRequest, Sink](request)
    an[IllegalArgumentException] should be thrownBy client.add[MyRequest, Source](request)

    an[IllegalArgumentException] should be thrownBy client
      .update[MyRequest, TopicInfo](Long.MaxValue.toString(), request)
    an[IllegalArgumentException] should be thrownBy client
      .update[MyRequest, HdfsInformation](Long.MaxValue.toString(), request)
    an[IllegalArgumentException] should be thrownBy client
      .update[MyRequest, Pipeline](Long.MaxValue.toString(), request)
    an[IllegalArgumentException] should be thrownBy client.update[MyRequest, Sink](Long.MaxValue.toString(), request)
    an[IllegalArgumentException] should be thrownBy client.update[MyRequest, Source](Long.MaxValue.toString(), request)
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(client)
    CloseOnce.close(configurator)
    TestClient.actorSystem.terminate()
  }
}

private final case class MyRequest(s: String, s2: Int, s3: String)
