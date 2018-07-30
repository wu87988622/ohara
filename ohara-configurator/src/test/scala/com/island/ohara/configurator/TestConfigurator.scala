package com.island.ohara.configurator

import java.util.UUID
import java.util.concurrent.{Executors, TimeUnit}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json._
import akka.stream.ActorMaterializer
import com.island.ohara.config.{OharaConfig, OharaJson}
import com.island.ohara.configurator.data._
import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce._
import com.island.ohara.rest.RestClient
import com.island.ohara.rule.MediumTest
import com.island.ohara.configurator.ConfiguratorImpl._
import com.island.ohara.serialization.{BYTES, INT, LONG, StringSerializer}
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class TestConfigurator extends MediumTest with Matchers with ConfiguratorJsonSupport with SprayJsonSupport {

  val topicPath = s"/${Configurator.VERSION}/${Configurator.TOPIC_PATH}"
  private implicit val actorSystem = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = actorSystem.dispatcher

  @Test
  def testSchema(): Unit = {
    val store = Store.inMemory(StringSerializer, OharaDataSerializer)
    val schemaName = "testSchema"
    val schemaType = Map("cf1" -> BYTES, "cf2" -> INT)
    val schemaIndex = Map("cf1" -> 1, "cf2" -> 2)
    val disabled = false
    val schema = OharaSchema.json(schemaName, schemaType, schemaIndex, disabled)
    val uuid = System.currentTimeMillis().toString
    val path = s"${Configurator.VERSION}/${Configurator.SCHEMA_PATH}"
    doClose(Configurator.builder.noCluster.uuidGenerator(() => uuid).hostname("localhost").port(0).store(store).build()) {
      configurator =>
        {
          doClose(RestClient(configurator.hostname, configurator.port)) { client =>
            {
              var response = client.post(path, schema)
              response.statusCode shouldBe 200
              response.body.indexOf(uuid) should not be -1
              configurator.schemas.size shouldBe 1
              configurator.schemas.next().name shouldBe schemaName
              configurator.schemas.next().types.foreach {
                case (name, t) => schemaType.get(name).get shouldBe t
              }

              response = client.get(s"$path/$uuid")
              response.statusCode shouldBe 200
              var returnedSchema = OharaSchema(OharaJson(response.body))
              configurator.schemas.next().types.foreach {
                case (name, t) => returnedSchema.types.get(name).get shouldBe t
              }

              response = client.delete(s"$path/phantom")
              response.statusCode shouldBe 400

              val newName = "testSchema"
              val newTypes = Map("cf3" -> BYTES, "cf2" -> LONG)
              val newIndexes = Map("cf3" -> 1, "cf2" -> 2)
              val newDisabled = true
              val newSchema = OharaSchema.json(newName, newTypes, newIndexes, newDisabled)
              response = client.put(s"$path/$uuid", newSchema)
              response.statusCode shouldBe 200

              response = client.get(s"$path/$uuid")
              response.statusCode shouldBe 200
              returnedSchema = OharaSchema(OharaJson(response.body))
              configurator.schemas.next().equals(returnedSchema, false) shouldBe true

              response = client.delete(s"$path/$uuid")
              response.statusCode shouldBe 200

              // we can't update a nonexistant schema
              response = client.put(s"$path/$uuid", newSchema)
              response.statusCode shouldBe 400

              response = client.get(s"$path/$uuid")
              response.statusCode shouldBe 400

              configurator.schemas.size shouldBe 0

              // add other ohara data
              val anotherUuid = (System.currentTimeMillis() + 100).toString
              val anotherName = "invalidschema"
              store.update(anotherUuid, OharaTopic.apply(anotherUuid, anotherName, 1, 1))
              response = client.get(s"$path/$anotherUuid")
              response.statusCode shouldBe 400
            }
          }
        }
    }
  }

  @Test
  def testSchemaIterator(): Unit = {
    val schemaCount = 100
    val schemas = (0 until schemaCount).map { index =>
      {
        OharaSchema.json(index.toString, Map(index.toString -> BYTES), Map(index.toString -> index), false)
      }
    }
    val path = s"${Configurator.VERSION}/${Configurator.SCHEMA_PATH}"
    doClose(Configurator.builder.noCluster.hostname("localhost").port(0).build()) { configurator =>
      {
        doClose(RestClient(configurator.hostname, configurator.port)) { client =>
          schemas.foreach(client.post(path, _).statusCode shouldBe 200)
        }
        configurator.schemas.size shouldBe schemaCount
      }
    }
  }

  @Test
  def testListSchemaUuid(): Unit = {
    val schemaCount = 10
    val uuids: Seq[String] = (0 until schemaCount).map(_.toString)
    var uuidIndex = 0
    val schemas: Seq[OharaJson] =
      (0 until schemaCount).map(index => OharaSchema.json(index.toString, Map("cf" -> BYTES), Map("cf" -> 1), false))
    val path = s"${Configurator.VERSION}/${Configurator.SCHEMA_PATH}"
    doClose(
      Configurator.builder.noCluster
        .uuidGenerator(() => {
          uuidIndex <= uuids.size shouldBe true
          try uuids(uuidIndex)
          finally uuidIndex += 1
        })
        .hostname("localhost")
        .port(0)
        .build()) { configurator =>
      {
        doClose(RestClient(configurator.hostname, configurator.port)) { client =>
          {
            schemas.zipWithIndex.foreach {
              case (schema, index) => {
                val response = client.post(path, schema)
                response.statusCode shouldBe 200
                response.body.indexOf(uuids(index)) should not be -1
              }
            }
            // test list
            val response = client.get(path)
            response.statusCode shouldBe 200
            val responsedUuids = OharaConfig(OharaJson(response.body)).getMap("uuids").get
            responsedUuids.size shouldBe uuids.size
            uuids.foreach(uuid => {
              // the uuid is equal with name
              responsedUuids.get(uuid).get shouldBe uuid
            })
          }
        }
      }
    }
  }

  @Test
  def testInvalidSchema(): Unit = {
    val path = s"${Configurator.VERSION}/${Configurator.SCHEMA_PATH}"
    doClose(Configurator.builder.noCluster.hostname("localhost").port(0).build()) { configurator =>
      {
        doClose(RestClient(configurator.hostname, configurator.port)) { client =>
          {
            var response = client.post(path, OharaJson("xxx"))
            response.statusCode shouldBe 400
            var exception = OharaException(OharaJson(response.body))
            exception.code.contains(classOf[IllegalArgumentException].getSimpleName)

            response = client.put(s"$path/12345", OharaJson("xxx"))
            response.statusCode shouldBe 400
            exception = OharaException(OharaJson(response.body))
            exception.code.contains(classOf[IllegalArgumentException].getSimpleName)
          }
        }
      }
    }
  }

  @Test
  def testInvalidMain(): Unit = {
    // enable this flag to make sure the instance of Configurator is always die.
    Configurator.closeRunningConfigurator = true
    try {
      an[IllegalArgumentException] should be thrownBy Configurator.main(Array[String]("localhost"))
      an[IllegalArgumentException] should be thrownBy Configurator.main(
        Array[String]("localhost", "localhost", "localhost"))
      an[IllegalArgumentException] should be thrownBy Configurator.main(
        Array[String](Configurator.HOSTNAME_KEY, "localhost", Configurator.PORT_KEY, "123", Configurator.TOPIC_KEY))
      an[IllegalArgumentException] should be thrownBy Configurator.main(
        Array[String](Configurator.HOSTNAME_KEY,
                      "localhost",
                      Configurator.PORT_KEY,
                      "123",
                      Configurator.TOPIC_KEY,
                      "topic"))
    } finally Configurator.closeRunningConfigurator = false
  }

  @Test
  def testMain(): Unit = {
    def runStandalone() = {
      Configurator.closeRunningConfigurator = false
      val service = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
      Future[Unit] {
        Configurator.main(Array[String](Configurator.HOSTNAME_KEY, "localhost", Configurator.PORT_KEY, "0"))
      }(service)
      import scala.concurrent.duration._
      try OharaTestUtil.await(() => Configurator.hasRunningConfigurator, 10 seconds)
      finally {
        Configurator.closeRunningConfigurator = true
        service.shutdownNow()
        service.awaitTermination(60, TimeUnit.SECONDS)
      }
    }

    def runDist() = {
      doClose(OharaTestUtil.localBrokers(3)) { util =>
        {
          Configurator.closeRunningConfigurator = false
          val service = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
          Future[Unit] {
            Configurator.main(
              Array[String](
                Configurator.HOSTNAME_KEY,
                "localhost",
                Configurator.PORT_KEY,
                "0",
                Configurator.BROKERS_KEY,
                util.brokersString,
                Configurator.TOPIC_KEY,
                methodName
              ))
          }(service)
          import scala.concurrent.duration._
          try OharaTestUtil.await(() => Configurator.hasRunningConfigurator, 30 seconds)
          finally {
            Configurator.closeRunningConfigurator = true
            service.shutdownNow()
            service.awaitTermination(60, TimeUnit.SECONDS)
          }
        }
      }
    }

    runStandalone()
    runDist()
  }

  @Test
  def testCreateTopic(): Unit = {
    val path = s"${Configurator.VERSION}/${Configurator.TOPIC_PATH}"
    val uuid = "123"
    doClose(Configurator.builder.noCluster.uuidGenerator(() => uuid).hostname("localhost").port(0).build()) {
      configurator =>
        {
          doClose(RestClient(configurator.hostname, configurator.port)) { client =>
            {
              val response =
                client.post(path,
                            OharaJson("{\"name\":my_topic, \"numberOfPartitions\":1, \"numberOfReplications\":1}"))
              withClue(s" body:${response.body}")(response.statusCode shouldBe 200)
              response.body shouldBe "{\"uuid\":\"123\"}"
            }
          }
        }
    }
  }

  @Test
  def getTopicShouldResponse(): Unit = {

    doClose(Configurator.builder.noCluster.hostname("localhost").port(0).build()) { configurator =>
      {

        // create topic
        val originTopicName = "5566"

        val createTopic = CreateTopic(originTopicName, 1, 1)

        val futureResponse = for {
          requestEntity <- Marshal(createTopic).to[RequestEntity]
          response <- Http().singleRequest(
            HttpRequest(method = HttpMethods.POST,
                        uri = "http://" + configurator.hostname + ":" + configurator.port + topicPath,
                        entity = requestEntity))
        } yield response

        val createTopicResponse = Await.result(futureResponse, 3.seconds)
        createTopicResponse.status shouldBe StatusCodes.OK

        val uuidResponse = Await.result(Unmarshal(createTopicResponse.entity).to[UuidResponse], 3 second)

        // get topic
        val request =
          HttpRequest(
            method = HttpMethods.GET,
            uri = "http://" + configurator.hostname + ":" + configurator.port + topicPath + "/" + uuidResponse.uuid)
        val responseFuture = Http().singleRequest(request)
        val response = Await.result(responseFuture, 3.seconds)

        response.status shouldBe StatusCodes.OK
        response.entity shouldBe HttpEntity(
          ContentTypes.`application/json`,
          GetTopicResponse(uuidResponse.uuid,
                           createTopic.name,
                           createTopic.numberOfPartitions,
                           createTopic.numberOfReplications).toJson.toString
        )
      }
    }
  }

  @Test
  def deleteTopic(): Unit = {

    doClose(Configurator.builder.noCluster.hostname("localhost").port(0).build()) { configurator =>
      {

        // create topic
        val originTopicName = "5566"

        val createTopic = CreateTopic(originTopicName, 1, 1)

        val futureResponse = for {
          requestEntity <- Marshal(createTopic).to[RequestEntity]
          response <- Http().singleRequest(
            HttpRequest(method = HttpMethods.POST,
                        uri = "http://" + configurator.hostname + ":" + configurator.port + topicPath,
                        entity = requestEntity))
        } yield response

        val createTopicResponse = Await.result(futureResponse, 3.seconds)
        createTopicResponse.status shouldBe StatusCodes.OK

        val uuidResponse = Await.result(Unmarshal(createTopicResponse.entity).to[UuidResponse], 3 second)

        // delete topic
        val request =
          HttpRequest(
            method = HttpMethods.DELETE,
            uri = "http://" + configurator.hostname + ":" + configurator.port + topicPath + "/" + uuidResponse.uuid)
        val responseFuture = Http().singleRequest(request)
        val response = Await.result(responseFuture, 3.seconds)

        response.status shouldBe StatusCodes.OK
        response.entity shouldBe HttpEntity(
          ContentTypes.`application/json`,
          GetTopicResponse(uuidResponse.uuid,
                           createTopic.name,
                           createTopic.numberOfPartitions,
                           createTopic.numberOfReplications).toJson.toString
        )
      }
    }
  }

  @Test
  def getTopicDidntExist(): Unit = {

    doClose(Configurator.builder.noCluster.hostname("localhost").port(0).build()) { configurator =>
      {
        val request =
          HttpRequest(
            method = HttpMethods.GET,
            uri = "http://" + configurator.hostname + ":" + configurator.port + topicPath + "/" + UUID.randomUUID())
        val responseFuture = Http().singleRequest(request)
        val response = Await.result(responseFuture, 3.seconds)

        response.status shouldBe StatusCodes.OK
        response.entity shouldBe HttpEntity(
          ContentTypes.`application/json`,
          TOPIC_IS_NOT_FOUND.toJson.toString
        )
      }
    }
  }

  @Test
  def deleteTopicDidntExist(): Unit = {

    doClose(Configurator.builder.noCluster.hostname("localhost").port(0).build()) { configurator =>
      {
        val request =
          HttpRequest(
            method = HttpMethods.DELETE,
            uri = "http://" + configurator.hostname + ":" + configurator.port + topicPath + "/" + UUID.randomUUID())
        val responseFuture = Http().singleRequest(request)
        val response = Await.result(responseFuture, 3.seconds)

        response.status shouldBe StatusCodes.OK
        response.entity shouldBe HttpEntity(
          ContentTypes.`application/json`,
          TOPIC_IS_NOT_FOUND.toJson.toString
        )
      }
    }
  }

  @Test
  def testListTopicUuid(): Unit = {
    val topicCount = 10
    val uuids: Seq[String] = (0 until topicCount).map(_.toString)
    var uuidIndex = 0
    val topics: Seq[OharaJson] =
      (0 until topicCount).map(index => OharaTopic.json(index.toString, 1, 1))
    val path = s"${Configurator.VERSION}/${Configurator.TOPIC_PATH}"
    doClose(
      Configurator.builder.noCluster
        .uuidGenerator(() => {
          uuidIndex <= uuids.size shouldBe true
          try uuids(uuidIndex)
          finally uuidIndex += 1
        })
        .hostname("localhost")
        .port(0)
        .build()) { configurator =>
      {
        doClose(RestClient(configurator.hostname, configurator.port)) { client =>
          {
            topics.zipWithIndex.foreach {
              case (topic, index) => {
                val response = client.post(path, topic)
                response.statusCode shouldBe 200
                response.body.indexOf(uuids(index)) should not be -1
              }
            }
            // test list
            val response = client.get(path)
            response.statusCode shouldBe 200
            val responsedUuids = OharaConfig(OharaJson(response.body)).getMap("uuids").get
            responsedUuids.size shouldBe uuids.size
            uuids.foreach(uuid => {
              // the uuid is equal with name
              responsedUuids.get(uuid).get shouldBe uuid
            })
          }
        }
      }
    }
  }
}
