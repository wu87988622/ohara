/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.configurator

import com.island.ohara.client.configurator.v0.ConnectorApi.{ConnectorConfiguration, ConnectorConfigurationRequest}
import com.island.ohara.client.configurator.v0.DatabaseApi.{JdbcInfo, JdbcInfoRequest}
import com.island.ohara.client.configurator.v0.FtpApi.{FtpInfo, FtpInfoRequest}
import com.island.ohara.client.configurator.v0.HadoopApi.{HdfsInfo, HdfsInfoRequest}
import com.island.ohara.client.configurator.v0.PipelineApi.{Pipeline, PipelineCreationRequest}
import com.island.ohara.client.configurator.v0.QueryApi.{RdbColumn, RdbInfo, RdbQuery}
import com.island.ohara.client.configurator.v0.TopicApi.{TopicCreationRequest, TopicInfo}
import com.island.ohara.client.configurator.v0.ValidationApi._
import com.island.ohara.client.configurator.v0._
import com.island.ohara.client.{WorkerClient, DatabaseClient}
import com.island.ohara.common.data.{Column, DataType}
import com.island.ohara.common.util.VersionUtil
import com.island.ohara.integration.WithBrokerWorker
import com.island.ohara.kafka.{BrokerClient, KafkaUtil}
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * this test includes two configurators - with/without cluster.
  * All test cases should work with all configurators.
  */
class TestConfigurator extends WithBrokerWorker with Matchers {
  private[this] val configurator0 = Configurator
    .builder()
    .hostname("localhost")
    .port(0)
    .brokerClient(BrokerClient.of(testUtil.brokersConnProps))
    .connectClient(WorkerClient(testUtil.workersConnProps))
    .build()

  private[this] val configurator1 =
    Configurator.fake()

  private[this] val configurators = Seq(configurator0, configurator1)

  private[this] val db = testUtil.dataBase
  private[this] val ftpServer = testUtil.ftpServer

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def testTopic(): Unit = {
    configurators.foreach { configurator =>
      def compareRequestAndResponse(request: TopicCreationRequest, response: TopicInfo): TopicInfo = {
        request.name shouldBe response.name
        request.numberOfReplications shouldBe response.numberOfReplications
        request.numberOfPartitions shouldBe response.numberOfPartitions
        response
      }

      def compare2Response(lhs: TopicInfo, rhs: TopicInfo): Unit = {
        lhs.id shouldBe rhs.id
        lhs.name shouldBe rhs.name
        lhs.numberOfReplications shouldBe rhs.numberOfReplications
        lhs.numberOfPartitions shouldBe rhs.numberOfPartitions
        lhs.lastModified shouldBe rhs.lastModified
      }

      val access = TopicApi.access().hostname(configurator.hostname).port(configurator.port)

      // test add
      result(access.list()).size shouldBe 0
      val request = TopicCreationRequest(methodName, 1, 1)
      val response = compareRequestAndResponse(request, result(access.add(request)))
      // verify the topic from kafka
      if (configurator == configurator0) {
        // the "name" used to create topic is uuid rather than name from request
        KafkaUtil.exist(testUtil.brokersConnProps, request.name) shouldBe false
        KafkaUtil.exist(testUtil.brokersConnProps, response.id) shouldBe true
        val topicInfo = KafkaUtil.topicDescription(testUtil.brokersConnProps, response.id)
        topicInfo.numberOfPartitions shouldBe 1
        topicInfo.numberOfReplications shouldBe 1
      }

      // test get
      compare2Response(response, result(access.get(response.id)))

      // test update
      val anotherRequest = TopicCreationRequest(methodName, 2, 1)
      val newResponse =
        compareRequestAndResponse(anotherRequest, result(access.update(response.id, anotherRequest)))
      // verify the topic from kafka
      if (configurator == configurator0) {
        KafkaUtil.exist(testUtil.brokersConnProps, response.id) shouldBe true
        val topicInfo = KafkaUtil.topicDescription(testUtil.brokersConnProps, response.id)
        topicInfo.numberOfPartitions shouldBe 2
        topicInfo.numberOfReplications shouldBe 1
      }

      // test get
      compare2Response(newResponse, result(access.get(newResponse.id)))

      // test delete
      result(access.list()).size shouldBe 1
      result(access.delete(response.id)) shouldBe newResponse
      result(access.list()).size shouldBe 0
      if (configurator == configurator0) KafkaUtil.exist(testUtil.brokersConnProps, response.id) shouldBe false

      // test nonexistent data
      an[IllegalArgumentException] should be thrownBy result(access.get("123"))
      an[IllegalArgumentException] should be thrownBy result(access.update("777", anotherRequest))

      // test same name
      val topicNames: Set[String] =
        (0 until 5).map(index => result(access.add(TopicCreationRequest(s"topic-$index", 1, 1))).name).toSet
      topicNames.size shouldBe 5
    }
  }

