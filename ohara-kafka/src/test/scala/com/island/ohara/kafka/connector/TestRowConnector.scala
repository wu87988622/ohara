package com.island.ohara.kafka.connector

import com.island.ohara.integration.{OharaTestUtil, With3Blockers3Workers}
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestRowConnector extends With3Blockers3Workers with Matchers {

  @Before
  def setUp(): Unit = {
    SimpleRowSinkTask.reset()
    SimpleRowSourceTask.reset()
  }

  @Test
  def testCreateConnectorWithMultiWorkers(): Unit = {
    val sourceTasks = 3
    val sinkTasks = 2
    val pollCountMax = 5
    testUtil.availableConnectors().body.contains(classOf[SimpleRowSourceConnector].getSimpleName) shouldBe true
    testUtil.runningConnectors().body shouldBe "[]"
    var resp = testUtil
      .sourceConnectorCreator()
      .name("my_source_connector")
      .connectorClass(classOf[SimpleRowSourceConnector])
      .topic("my_connector_topic")
      .taskNumber(sourceTasks)
      .config(Map(SimpleRowSourceConnector.POLL_COUNT_MAX -> pollCountMax.toString))
      .run()
    withClue(s"body:${resp.body}") {
      resp.statusCode shouldBe 201
    }
    // wait for starting the source connector
    OharaTestUtil.await(() => testUtil.runningConnectors().body.contains("my_source_connector"), 10 second)
    // wait for starting the source task
    OharaTestUtil.await(() => SimpleRowSourceTask.pollCount.get >= pollCountMax, 10 second)
    resp = testUtil
      .sinkConnectorCreator()
      .name("my_sink_connector")
      .connectorClass(classOf[SimpleRowSinkConnector])
      .topic("my_connector_topic")
      .taskNumber(sinkTasks)
      .run()
    withClue(s"body:${resp.body}") {
      resp.statusCode shouldBe 201
    }
    // wait for starting the sink connector
    OharaTestUtil.await(() => testUtil.runningConnectors().body.contains("my_sink_connector"), 10 second)
    // wait for starting the sink task
    OharaTestUtil.await(() => SimpleRowSinkTask.runningTaskCount.get == sinkTasks, 10 second)

    // check the data sent by source task
    OharaTestUtil.await(
      () => SimpleRowSourceTask.submittedRows.size == pollCountMax * SimpleRowSourceTask.rows.size,
      30 second
    )
    SimpleRowSourceTask.rows.foreach(row => SimpleRowSourceTask.submittedRows.contains(row) shouldBe true)

    // check the data received by sink task
    OharaTestUtil
      .await(() => SimpleRowSinkTask.receivedRows.size == pollCountMax * SimpleRowSourceTask.rows.size, 30 second)
    SimpleRowSourceTask.rows.foreach(row => SimpleRowSinkTask.receivedRows.contains(row) shouldBe true)
  }
}
