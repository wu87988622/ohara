package com.island.ohara.configurator

import java.util.concurrent.{Executors, TimeUnit}

import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.{OharaTestUtil, With3Blockers3Workers}
import com.island.ohara.io.CloseOnce
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.KafkaClient
import com.island.ohara.rest.ConfiguratorJson._
import com.island.ohara.rest.{ConfiguratorClient, ConnectorClient}
import com.island.ohara.serialization.{DataType, Serializer}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{ExecutionContext, Future}

/**
  * this test includes two configurators - with/without cluster.
  * All test cases should work with all configurators.
  */
class TestConfigurator extends With3Blockers3Workers with Matchers {

  private[this] val configurator0 =
    Configurator.builder
      .hostname("localhost")
      .port(0)
      .store(
        Store
          .builder(Serializer.STRING, Serializer.OBJECT)
          .numberOfPartitions(1)
          .numberOfReplications(1)
          .topicName(classOf[TestConfigurator].getSimpleName)
          .brokers(testUtil.brokersString)
          .build())
      .kafkaClient(KafkaClient(testUtil.brokersString))
      .connectClient(ConnectorClient(testUtil.workersString))
      .build()

  private[this] val configurator1 =
    Configurator.builder.hostname("localhost").port(0).noCluster.build()

  private[this] val configurators = Seq(configurator0, configurator1)

  private[this] val client0 = ConfiguratorClient(s"${configurator0.hostname}:${configurator0.port}")
  private[this] val client1 = ConfiguratorClient(s"${configurator1.hostname}:${configurator1.port}")
  private[this] val clients = Seq(client0, client1)

  @Test
  def testTopic(): Unit = {
    clients.foreach(client => {
      def compareRequestAndResponse(request: TopicInfoRequest, response: TopicInfo): TopicInfo = {
        request.name shouldBe response.name
        request.numberOfReplications shouldBe response.numberOfReplications
        request.numberOfPartitions shouldBe response.numberOfPartitions
        response
      }

      def compare2Response(lhs: TopicInfo, rhs: TopicInfo): Unit = {
        lhs.uuid shouldBe rhs.uuid
        lhs.name shouldBe rhs.name
        lhs.numberOfReplications shouldBe rhs.numberOfReplications
        lhs.numberOfPartitions shouldBe rhs.numberOfPartitions
        lhs.lastModified shouldBe rhs.lastModified
      }

      // test add
      client.list[TopicInfo].size shouldBe 0
      val request = TopicInfoRequest(methodName, 1, 1)
      val response = compareRequestAndResponse(request, client.add[TopicInfoRequest, TopicInfo](request))

      // test get
      compare2Response(response, client.get[TopicInfo](response.uuid))

      // test update
      val anotherRequest = TopicInfoRequest(methodName, 2, 1)
      val newResponse =
        compareRequestAndResponse(anotherRequest,
                                  client.update[TopicInfoRequest, TopicInfo](response.uuid, anotherRequest))

      // test get
      compare2Response(newResponse, client.get[TopicInfo](newResponse.uuid))

      // test delete
      client.list[TopicInfo].size shouldBe 1
      client.delete[TopicInfo](response.uuid)
      client.list[TopicInfo].size shouldBe 0

      // test nonexistent data
      an[IllegalArgumentException] should be thrownBy client.get[TopicInfo]("123")
      an[IllegalArgumentException] should be thrownBy client.update[TopicInfoRequest, TopicInfo]("777", anotherRequest)
    })
  }

