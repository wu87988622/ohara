package com.island.ohara.kafka.connector

import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce._
import com.island.ohara.rule.MediumTest
import org.junit.{Before, Test}
import org.scalatest.Matchers

import scala.concurrent.duration._

class TestRowConnector extends MediumTest with Matchers {

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
    doClose(new OharaTestUtil(3, 2)) { testUtil =>
      {
        testUtil.availableConnectors().contains(classOf[SimpleRowSourceConnector].getSimpleName) shouldBe true
        testUtil.runningConnectors() shouldBe "[]"
        var resp = testUtil
          .sourceConnectorCreator()
          .name("my_source_connector")
          .connectorClass(classOf[SimpleRowSourceConnector])
          .topic("my_connector_topic")
          .taskNumber(sourceTasks)
          .config(Map(SimpleRowSourceConnector.POLL_COUNT_MAX -> pollCountMax.toString))
          .run()
        withClue(s"body:${resp._2}") {
          resp._1 shouldBe 201
        }
        // wait for starting the source connector
        testUtil.await(() => testUtil.runningConnectors().contains("my_source_connector"), 10 second)
        // wait for starting the source task
        testUtil.await(() => SimpleRowSourceTask.pollCount.get >= pollCountMax, 10 second)
        resp = testUtil
          .sinkConnectorCreator()
          .name("my_sink_connector")
          .connectorClass(classOf[SimpleRowSinkConnector])
          .topic("my_connector_topic")
          .taskNumber(sinkTasks)
          .run()
        withClue(s"body:${resp._2}") {
          resp._1 shouldBe 201
        }
        // wait for starting the sink connector
        testUtil.await(() => testUtil.runningConnectors().contains("my_sink_connector"), 10 second)
        // wait for starting the sink task
        testUtil.await(() => SimpleRowSinkTask.runningTaskCount.get == sinkTasks, 10 second)

        // check the data sent by source task
        testUtil.await(
          () => SimpleRowSourceTask.submittedRows.size == pollCountMax * SimpleRowSourceTask.rows.size,
          30 second
        )
        SimpleRowSourceTask.rows.foreach(row => SimpleRowSourceTask.submittedRows.contains(row) shouldBe true)

        // check the data received by sink task
        testUtil.await(() => SimpleRowSinkTask.receivedRows.size == pollCountMax * SimpleRowSourceTask.rows.size,
                       30 second)
        SimpleRowSourceTask.rows.foreach(row => SimpleRowSinkTask.receivedRows.contains(row) shouldBe true)
      }
    }
  }
}