  @Test
  def testHdfsInformation(): Unit = configurators.foreach { configurator =>
    def compareRequestAndResponse(request: HdfsInfoRequest, response: HdfsInfo): HdfsInfo = {
      request.name shouldBe response.name
      request.uri shouldBe response.uri
      response
    }

    def compare2Response(lhs: HdfsInfo, rhs: HdfsInfo): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe rhs.name
      lhs.uri shouldBe rhs.uri
      lhs.lastModified shouldBe rhs.lastModified
    }

    // test add
    val hdfsAccess = HadoopApi.access().hostname(configurator.hostname).port(configurator.port)
    result(hdfsAccess.list()).size shouldBe 0
    val request = HdfsInfoRequest(methodName, "file:///")
    val response = result(hdfsAccess.add(request))

    // test get
    compare2Response(response, result(hdfsAccess.get(response.id)))

    // test update
    val anotherRequest = HdfsInfoRequest(s"$methodName-2", "file:///")
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(hdfsAccess.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(hdfsAccess.get(newResponse.id)))

    // test delete
    result(hdfsAccess.list()).size shouldBe 1
    result(hdfsAccess.delete(response.id)) shouldBe newResponse
    result(hdfsAccess.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(hdfsAccess.get("123"))
    an[IllegalArgumentException] should be thrownBy result(hdfsAccess.update("777", anotherRequest))
  }

  @Test
  def testFtpInformation(): Unit = configurators.foreach { configurator =>
    def compareRequestAndResponse(request: FtpInfoRequest, response: FtpInfo): FtpInfo = {
      request.name shouldBe response.name
      request.hostname shouldBe response.hostname
      request.port shouldBe response.port
      request.user shouldBe response.user
      request.password shouldBe response.password
      response
    }

    def compare2Response(lhs: FtpInfo, rhs: FtpInfo): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe lhs.name
      lhs.hostname shouldBe lhs.hostname
      lhs.port shouldBe lhs.port
      lhs.user shouldBe lhs.user
      lhs.password shouldBe lhs.password
      lhs.lastModified shouldBe rhs.lastModified
    }

    val access = FtpApi.access().hostname(configurator.hostname).port(configurator.port)
    // test add
    result(access.list()).size shouldBe 0

    val request = FtpInfoRequest("test", "152.22.23.12", 5, "test", "test")
    val response = compareRequestAndResponse(request, result(access.add(request)))

    // test get
    compare2Response(response, result(access.get(response.id)))

    // test update
    val anotherRequest = FtpInfoRequest("test2", "152.22.23.125", 1222, "test", "test")
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(access.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(access.get(newResponse.id)))

    // test delete
    result(access.list()).size shouldBe 1
    result(access.delete(response.id)) shouldBe newResponse
    result(access.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(access.get("asdadas"))
    an[IllegalArgumentException] should be thrownBy result(access.update("asdadas", anotherRequest))
  }

  @Test
  def testJdbcInformation(): Unit = configurators.foreach { configurator =>
    def compareRequestAndResponse(request: JdbcInfoRequest, response: JdbcInfo): JdbcInfo = {
      request.name shouldBe response.name
      request.url shouldBe response.url
      request.user shouldBe response.user
      request.password shouldBe response.password
      response
    }

    def compare2Response(lhs: JdbcInfo, rhs: JdbcInfo): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe lhs.name
      lhs.url shouldBe lhs.url
      lhs.user shouldBe lhs.user
      lhs.password shouldBe lhs.password
      lhs.lastModified shouldBe rhs.lastModified
    }

    val access = DatabaseApi.access().hostname(configurator.hostname).port(configurator.port)
    // test add
    result(access.list()).size shouldBe 0