  @Test
  def testSchema(): Unit = {
    clients.foreach(client => {
      def compareRequestAndResponse(request: SchemaRequest, response: Schema): Schema = {
        request.name shouldBe response.name
        request.disabled shouldBe response.disabled
        request.orders.sameElements(response.orders) shouldBe true
        request.types.sameElements(response.types) shouldBe true
        response
      }

      def compare2Response(lhs: Schema, rhs: Schema): Unit = {
        lhs.uuid shouldBe rhs.uuid
        lhs.name shouldBe rhs.name
        lhs.orders.sameElements(rhs.orders) shouldBe true
        lhs.types.sameElements(rhs.types) shouldBe true
        lhs.lastModified shouldBe rhs.lastModified
      }

      // test add
      client.list[Schema].size shouldBe 0
      val request = SchemaRequest(methodName, Map("cf0" -> DataType.BYTES), Map("cf0" -> 1), false)
      val response = compareRequestAndResponse(request, client.add[SchemaRequest, Schema](request))

      // test get
      compare2Response(response, client.get[Schema](response.uuid))

      // test update
      val anotherRequest = SchemaRequest(methodName,
                                         Map("cf0" -> DataType.BYTES, "cf1" -> DataType.DOUBLE),
                                         Map("cf0" -> 1, "cf1" -> 2),
                                         false)
      val newResponse =
        compareRequestAndResponse(anotherRequest, client.update[SchemaRequest, Schema](response.uuid, anotherRequest))

      // test get
      compare2Response(newResponse, client.get[Schema](newResponse.uuid))

      // test delete
      client.list[Schema].size shouldBe 1
      client.delete[Schema](response.uuid)
      client.list[Schema].size shouldBe 0

      // test nonexistent data
      an[IllegalArgumentException] should be thrownBy client.get[Schema]("123")
      an[IllegalArgumentException] should be thrownBy client.update[SchemaRequest, Schema]("777", anotherRequest)
    })
  }

  @Test
  def testHdfsInformation(): Unit = {
    clients.foreach(client => {
      def compareRequestAndResponse(request: HdfsInformationRequest, response: HdfsInformation): HdfsInformation = {
        request.name shouldBe response.name
        request.uri shouldBe response.uri
        response
      }

      def compare2Response(lhs: HdfsInformation, rhs: HdfsInformation): Unit = {
        lhs.uuid shouldBe rhs.uuid
        lhs.name shouldBe rhs.name
        lhs.uri shouldBe rhs.uri
        lhs.lastModified shouldBe rhs.lastModified
      }

      // test add
      client.list[HdfsInformation].size shouldBe 0
      val request = HdfsInformationRequest(methodName, "file:///")
      val response = compareRequestAndResponse(request, client.add[HdfsInformationRequest, HdfsInformation](request))

      // test get
      compare2Response(response, client.get[HdfsInformation](response.uuid))

      // test update
      val anotherRequest = HdfsInformationRequest(s"$methodName-2", "file:///")
      val newResponse =
        compareRequestAndResponse(anotherRequest,
                                  client.update[HdfsInformationRequest, HdfsInformation](response.uuid, anotherRequest))

      // test get
      compare2Response(newResponse, client.get[HdfsInformation](newResponse.uuid))

      // test delete
      client.list[HdfsInformation].size shouldBe 1
      client.delete[HdfsInformation](response.uuid)
      client.list[HdfsInformation].size shouldBe 0

      // test nonexistent data
      an[IllegalArgumentException] should be thrownBy client.get[HdfsInformation]("123")
      an[IllegalArgumentException] should be thrownBy client
        .update[HdfsInformationRequest, HdfsInformation]("777", anotherRequest)
    })
  }

  @Test
  def testValidationOfHdfs(): Unit = {
    clients.foreach(client => {
      val report = client.validate[HdfsValidationRequest, ValidationReport](HdfsValidationRequest("file:///tmp"))
      report.isEmpty shouldBe false
      report.foreach(_.pass shouldBe true)
    })
  }

  @Test
  def testClusterInformation(): Unit = {
    // only test the configurator based on mini cluster
    val clusterInformation = client0.cluster[ClusterInformation]
    clusterInformation.brokers shouldBe testUtil.brokersString
    clusterInformation.workers shouldBe testUtil.workersString
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
      doClose(OharaTestUtil.localWorkers(3, 3)) { util =>
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
                Configurator.WORKERS_KEY,
                util.workersString,
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
  def testInvalidMain(): Unit = {
    // enable this flag to make sure the instance of Configurator is always die.
    Configurator.closeRunningConfigurator = true
    try {
      an[IllegalArgumentException] should be thrownBy Configurator.main(Array[String]("localhost"))
      an[IllegalArgumentException] should be thrownBy Configurator.main(
        Array[String]("localhost", "localhost", "localhost"))
      an[IllegalArgumentException] should be thrownBy Configurator.main(
        Array[String](Configurator.HOSTNAME_KEY, "localhost", Configurator.PORT_KEY, "0", Configurator.TOPIC_KEY))
    } finally Configurator.closeRunningConfigurator = false
  }

  @After
  def tearDown(): Unit = {
    clients.foreach(CloseOnce.close(_))
    configurators.foreach(CloseOnce.close(_))
  }
}
