package com.island.ohara.configurator

import java.util.concurrent.{Executors, TimeUnit}

import com.island.ohara.client.ConfiguratorJson._
import com.island.ohara.client.{ConfiguratorClient, ConnectorClient, DatabaseClient}
import com.island.ohara.configurator.store.Store
import com.island.ohara.integration.{OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.io.CloseOnce
import com.island.ohara.io.CloseOnce._
import com.island.ohara.kafka.{KafkaClient, KafkaUtil}
import com.island.ohara.serialization.DataType
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{ExecutionContext, Future}

/**
  * this test includes two configurators - with/without cluster.
  * All test cases should work with all configurators.
  */
class TestConfigurator extends With3Brokers3Workers with Matchers {

  private[this] val configurator0 =
    Configurator
      .builder()
      .hostname("localhost")
      .port(0)
      .store(
        Store
          .builder()
          .numberOfPartitions(1)
          .numberOfReplications(1)
          .topicName(classOf[TestConfigurator].getSimpleName)
          .brokers(testUtil.brokers)
          .build[String, Any])
      .kafkaClient(KafkaClient(testUtil.brokers))
      .connectClient(ConnectorClient(testUtil.workers))
      .build()

  private[this] val configurator1 =
    Configurator.builder().hostname("localhost").port(0).noCluster.build()

  private[this] val configurators = Seq(configurator0, configurator1)

  private[this] val client0 = ConfiguratorClient(s"${configurator0.hostname}:${configurator0.port}")
  private[this] val client1 = ConfiguratorClient(s"${configurator1.hostname}:${configurator1.port}")
  private[this] val clients = Seq(client0, client1)

  private[this] val db = testUtil.dataBase

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
      // verify the topic from kafka
      if (client == client0) {
        // the "name" used to create topic is uuid rather than name of request
        KafkaUtil.exist(testUtil.brokers, request.name) shouldBe false
        KafkaUtil.exist(testUtil.brokers, response.uuid) shouldBe true
        val topicInfo = KafkaUtil.topicInfo(testUtil.brokers, response.uuid).get
        topicInfo.numberOfPartitions shouldBe 1
        topicInfo.numberOfReplications shouldBe 1
      }

      // test get
      compare2Response(response, client.get[TopicInfo](response.uuid))

      // test update
      val anotherRequest = TopicInfoRequest(methodName, 2, 1)
      val newResponse =
        compareRequestAndResponse(anotherRequest,
                                  client.update[TopicInfoRequest, TopicInfo](response.uuid, anotherRequest))
      // verify the topic from kafka
      if (client == client0) {
        KafkaUtil.exist(testUtil.brokers, response.uuid) shouldBe true
        val topicInfo = KafkaUtil.topicInfo(testUtil.brokers, response.uuid).get
        topicInfo.numberOfPartitions shouldBe 2
        topicInfo.numberOfReplications shouldBe 1
      }

      // test get
      compare2Response(newResponse, client.get[TopicInfo](newResponse.uuid))

      // test delete
      client.list[TopicInfo].size shouldBe 1
      client.delete[TopicInfo](response.uuid)
      client.list[TopicInfo].size shouldBe 0
      if (client == client0) {
        KafkaUtil.exist(testUtil.brokers, response.uuid) shouldBe false
        KafkaUtil.topicInfo(testUtil.brokers, response.uuid).isEmpty shouldBe true
      }

      // test nonexistent data
      an[IllegalArgumentException] should be thrownBy client.get[TopicInfo]("123")
      an[IllegalArgumentException] should be thrownBy client.update[TopicInfoRequest, TopicInfo]("777", anotherRequest)

      // test same name
      val topicNames: Set[String] = (0 until 5)
        .map(index => client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(s"topic-$index", 1, 1)).name)
        .toSet
      topicNames.size shouldBe 5
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
  def testPipeline(): Unit = {
    clients.foreach(client => {
      def compareRequestAndResponse(request: PipelineRequest, response: Pipeline): Pipeline = {
        request.name shouldBe response.name
        request.rules.sameElements(response.rules) shouldBe true
        response
      }

      def compare2Response(lhs: Pipeline, rhs: Pipeline): Unit = {
        lhs.uuid shouldBe rhs.uuid
        lhs.name shouldBe rhs.name
        lhs.rules.sameElements(rhs.rules) shouldBe true
        lhs.objects.sameElements(rhs.objects) shouldBe true
        lhs.lastModified shouldBe rhs.lastModified
      }

      // test add
      val uuid_0 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid
      val uuid_1 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid
      val uuid_2 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid

      client.list[Pipeline].size shouldBe 0

      val request = PipelineRequest(methodName, Map(uuid_0 -> uuid_1))
      val response = compareRequestAndResponse(request, client.add[PipelineRequest, Pipeline](request))
      response.status shouldBe Status.STOPPED

      // test get
      compare2Response(response, client.get[Pipeline](response.uuid))

      // test update
      val anotherRequest = PipelineRequest(methodName, Map(uuid_0 -> uuid_2))
      val newResponse =
        compareRequestAndResponse(anotherRequest,
                                  client.update[PipelineRequest, Pipeline](response.uuid, anotherRequest))
      // test get
      compare2Response(newResponse, client.get[Pipeline](newResponse.uuid))

      // test delete
      client.list[Pipeline].size shouldBe 1
      client.delete[Pipeline](response.uuid)
      client.list[Pipeline].size shouldBe 0

      // test nonexistent data
      an[IllegalArgumentException] should be thrownBy client.get[Pipeline]("123")
      an[IllegalArgumentException] should be thrownBy client.update[PipelineRequest, Pipeline]("777", anotherRequest)

      // test invalid request: nonexistent uuid
      val invalidRequest = PipelineRequest(methodName, Map("invalid" -> uuid_2))
      an[IllegalArgumentException] should be thrownBy client.add[PipelineRequest, Pipeline](invalidRequest)
    })
  }

  @Test
  def testControlPipeline(): Unit = {
    clients.foreach(client => {
      // test add
      val uuid_0 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid
      val uuid_1 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid

      val response =
        client.add[PipelineRequest, Pipeline](PipelineRequest(methodName, Map(uuid_0 -> uuid_1)))

      // the pipeline is already stopped
      an[IllegalArgumentException] should be thrownBy client.stop[Pipeline](response.uuid)

      val newResponse = client.start[Pipeline](response.uuid)
      response.uuid shouldBe newResponse.uuid
      response.name shouldBe newResponse.name
      response.status shouldBe Status.STOPPED
      newResponse.status shouldBe Status.RUNNING
      response.rules.sameElements(newResponse.rules) shouldBe true
      response.objects.sameElements(newResponse.objects) shouldBe true
      response.lastModified <= newResponse.lastModified shouldBe true

      // the pipeline is already running
      an[IllegalArgumentException] should be thrownBy client.start[Pipeline](response.uuid)
    })
  }

  @Test
  def testModifyTopicFromPipeline(): Unit = {
    clients.foreach(client => {

      val uuid_0 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid
      val uuid_1 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid
      val uuid_2 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid
      client.list[TopicInfo].size shouldBe 3

      val response =
        client.add[PipelineRequest, Pipeline](PipelineRequest(methodName, Map(uuid_0 -> uuid_1)))
      response.status shouldBe Status.STOPPED

      // the uuid_1 is used by pipeline so configurator disallow us to remove it
      an[IllegalArgumentException] should be thrownBy client.delete[TopicInfo](uuid_1)

      // the pipeline is not running so it is ok to update the topic
      client.update[TopicInfoRequest, TopicInfo](uuid_0, TopicInfoRequest(methodName, 2, 1))

      client.start[Pipeline](response.uuid)
      an[IllegalArgumentException] should be thrownBy client
        .update[TopicInfoRequest, TopicInfo](uuid_0, TopicInfoRequest(methodName, 2, 1))

      // fail to update a running pipeline
      an[IllegalArgumentException] should be thrownBy client
        .update[PipelineRequest, Pipeline](response.uuid, PipelineRequest(methodName, Map(uuid_0 -> uuid_2)))
      // fail to delete a running pipeline
      an[IllegalArgumentException] should be thrownBy client
        .update[PipelineRequest, Pipeline](response.uuid, PipelineRequest(methodName, Map(uuid_0 -> uuid_2)))

      // update the pipeline to use another topic (uuid_2)
      client.stop[Pipeline](response.uuid)
      client.update[PipelineRequest, Pipeline](response.uuid, PipelineRequest(methodName, Map(uuid_0 -> uuid_2)))

      // it is ok to remove the topic (uuid_1) since we have updated the pipeline to use another topic (uuid_2)
      client.delete[TopicInfo](uuid_1).uuid shouldBe uuid_1
    })
  }

  @Test
  def testBindInvalidObjects2Pipeline(): Unit = {
    clients.foreach(client => {
      val uuid_0 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid
      val uuid_1 =
        client.add[HdfsInformationRequest, HdfsInformation](HdfsInformationRequest(methodName, "file:///")).uuid
      val uuid_2 =
        client.add[HdfsInformationRequest, HdfsInformation](HdfsInformationRequest(methodName, "file:///")).uuid
      val uuid_3 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid
      client.list[TopicInfo].size shouldBe 2
      client.list[HdfsInformation].size shouldBe 2

      // uuid_0 -> uuid_0: self-bound
      an[IllegalArgumentException] should be thrownBy client.add[PipelineRequest, Pipeline](
        PipelineRequest(methodName, Map(uuid_0 -> uuid_0)))
      // uuid_1 can't be applied to pipeline
      an[IllegalArgumentException] should be thrownBy client.add[PipelineRequest, Pipeline](
        PipelineRequest(methodName, Map(uuid_0 -> uuid_1)))
      // uuid_2 can't be applied to pipeline
      an[IllegalArgumentException] should be thrownBy client.add[PipelineRequest, Pipeline](
        PipelineRequest(methodName, Map(uuid_0 -> uuid_2)))

      val res = client.add[PipelineRequest, Pipeline](PipelineRequest(methodName, Map(uuid_0 -> uuid_3)))
      // uuid_0 -> uuid_0: self-bound
      an[IllegalArgumentException] should be thrownBy client
        .update[PipelineRequest, Pipeline](res.uuid, PipelineRequest(methodName, Map(uuid_0 -> uuid_0)))
      // uuid_1 can't be applied to pipeline
      an[IllegalArgumentException] should be thrownBy client
        .update[PipelineRequest, Pipeline](res.uuid, PipelineRequest(methodName, Map(uuid_0 -> uuid_1)))
      // uuid_2 can't be applied to pipeline
      an[IllegalArgumentException] should be thrownBy client
        .update[PipelineRequest, Pipeline](res.uuid, PipelineRequest(methodName, Map(uuid_0 -> uuid_2)))

      // good case
      client.update[PipelineRequest, Pipeline](res.uuid, PipelineRequest(methodName, Map(uuid_0 -> uuid_3)))
    })
  }

  @Test
  def testUnreadyRules2Pipeline(): Unit = {
    clients.foreach(client => {
      val uuid_0 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid
      val uuid_1 = client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(methodName, 1, 1)).uuid
      client.list[TopicInfo].size shouldBe 2
      var res = client.add[PipelineRequest, Pipeline](PipelineRequest(methodName, Map(uuid_0 -> UNKNOWN)))
      res.rules.size shouldBe 1
      res.rules(uuid_0) shouldBe UNKNOWN
      res.status shouldBe Status.STOPPED
      // the rules are unready so it fails to start the pipeline
      an[IllegalArgumentException] should be thrownBy client.start[Pipeline](res.uuid)

      // complete the rules
      res = client.update[PipelineRequest, Pipeline](res.uuid, PipelineRequest(methodName, Map(uuid_0 -> uuid_1)))
      res.rules.size shouldBe 1
      res.rules(uuid_0) shouldBe uuid_1
      res.status shouldBe Status.STOPPED
      res = client.start[Pipeline](res.uuid)
      res.rules.size shouldBe 1
      res.rules(uuid_0) shouldBe uuid_1
      res.status shouldBe Status.RUNNING
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
  def testValidationOfRdb(): Unit = {
    clients.foreach(client => {
      val report =
        client.validate[RdbValidationRequest, ValidationReport](RdbValidationRequest(db.url, db.user, db.password))
      report.isEmpty shouldBe false
      report.foreach(_.pass shouldBe true)
    })
  }

  @Test
  def testGet2UnmatchedType(): Unit = {
    client0.list[HdfsInformation].size shouldBe 0
    val request = HdfsInformationRequest(methodName, "file:///")
    var response: HdfsInformation = client0.add[HdfsInformationRequest, HdfsInformation](request)
    request.name shouldBe response.name
    request.uri shouldBe response.uri

    response = client0.get[HdfsInformation](response.uuid)
    request.name shouldBe response.name
    request.uri shouldBe response.uri

    an[IllegalArgumentException] should be thrownBy client0.get[TopicInfo](response.uuid)
    an[IllegalArgumentException] should be thrownBy client0.get[Source](response.uuid)

    client0.delete[HdfsInformation](response.uuid)
  }

  @Test
  def testClusterInformation(): Unit = {
    // only test the configurator based on mini cluster
    val clusterInformation = client0.cluster[ClusterInformation]
    clusterInformation.brokers shouldBe testUtil.brokers
    clusterInformation.workers shouldBe testUtil.workers
    clusterInformation.supportedDatabases.contains("mysql") shouldBe true
    clusterInformation.supportedDataTypes shouldBe DataType.all
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
                util.brokers,
                Configurator.WORKERS_KEY,
                util.workers,
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

  @Test
  def testClear(): Unit = {
    clients.foreach(client => {
      client.list[TopicInfo].size shouldBe 0
      (0 until 10).map(index => client.add[TopicInfoRequest, TopicInfo](TopicInfoRequest(index.toString, 1, 1)))
      client.list[TopicInfo].size shouldBe 10
      configurators.foreach(_.clear())
      client.list[TopicInfo].size shouldBe 0
    })
  }

  @Test
  def testQueryDb(): Unit = {
    val tableName = methodName
    doClose(DatabaseClient(db.url, db.user, db.password)) { dbClient =>
      {
        clients.foreach(client => {
          val result = client.query[RdbQuery, RdbInformation](RdbQuery(db.url, db.user, db.password, None, None, None))
          result.name shouldBe "mysql"
          result.tables.isEmpty shouldBe true

          val cf0 = RdbColumn("cf0", "INTEGER", true)
          val cf1 = RdbColumn("cf1", "INTEGER", false)
          def verify(info: RdbInformation): Unit = {
            info.tables.count(_.name == tableName) shouldBe 1
            val table = info.tables.filter(_.name == tableName).head
            table.schema.size shouldBe 2
            table.schema.count(_.name == cf0.name) shouldBe 1
            table.schema.filter(_.name == cf0.name).head.pk shouldBe cf0.pk
            table.schema.count(_.name == cf1.name) shouldBe 1
            table.schema.filter(_.name == cf1.name).head.pk shouldBe cf1.pk
          }
          dbClient.createTable(tableName, Seq(cf0, cf1))

          verify(client.query[RdbQuery, RdbInformation](RdbQuery(db.url, db.user, db.password, None, None, None)))
          verify(
            client.query[RdbQuery, RdbInformation](
              RdbQuery(db.url, db.user, db.password, Some(db.catalog), None, Some(tableName))))

          dbClient.dropTable(tableName)
        })
      }
    }
  }

  @Test
  def testSource(): Unit = {
    clients.foreach(client => {
      def compareRequestAndResponse(request: SourceRequest, response: Source): Source = {
        request.name shouldBe response.name
        request.configs.sameElements(response.configs) shouldBe true
        response
      }

      def compare2Response(lhs: Source, rhs: Source): Unit = {
        lhs.uuid shouldBe rhs.uuid
        lhs.name shouldBe rhs.name
        lhs.configs.sameElements(rhs.configs) shouldBe true
        lhs.lastModified shouldBe rhs.lastModified
      }

      // test add
      client.list[Source].size shouldBe 0
      val request = SourceRequest(methodName, "jdbc", Map("c0" -> "v0", "c1" -> "v1"))
      val response = compareRequestAndResponse(request, client.add[SourceRequest, Source](request))

      // test get
      compare2Response(response, client.get[Source](response.uuid))

      // test update
      val anotherRequest = SourceRequest(methodName, "jdbc", Map("c0" -> "v0", "c1" -> "v1", "c2" -> "v2"))
      val newResponse =
        compareRequestAndResponse(anotherRequest, client.update[SourceRequest, Source](response.uuid, anotherRequest))

      // test get
      compare2Response(newResponse, client.get[Source](newResponse.uuid))

      // test delete
      client.list[Source].size shouldBe 1
      client.delete[Source](response.uuid)
      client.list[Source].size shouldBe 0

      // test nonexistent data
      an[IllegalArgumentException] should be thrownBy client.get[Source]("123")
      an[IllegalArgumentException] should be thrownBy client.update[SourceRequest, Source]("777", anotherRequest)
    })
  }
  @Test
  def testModifySourceFromPipeline(): Unit = {
    clients.foreach(client => {

      val uuid_0 = client.add[SourceRequest, Source](SourceRequest(methodName, "jdbc", Map("a" -> "b"))).uuid
      val uuid_1 = client.add[SourceRequest, Source](SourceRequest(methodName, "jdbc", Map("b" -> "b"))).uuid
      val uuid_2 = client.add[SourceRequest, Source](SourceRequest(methodName, "jdbc", Map("c" -> "b"))).uuid
      client.list[Source].size shouldBe 3

      val response =
        client.add[PipelineRequest, Pipeline](PipelineRequest(methodName, Map(uuid_0 -> uuid_1)))
      response.status shouldBe Status.STOPPED

      // the uuid_1 is used by pipeline so configurator disallow us to remove it
      an[IllegalArgumentException] should be thrownBy client.delete[Source](uuid_1)

      // the pipeline is not running so it is ok to update the source
      client.update[SourceRequest, Source](uuid_0, SourceRequest(methodName, "jdbc", Map("d" -> "b")))

      client.start[Pipeline](response.uuid)
      an[IllegalArgumentException] should be thrownBy client
        .update[SourceRequest, Source](uuid_0, SourceRequest(methodName, "jdbc", Map("d" -> "b")))

      // update the pipeline to use another source (uuid_2)
      client.stop[Pipeline](response.uuid)
      client.update[PipelineRequest, Pipeline](response.uuid, PipelineRequest(methodName, Map(uuid_0 -> uuid_2)))

      // it is ok to remove the source (uuid_1) since we have updated the pipeline to use another source (uuid_2)
      client.delete[Source](uuid_1).uuid shouldBe uuid_1
    })
  }

  @Test
  def testSink(): Unit = {
    clients.foreach(client => {
      def compareRequestAndResponse(request: SinkRequest, response: Sink): Sink = {
        request.name shouldBe response.name
        request.configs.sameElements(response.configs) shouldBe true
        response
      }

      def compare2Response(lhs: Sink, rhs: Sink): Unit = {
        lhs.uuid shouldBe rhs.uuid
        lhs.name shouldBe rhs.name
        lhs.configs.sameElements(rhs.configs) shouldBe true
        lhs.lastModified shouldBe rhs.lastModified
      }

      // test add
      client.list[Sink].size shouldBe 0
      val request = SinkRequest(methodName, "jdbc", Map("c0" -> "v0", "c1" -> "v1"))
      val response = compareRequestAndResponse(request, client.add[SinkRequest, Sink](request))

      // test get
      compare2Response(response, client.get[Sink](response.uuid))

      // test update
      val anotherRequest = SinkRequest(methodName, "jdbc", Map("c0" -> "v0", "c1" -> "v1", "c2" -> "v2"))
      val newResponse =
        compareRequestAndResponse(anotherRequest, client.update[SinkRequest, Sink](response.uuid, anotherRequest))

      // test get
      compare2Response(newResponse, client.get[Sink](newResponse.uuid))

      // test delete
      client.list[Sink].size shouldBe 1
      client.delete[Sink](response.uuid)
      client.list[Sink].size shouldBe 0

      // test nonexistent data
      an[IllegalArgumentException] should be thrownBy client.get[Sink]("123")
      an[IllegalArgumentException] should be thrownBy client.update[SinkRequest, Sink]("777", anotherRequest)
    })
  }

  @Test
  def testModifySinkFromPipeline(): Unit = {
    clients.foreach(client => {

      val uuid_0 = client.add[SinkRequest, Sink](SinkRequest(methodName, "jdbc", Map("a" -> "b"))).uuid
      val uuid_1 = client.add[SinkRequest, Sink](SinkRequest(methodName, "jdbc", Map("b" -> "b"))).uuid
      val uuid_2 = client.add[SinkRequest, Sink](SinkRequest(methodName, "jdbc", Map("c" -> "b"))).uuid
      client.list[Sink].size shouldBe 3

      val response =
        client.add[PipelineRequest, Pipeline](PipelineRequest(methodName, Map(uuid_0 -> uuid_1)))
      response.status shouldBe Status.STOPPED

      // the uuid_1 is used by pipeline so configurator disallow us to remove it
      an[IllegalArgumentException] should be thrownBy client.delete[Sink](uuid_1)

      // the pipeline is not running so it is ok to update the sink
      client.update[SinkRequest, Sink](uuid_0, SinkRequest(methodName, "jdbc", Map("d" -> "b")))

      client.start[Pipeline](response.uuid)
      an[IllegalArgumentException] should be thrownBy client
        .update[SinkRequest, Sink](uuid_0, SinkRequest(methodName, "jdbc", Map("d" -> "b")))

      // update the pipeline to use another sink (uuid_2)
      client.stop[Pipeline](response.uuid)
      client.update[PipelineRequest, Pipeline](response.uuid, PipelineRequest(methodName, Map(uuid_0 -> uuid_2)))

      // it is ok to remove the sink (uuid_1) since we have updated the pipeline to use another sink (uuid_2)
      client.delete[Sink](uuid_1).uuid shouldBe uuid_1
    })
  }

  @After
  def tearDown(): Unit = {
    clients.foreach(CloseOnce.close)
    configurators.foreach(c => {
      c.clear()
      c.close()
    })
  }
}