    val request = JdbcInfoRequest("test", "oracle://152.22.23.12:4222", "test", "test")
    val response = compareRequestAndResponse(request, result(access.add(request)))

    // test get
    compare2Response(response, result(access.get(response.id)))

    // test update
    val anotherRequest = JdbcInfoRequest("test2", "msSQL://152.22.23.12:4222", "test", "test")
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(access.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(access.get(newResponse.id)))

    // test delete
    result(access.list()).size shouldBe 1
    result(access.delete(response.id)) shouldBe newResponse
    result(access.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(access.get("adasd"))
    an[IllegalArgumentException] should be thrownBy result(access.update("adasd", anotherRequest))
  }

  @Test
  def testPipeline(): Unit = configurators.foreach { configurator =>
    def compareRequestAndResponse(request: PipelineCreationRequest, response: Pipeline): Pipeline = {
      request.name shouldBe response.name
      request.rules shouldBe response.rules
      response
    }

    def compare2Response(lhs: Pipeline, rhs: Pipeline): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe rhs.name
      lhs.rules shouldBe rhs.rules
      lhs.objects shouldBe rhs.objects
      lhs.lastModified shouldBe rhs.lastModified
    }

    // test add
    val topicAccess = TopicApi.access().hostname(configurator.hostname).port(configurator.port)
    val pipelineAccess = PipelineApi.access().hostname(configurator.hostname).port(configurator.port)
    val uuid_0 = result(topicAccess.add(TopicCreationRequest(methodName(), 1, 1))).id
    val uuid_1 = result(topicAccess.add(TopicCreationRequest(methodName(), 1, 1))).id
    val uuid_2 = result(topicAccess.add(TopicCreationRequest(methodName(), 1, 1))).id

    result(pipelineAccess.list()).size shouldBe 0

    val request = PipelineCreationRequest(methodName, Map(uuid_0 -> uuid_1))
    val response = compareRequestAndResponse(request, result(pipelineAccess.add(request)))

    // test get
    compare2Response(response, result(pipelineAccess.get(response.id)))

    // test update
    val anotherRequest = PipelineCreationRequest(methodName, Map(uuid_0 -> uuid_2))
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(pipelineAccess.update(response.id, anotherRequest)))

    // topics should have no state
    newResponse.objects.foreach(_.state shouldBe None)

    // test get
    compare2Response(newResponse, result(pipelineAccess.get(newResponse.id)))

    // test delete
    result(pipelineAccess.list()).size shouldBe 1
    result(pipelineAccess.delete(response.id)) shouldBe newResponse
    result(pipelineAccess.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(pipelineAccess.get("asdasdsad"))
    an[IllegalArgumentException] should be thrownBy result(pipelineAccess.update("asdasdsad", anotherRequest))

    // test invalid request: nonexistent uuid
    val invalidRequest = PipelineCreationRequest(methodName, Map("invalid" -> uuid_2))
    an[IllegalArgumentException] should be thrownBy result(pipelineAccess.add(invalidRequest))
  }

  @Test
  def testBindInvalidObjects2Pipeline(): Unit = configurators.foreach { configurator =>
    val topicAccess = TopicApi.access().hostname(configurator.hostname).port(configurator.port)
    val hdfsAccess = HadoopApi.access().hostname(configurator.hostname).port(configurator.port)
    val pipelineAccess = PipelineApi.access().hostname(configurator.hostname).port(configurator.port)
    val uuid_0 = result(topicAccess.add(TopicCreationRequest(methodName(), 1, 1))).id
    val uuid_1 = result(hdfsAccess.add(HdfsInfoRequest(methodName, "file:///"))).id
    val uuid_2 = result(hdfsAccess.add(HdfsInfoRequest(methodName, "file:///"))).id
    val uuid_3 = result(topicAccess.add(TopicCreationRequest(methodName(), 1, 1))).id
    result(topicAccess.list()).size shouldBe 2
    result(hdfsAccess.list()).size shouldBe 2

    // uuid_0 -> uuid_0: self-bound
    an[IllegalArgumentException] should be thrownBy result(
      pipelineAccess.add(PipelineCreationRequest(methodName, Map(uuid_0 -> uuid_0))))
    // uuid_1 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineAccess.add(PipelineCreationRequest(methodName, Map(uuid_0 -> uuid_1))))
    // uuid_2 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineAccess.add(PipelineCreationRequest(methodName, Map(uuid_0 -> uuid_2))))

    val res = result(pipelineAccess.add(PipelineCreationRequest(methodName, Map(uuid_0 -> uuid_3))))
    // uuid_0 -> uuid_0: self-bound
    an[IllegalArgumentException] should be thrownBy result(
      pipelineAccess.update(res.id, PipelineCreationRequest(methodName, Map(uuid_0 -> uuid_0))))
    // uuid_1 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineAccess.update(res.id, PipelineCreationRequest(methodName, Map(uuid_0 -> uuid_1))))
    // uuid_2 can't be applied to pipeline
    an[IllegalArgumentException] should be thrownBy result(
      pipelineAccess.update(res.id, PipelineCreationRequest(methodName, Map(uuid_0 -> uuid_2))))

    // good case
    result(pipelineAccess.update(res.id, PipelineCreationRequest(methodName, Map(uuid_0 -> uuid_3)))).name shouldBe methodName
  }

  @Test
  def testValidationOfHdfs(): Unit = configurators.foreach { configurator =>
    val report = result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(HdfsValidationRequest("file:///tmp")))
    report.isEmpty shouldBe false
    report.foreach(_.pass shouldBe true)
  }

  @Test
  def testValidationOfRdb(): Unit = configurators.foreach { configurator =>
    val report = result(
      ValidationApi
        .access()
        .hostname(configurator.hostname)
        .port(configurator.port)
        .verify(RdbValidationRequest(db.url, db.user, db.password)))
    report.isEmpty shouldBe false
    report.foreach(_.pass shouldBe true)
  }

  @Test
  def testValidationOfFtp(): Unit =
    configurators.foreach { configurator =>
      val report = result(
        ValidationApi
          .access()
          .hostname(configurator.hostname)
          .port(configurator.port)
          .verify(FtpValidationRequest(ftpServer.hostname, ftpServer.port, ftpServer.user, ftpServer.password)))
      report.isEmpty shouldBe false
      report.foreach(_.pass shouldBe true)
    }

  @Test
  def testGet2UnmatchedType(): Unit = {
    val access = HadoopApi.access().hostname(configurator0.hostname).port(configurator0.port)
    result(access.list()).size shouldBe 0
    val request = HdfsInfoRequest(methodName, "file:///")
    var response: HdfsInfo = result(access.add(request))
    request.name shouldBe response.name
    request.uri shouldBe response.uri

    response = result(access.get(response.id))
    request.name shouldBe response.name
    request.uri shouldBe response.uri

    an[IllegalArgumentException] should be thrownBy result(
      TopicApi.access().hostname(configurator0.hostname).port(configurator0.port).get(response.id))
    result(access.delete(response.id)) shouldBe response
  }

  @Test
  def testClusterInformation(): Unit = {
    // only test the configurator based on mini cluster
    val clusterInformation = result(InfoApi.access().hostname(configurator0.hostname).port(configurator0.port).get())
    clusterInformation.brokers shouldBe testUtil.brokersConnProps
    clusterInformation.workers shouldBe testUtil.workersConnProps
    clusterInformation.supportedDatabases.contains("mysql") shouldBe true
    clusterInformation.supportedDataTypes shouldBe DataType.all.asScala
    clusterInformation.sources.exists(x => x.className.contains("com.island")) shouldBe true
    clusterInformation.sinks.exists(x => x.className.contains("com.island")) shouldBe true
    clusterInformation.versionInfo.version shouldBe VersionUtil.VERSION
    clusterInformation.versionInfo.user shouldBe VersionUtil.USER
    clusterInformation.versionInfo.revision shouldBe VersionUtil.REVISION
    clusterInformation.versionInfo.date shouldBe VersionUtil.DATE
  }

  @Test
  def testInvalidMain(): Unit = {
    // enable this flag to make sure the instance from Configurator is always die.
    Configurator.closeRunningConfigurator = true
    try {
      an[IllegalArgumentException] should be thrownBy Configurator.main(Array[String]("localhost"))
      an[IllegalArgumentException] should be thrownBy Configurator.main(
        Array[String]("localhost", "localhost", "localhost"))
      an[IllegalArgumentException] should be thrownBy Configurator.main(
        Array[String](Configurator.HOSTNAME_KEY, "localhost", Configurator.PORT_KEY, "0", Configurator.BROKERS_KEY))
    } finally Configurator.closeRunningConfigurator = false
  }
  @Test
  def testQueryDb(): Unit = {
    val tableName = methodName
    val dbClient = DatabaseClient(db.url, db.user, db.password)
    try configurators.foreach { configurator =>
      val r = result(
        QueryApi
          .access()
          .hostname(configurator.hostname)
          .port(configurator.port)
          .query(RdbQuery(db.url, db.user, db.password, None, None, None)))
      r.name shouldBe "mysql"
      r.tables.isEmpty shouldBe true

      val cf0 = RdbColumn("cf0", "INTEGER", true)
      val cf1 = RdbColumn("cf1", "INTEGER", false)
      def verify(info: RdbInfo): Unit = {
        info.tables.count(_.name == tableName) shouldBe 1
        val table = info.tables.filter(_.name == tableName).head
        table.schema.size shouldBe 2
        table.schema.count(_.name == cf0.name) shouldBe 1
        table.schema.filter(_.name == cf0.name).head.pk shouldBe cf0.pk
        table.schema.count(_.name == cf1.name) shouldBe 1
        table.schema.filter(_.name == cf1.name).head.pk shouldBe cf1.pk
      }
      dbClient.createTable(tableName, Seq(cf0, cf1))

      verify(
        result(
          QueryApi
            .access()
            .hostname(configurator.hostname)
            .port(configurator.port)
            .query(RdbQuery(db.url, db.user, db.password, None, None, None))))
      verify(
        result(
          QueryApi
            .access()
            .hostname(configurator.hostname)
            .port(configurator.port)
            .query(RdbQuery(db.url, db.user, db.password, Some(db.databaseName), None, Some(tableName)))))
      dbClient.dropTable(tableName)
    } finally dbClient.close()
  }

  @Test
  def testSource(): Unit = configurators.foreach { configurator =>
    def compareRequestAndResponse(request: ConnectorConfigurationRequest,
                                  response: ConnectorConfiguration): ConnectorConfiguration = {
      request.name shouldBe response.name
      request.schema shouldBe response.schema
      request.configs shouldBe response.configs
      response
    }

    def compare2Response(lhs: ConnectorConfiguration, rhs: ConnectorConfiguration): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe rhs.name
      lhs.schema shouldBe rhs.schema
      lhs.configs shouldBe rhs.configs
      lhs.lastModified shouldBe rhs.lastModified
    }
    val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

    val schema = Seq(Column.of("cf", DataType.BOOLEAN, 1), Column.of("cf", DataType.BOOLEAN, 2))
    // test add
    result(access.list()).size shouldBe 0
    val request = ConnectorConfigurationRequest(name = methodName,
                                                className = "jdbc",
                                                schema = schema,
                                                configs = Map("c0" -> "v0", "c1" -> "v1"),
                                                topics = Seq.empty,
                                                numberOfTasks = 1)
    val response =
      compareRequestAndResponse(request, result(access.add(request)))

    // test get
    compare2Response(response, result(access.get(response.id)))

    // test update
    val anotherRequest = ConnectorConfigurationRequest(name = methodName,
                                                       className = "jdbc",
                                                       schema = schema,
                                                       configs = Map("c0" -> "v0", "c1" -> "v1", "c2" -> "v2"),
                                                       topics = Seq.empty,
                                                       numberOfTasks = 1)
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(access.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(access.get(newResponse.id)))

    // test delete
    result(access.list()).size shouldBe 1
    result(access.delete(response.id)) shouldBe newResponse
    result(access.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(access.get("asdasdasd"))
    an[IllegalArgumentException] should be thrownBy result(access.update("Asdasd", anotherRequest))
  }

  @Test
  def testInvalidSource(): Unit = configurators.foreach { configurator =>
    val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

    result(access.list()).size shouldBe 0

    val illegalOrder = Seq(Column.of("cf", DataType.BOOLEAN, 0), Column.of("cf", DataType.BOOLEAN, 2))
    an[IllegalArgumentException] should be thrownBy result(
      access.add(
        ConnectorConfigurationRequest(name = methodName,
                                      className = "jdbc",
                                      schema = illegalOrder,
                                      configs = Map("c0" -> "v0", "c1" -> "v1"),
                                      topics = Seq.empty,
                                      numberOfTasks = 1)))
    result(access.list()).size shouldBe 0

    val duplicateOrder = Seq(Column.of("cf", DataType.BOOLEAN, 1), Column.of("cf", DataType.BOOLEAN, 1))
    an[IllegalArgumentException] should be thrownBy result(
      access.add(
        ConnectorConfigurationRequest(name = methodName,
                                      className = "jdbc",
                                      schema = duplicateOrder,
                                      configs = Map("c0" -> "v0", "c1" -> "v1"),
                                      topics = Seq.empty,
                                      numberOfTasks = 1)))
    result(access.list()).size shouldBe 0
  }

  @Test
  def testSink(): Unit = configurators.foreach { configurator =>
    def compareRequestAndResponse(request: ConnectorConfigurationRequest,
                                  response: ConnectorConfiguration): ConnectorConfiguration = {
      request.name shouldBe response.name
      request.configs shouldBe response.configs
      response
    }

    def compare2Response(lhs: ConnectorConfiguration, rhs: ConnectorConfiguration): Unit = {
      lhs.id shouldBe rhs.id
      lhs.name shouldBe rhs.name
      lhs.schema shouldBe rhs.schema
      lhs.configs shouldBe rhs.configs
      lhs.lastModified shouldBe rhs.lastModified
    }

    val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)
    val schema = Seq(Column.of("cf", DataType.BOOLEAN, 1), Column.of("cf", DataType.BOOLEAN, 2))

    // test add
    result(access.list()).size shouldBe 0
    val request = ConnectorConfigurationRequest(name = methodName,
                                                className = "jdbc",
                                                schema = schema,
                                                configs = Map("c0" -> "v0", "c1" -> "v1"),
                                                topics = Seq.empty,
                                                numberOfTasks = 1)
    val response =
      compareRequestAndResponse(request, result(access.add(request)))

    // test get
    compare2Response(response, result(access.get(response.id)))

    // test update
    val anotherRequest = ConnectorConfigurationRequest(name = methodName,
                                                       className = "jdbc",
                                                       schema = schema,
                                                       configs = Map("c0" -> "v0", "c1" -> "v1", "c2" -> "v2"),
                                                       topics = Seq.empty,
                                                       numberOfTasks = 1)
    val newResponse =
      compareRequestAndResponse(anotherRequest, result(access.update(response.id, anotherRequest)))

    // test get
    compare2Response(newResponse, result(access.get(newResponse.id)))

    // test delete
    result(access.list()).size shouldBe 1
    result(access.delete(response.id)) shouldBe newResponse
    result(access.list()).size shouldBe 0

    // test nonexistent data
    an[IllegalArgumentException] should be thrownBy result(access.get("asdasdasd"))
    an[IllegalArgumentException] should be thrownBy result(access.update("Asdasd", anotherRequest))
  }

  @Test
  def testInvalidSink(): Unit = configurators.foreach { configurator =>
    val access = ConnectorApi.access().hostname(configurator.hostname).port(configurator.port)

    result(access.list()).size shouldBe 0

    val illegalOrder = Seq(Column.of("cf", DataType.BOOLEAN, 0), Column.of("cf", DataType.BOOLEAN, 2))
    an[IllegalArgumentException] should be thrownBy result(
      access.add(
        ConnectorConfigurationRequest(name = methodName,
                                      className = "jdbc",
                                      schema = illegalOrder,
                                      configs = Map("c0" -> "v0", "c1" -> "v1"),
                                      topics = Seq.empty,
                                      numberOfTasks = 1)))
    result(access.list()).size shouldBe 0

    val duplicateOrder = Seq(Column.of("cf", DataType.BOOLEAN, 1), Column.of("cf", DataType.BOOLEAN, 1))
    an[IllegalArgumentException] should be thrownBy result(
      access.add(
        ConnectorConfigurationRequest(name = methodName,
                                      className = "jdbc",
                                      schema = duplicateOrder,
                                      configs = Map("c0" -> "v0", "c1" -> "v1"),
                                      topics = Seq.empty,
                                      numberOfTasks = 1)))
    result(access.list()).size shouldBe 0
  }

  @After
  def tearDown(): Unit = configurators.foreach(_.close())
}
